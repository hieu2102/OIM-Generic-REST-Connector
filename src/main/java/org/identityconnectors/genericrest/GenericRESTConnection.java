//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.identityconnectors.genericrest;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicHeader;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.genericrest.utils.GenericRESTUtil;
import org.identityconnectors.restcommon.ClientHandler;

public class GenericRESTConnection {
    private GenericRESTConfiguration configuration;
    private CloseableHttpClient client;
    private Map<String, String> authHeader;
    private final Instant connStartTime;
    private Instant connEndTime;
    private static Log log = Log.getLog(GenericRESTConnection.class);

    public GenericRESTConnection(GenericRESTConfiguration config) {
        this.configuration = config;
        HttpClientBuilder clientBuilder = HttpClients.custom();
        log.info("authentication type is:{0}", new Object[]{this.configuration.getAuthenticationType()});
        this.setProxy(clientBuilder);
        this.setTimeOut(clientBuilder);
        this.connStartTime = Instant.now();
        log.info("Connection start time is {0}", new Object[]{this.connStartTime});
        this.setAuthHeaders(clientBuilder);
        this.setAccessTokenExpireTime();
    }

    private void setAuthHeaders(HttpClientBuilder clientBuilder) {
        log.ok("Method Entered", new Object[0]);
        String authClassName = this.configuration.getCustomAuthClassName();
        log.info("auth Class Name is:{0}", new Object[]{authClassName});

        try {
            this.authHeader = ClientHandler.getAuthenticationHeaders(GenericRESTUtil.formAuthConfigParamsMap(this.configuration), authClassName);
        } catch (Exception var4) {
            log.error("Exception in getting authentication header, {0}", new Object[]{var4});
            throw new ConnectorException(this.configuration.getMessage("ex.getAuthHeader", "Exception in getting authentication header", new Object[0]) + " " + var4.getMessage(), var4);
        }

        this.client = this.getAuthClient(clientBuilder, this.configuration.getAuthenticationType(), this.authHeader);
        log.ok("Method Exiting", new Object[0]);
    }

    public CloseableHttpClient getConnection() {
        return this.client;
    }

    private CloseableHttpClient getAuthClient(
            HttpClientBuilder clientBuilder,
            String authType,
            Map<String, String> authHeader
    ) {
        log.ok("Method Entered", new Object[0]);
        log.info("building auth:{0} client", new Object[]{authType});
        List<Header> headers = new ArrayList();
        if (this.configuration.getCustomHeaders() != null && this.configuration.getCustomHeaders().length > 0) {
            authHeader.putAll(GenericRESTUtil.convertConfigArrayToMap(this.configuration.getCustomHeaders()));
        }

        Iterator var5 = authHeader.keySet().iterator();

        while (var5.hasNext()) {
            String entry = (String) var5.next();
            Header header = new BasicHeader(entry, (String) authHeader.get(entry));
            headers.add(header);
        }

        Header contentHeader = new BasicHeader("Content-Type", this.configuration.getHttpHeaderContentType());
        Header acceptHeader = new BasicHeader("Accept", this.configuration.getHttpHeaderAccept());
        headers.add(contentHeader);
        headers.add(acceptHeader);
        clientBuilder.setDefaultHeaders(headers);
        log.info("returning auth client", new Object[0]);
        log.ok("Method Exiting", new Object[0]);
        return clientBuilder.build();
    }

    private void setTimeOut(HttpClientBuilder clientBuilder) {
        log.ok("Method Entered", new Object[0]);
        log.info("setting  timeout", new Object[0]);
        clientBuilder.setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(this.configuration.getConnectionTimeOut()).setSocketTimeout(this.configuration.getSocketTimeOut()).build());
        log.info("timeout set", new Object[0]);
        log.ok("Method Exiting", new Object[0]);
    }

    private void setProxy(HttpClientBuilder clientBuilder) {
        log.info("setting  proxy", new Object[0]);
        log.ok("Method Entered", new Object[0]);
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        if (!StringUtil.isBlank(this.configuration.getProxyHost()) && this.configuration.getProxyPort() > 0) {
            HttpHost proxy = new HttpHost(this.configuration.getProxyHost(), this.configuration.getProxyPort(), this.configuration.getProxyScheme());
            HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            if (this.configuration.getProxyUser() != null && this.configuration.getProxyPassword() != null) {
                credsProvider.setCredentials(new AuthScope(this.configuration.getProxyHost(), this.configuration.getProxyPort()), new UsernamePasswordCredentials(this.configuration.getProxyUser(), GenericRESTUtil.decryptPassword(this.configuration.getProxyPassword())));
                clientBuilder.setDefaultCredentialsProvider(credsProvider);
            }

            clientBuilder.setRoutePlanner(routePlanner);
            log.info("proxy set", new Object[0]);
            log.ok("Method Exiting", new Object[0]);
        } else {
            log.info("proxy not set", new Object[0]);
        }
    }

    private void setAccessTokenExpireTime() {
        String sTokenValidity = (String) this.authHeader.get("accessTokenValidity");
        long lTokenValidity = 0L;
        log.info("validitiy duration is {0}", new Object[]{sTokenValidity});

        try {
            if (sTokenValidity == null || Long.parseLong(sTokenValidity) <= 0L) {
                return;
            }

            lTokenValidity = Long.parseLong(sTokenValidity);
            if (lTokenValidity >= 1800L) {
                lTokenValidity -= 240L;
            } else {
                lTokenValidity = (long) (0.85 * (double) lTokenValidity);
            }

            this.connEndTime = this.connStartTime.plusSeconds(lTokenValidity);
        } catch (NumberFormatException var5) {
            log.error("Invalid token validity." + var5.getMessage(), new Object[0]);
        }

        log.info("validitiy duration: {0}, \tAccess token expire time: {1}", new Object[]{lTokenValidity, this.connEndTime});
    }

    public Instant getConnectionEndTime() {
        return this.connEndTime;
    }

    public void disposeConnection() {
        log.info("disposing connection", new Object[0]);

        try {
            this.client.close();
        } catch (IOException var2) {
            log.error("Exception occurred while closing the connection", new Object[0]);
            throw new ConnectorException(this.configuration.getMessage("ex.resourceCloseFailure", "Exception occurred while closing the connection", new Object[0]) + " " + var2.getMessage(), var2);
        }
    }
}
