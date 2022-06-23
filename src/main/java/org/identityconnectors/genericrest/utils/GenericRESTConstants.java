//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.identityconnectors.genericrest.utils;

import java.util.Arrays;
import java.util.List;

import org.identityconnectors.restcommon.utils.RESTCommonConstants;
import org.identityconnectors.restcommon.utils.RESTCommonConstants.HTTPOperationType;

public class GenericRESTConstants {
    public static final String METHOD_ENTERED = "Method Entered";
    public static final String METHOD_EXITING = "Method Exiting";
    public static final String ID = "id";
    public static final String UID = "__UID__";
    public static final String APPLICATION_JSON = "application/json";
    public static final Object USERS = "USERS";
    public static final Object GROUPS = "GROUPS";
    public static final String ENABLE = "__ENABLE__";
    public static final String MEMBERSHIP = "__MEMBERSHIP__";
    public static final String MESSAGE_CATALOG_PATH = "org/identityconnectors/genericrest/Messages";
    public static final String DISPLAY_GENERIC_REST_CONNECTOR = "display_GenericRESTConnector";
    public static final int DEFAULT_TOKEN_LIFESPAN = 3600;
    public static final int HALF_HOUR_TOKEN_LIFESPAN = 1800;
    public static final int BUFFER_TIME_SPAN = 240;
    public static final String JSON_PARSER_CLASS_NAME = "org.identityconnectors.restcommon.parser.impl.JSONParser";
    public static final String PLACEHOLDER_START = "$(";
    public static final String PLACEHOLDER_END = ")$";
    public static final String SINGLE = "SINGLE";
    public static final String DELIMITER = ".";
    public static final String CONFIGURATION_DELIMITER = "=";
    public static final String SEMICOLON = ";";
    public static final String COMMA = ",";
    public static final String HTTPS = "https://";
    public static final String HTTP = "http://";
    public static final String FILTER_SUFFIX = "Filter Suffix";
    public static final String INCREMENTAL_RECON_ATTRIBUTE = "Incremental Recon Attribute";
    public static final String LATEST_TOKEN = "Latest Token";
    public static final String RESPONSESLISTTAG = "ResponsesListTag";
    public static final String PAGEATTRTAG = "PageAttrTag";
    public static final String OIM_ORGNAME = "OIM Organization Name";
    public static final String PWD = "__PASSWORD__";
    public static final String URL = "URL";
    public static final String EMPTY_STRING = "";
    public static final String PASSWORD_MARKER = "##";
    public static final String CHILDATTR = "CHILD.";
    public static final String PARENTATTR = "PARENT.";
    public static final String PLACEHOLDER_PAGE_SIZE = "$(PAGE_SIZE)$";
    public static final String PLACEHOLDER_PAGE_OFFSET = "$(PAGE_OFFSET)$";
    public static final String PLACEHOLDER_PAGE_INCREMENT = "$(PAGE_INCREMENT)$";
    public static final String PLACEHOLDER_FILTERSUFFIX = "$(Filter Suffix)$";
    public static final String PLACEHOLDER_PAGE_TOKEN = "$(PAGE_TOKEN)$";
    public static final String FORWARDSLASH_CHAR = "/";
    public static final String QUESTIONMARK_CHAR = "?";
    public static final String BLANK_CHAR = "";
    public static final char AMPERSAND_CHAR = '&';
    public static final String AUTH_IMPL_PACKAGE = "org.identityconnectors.restcommon.auth.impl";
    public static final String PARSER_IMPL_PACKAGE = "org.identityconnectors.restcommon.parser.impl";
    public static final List<String> AUTH_CONFIG_PARAMS = Arrays.asList("host", "port", "authenticationType", "sslEnabled", "username", "sub", "iss", "scope", "aud", "tokenLifespan", "privateKeyLocation", "password", "privateKeySecret", "privateKeyFormat", "proxyPassword", "signatureAlgorithm", "clientId", "clientSecret", "authenticationServerUrl", "proxyHost", "proxyPort", "proxyUser", "connectionTimeOut", "socketTimeOut", "httpHeaderContentType", "httpHeaderAccept", "customAuthClassName", "jsonResourcesTag", "customParserClassName", "authorizationUrl", "clientUrl", "companyId");
    public static final String GETTER = "get";
    public static final String CUSTOM_AUTH_HEADER = "customAuthHeaders";
    public static final String ACCESS_TOKEN_VALIDITY = "accessTokenValidity";
    public static final String INCREMENTAL_RECON_ATTRIBUTE_PLACEHOLDER = "$(Incremental Recon Attribute)$";
    public static final String LATEST_TOKEN_PLACEHOLDER = "$(Latest Token)$";
    public static final String QUERY_PARAM_SEPARATOR = "&";
    public static final String UID_EQUALS_FILTER_PATTERN = "__UID__=";

    public GenericRESTConstants() {
    }

    public static enum CONNECTOR_OPERATION {
        CREATEOP(HTTPOperationType.POST), UPDATEOP(HTTPOperationType.PATCH), DELETEOP(HTTPOperationType.DELETE), SEARCHOP(HTTPOperationType.GET), TESTOP(HTTPOperationType.GET), ADDATTRIBUTE(HTTPOperationType.PATCH), REMOVEATTRIBUTE(HTTPOperationType.DELETE);

        RESTCommonConstants.HTTPOperationType defaultHTTPOperation;

        private CONNECTOR_OPERATION(RESTCommonConstants.HTTPOperationType httpOperation) {
            this.defaultHTTPOperation = httpOperation;
        }

        public RESTCommonConstants.HTTPOperationType getDefaultHTTPOperation() {
            return this.defaultHTTPOperation;
        }
    }

    public static enum ParserTypes {
        JSON("JSONParser");

        private String parserClassName;

        private ParserTypes(String parserClassName) {
            this.parserClassName = parserClassName;
        }

        public String getParserClassName() {
            return "org.identityconnectors.restcommon.parser.impl." + this.parserClassName;
        }
    }

    public static enum AuthenticationTypes {
        BASIC_AUTH("Basic", "HttpBasicAuth"), JWT("jwt", "OAuthJWT"), CLIENT_CRED("client_credentials", "OAuthClientCredentials"), RESOURCE_OWNER_PWD("password", "OAuthPassword"), CUSTOM("custom", "Custom"), OTHER("other", "OAuthAuthorizationCode"), OAUTH_SAML("oauth_saml", "OAuthSAML");

        private String authName;
        private String authClassName;

        private AuthenticationTypes(
                String authName,
                String authClassName
        ) {
            this.authName = authName;
            this.authClassName = authClassName;
        }

        public String getAuthName() {
            return this.authName;
        }

        public String getAuthClassName() {
            return "org.identityconnectors.restcommon.auth.impl." + this.authClassName;
        }
    }
}
