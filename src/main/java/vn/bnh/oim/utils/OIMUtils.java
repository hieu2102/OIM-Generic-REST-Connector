package vn.bnh.oim.utils;

import Thor.API.Operations.*;
import Thor.API.tcResultSet;
import oracle.core.ojdl.logging.ODLLogger;
import oracle.iam.identity.exception.NoSuchUserException;
import oracle.iam.identity.exception.UserLookupException;
import oracle.iam.identity.orgmgmt.api.OrganizationManager;
import oracle.iam.identity.rolemgmt.api.RoleManager;
import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.Platform;
import oracle.iam.platform.authz.exception.AccessDeniedException;
import oracle.iam.platform.utils.lookup.LookupUtils;
import oracle.iam.platformservice.api.PlatformService;

import javax.security.auth.login.LoginException;
import java.time.LocalDate;
import java.util.*;

import weblogic.security.auth.login.UsernamePasswordLoginModule;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * class sử dụng các service của hệ thống OIG qua user xelsysadm
 *
 * @author hieund
 */
public class OIMUtils {
    // ev oim client
    // Logger


    private final ODLLogger logger = ODLLogger.getODLLogger(OIMUtils.class.getName());
    private final String OIM_ADMIN_USERNAME;
    private final String OIM_ADMIN_PASSWORD;
    private final String OIM_HOSTNAME;
    private final String OIM_PORT; // For SSL, use 14001; For
    private final String OIM_PROVIDER_URL; // For SSL, use t3s protocol;
    private final String AUTHWL_PATH = "config/authwl.conf";
    private final String APPSERVER_TYPE = "wls";
    private final String FACTORY_INITIAL_TYPE = "weblogic.jndi.WLInitialContextFactory";
    public OIMClient oimClient = null;
    public LookupUtils lookup;
    // new service
    public tcUserOperationsIntf userOperationsintf = null;
    public tcFormInstanceOperationsIntf formInstanceOperationsIntf = null;
    public tcProvisioningOperationsIntf provisioningOperationsIntf = null;
    public tcFormDefinitionOperationsIntf formDefOperationIntf = null;
    public tcFormInstanceOperationsIntf formInstanceIntf = null;
    public tcITResourceInstanceOperationsIntf itResDefOperationIntf = null;
    public UserManager usrService = null;
    public PlatformService service = null;
    public RoleManager roleService = null;
    public OrganizationManager orgService = null;

    /**
     * Constructor khởi tạo object OIMUtils khi chạy trên server remote
     *
     * @param initValues HashMap chứa thông tin đăng nhập của user xelsysadm <br>
     *                   OIM_HOSTNAME: hostname của hệ thống OIG <br>
     *                   OIM_PORT: port của hệ thống OIG <br>
     *                   OIM_ADMIN_PASSWORD: password của user xelsysadm <br>
     *                   OIM_ADMIN_USERNAME: xelsysadm
     */
    public OIMUtils(HashMap<String, String> initValues) throws LoginException {
        this.OIM_HOSTNAME = initValues.get("OIM_HOSTNAME");
        this.OIM_PORT = initValues.get("OIM_PORT");
        this.OIM_ADMIN_USERNAME = initValues.get("OIM_ADMIN_USERNAME");
        this.OIM_ADMIN_PASSWORD = initValues.get("OIM_ADMIN_PASSWORD");
        OIM_PROVIDER_URL = "t3://" + OIM_HOSTNAME + ":" + OIM_PORT;
        initialize();
    }

    public OIMClient getOimClient() {
        return this.oimClient;
    }

    /**
     * method lấy thông tin của cán bộ trên hệ thống OIG theo tài khoản domain (user
     * login)
     *
     * @param userLogin tên đăng nhập AD của cán bộ
     * @return
     * @throws Exception
     */
    public User getUser(String userLogin) throws Exception {
        Set<String> resAttrs = new HashSet<String>();
        User user = null;
        try {
            user = usrService.getDetails(userLogin, resAttrs, true);
        } catch (NoSuchUserException | UserLookupException | AccessDeniedException e) {
            throw new Exception("Khong tim thay CB " + userLogin + " tren OIG");
        }
        return user;

    }

    /**
     * Constructor OIMUtils khi chạy trên server OIG, lấy thông tin đăng nhập của
     * user xelsysadm từ IT Resource "Initialize Services" trên hệ thống OIG
     */
    public OIMUtils() {
        HashMap<String, String> initValues = getServiceInitITResource();
        this.OIM_HOSTNAME = initValues.get("OIM_HOSTNAME");
        this.OIM_PORT = initValues.get("OIM_PORT");
        this.OIM_ADMIN_USERNAME = initValues.get("OIM_ADMIN_USERNAME");
        this.OIM_ADMIN_PASSWORD = initValues.get("OIM_ADMIN_PASSWORD");
        OIM_PROVIDER_URL = "t3://" + OIM_HOSTNAME + ":" + OIM_PORT;
        try {
            initialize();
        } catch (LoginException e) {
        }
    }

    /**
     * method lấy thông tin đăng nhập của user xelsysadm thông qua IT Resource
     * "Initialize Services"
     *
     * @return HashMap chứa thông tin đăng nhập của user xelsysadm (hostname, port,
     * username, password)
     */
    public HashMap<String, String> getServiceInitITResource() {

        HashMap<String, String> adITResourcefileds = new HashMap<String, String>();

        try {
            tcITResourceInstanceOperationsIntf tcITResourceIntf = Platform.getService(tcITResourceInstanceOperationsIntf.class);
            HashMap<String, String> searchcriteria = new HashMap<String, String>();
            searchcriteria.put("IT Resources.Name", "Initialize Services");
            tcResultSet resultSet = tcITResourceIntf.findITResourceInstances(searchcriteria);
            resultSet = tcITResourceIntf.getITResourceInstanceParameters(resultSet.getLongValue("IT Resources.Key"));

            for (int i = 0; i < resultSet.getRowCount(); i++) {
                resultSet.goToRow(i);
                String name = resultSet.getStringValue("IT Resources Type Parameter.Name");

                String value = resultSet.getStringValue("IT Resources Type Parameter Value.Value");
                adITResourcefileds.put(name, value);
            }
        } catch (Exception ae) {
        }
        return adITResourcefileds;

    }

    /**
     * method đăng nhập vào hệ thống OIG với user xelsysadm và khởi tạo các service
     *
     * @throws LoginException
     */
    private void initialize() throws LoginException {
        // Set system properties required for OIMClient
        System.out.println("Begin OIM client Login ");
        Configuration.setConfiguration(new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
//                Preconditions.checkArgument("xellerate".equals(name));
                return new AppConfigurationEntry[]{new AppConfigurationEntry(UsernamePasswordLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, Collections.singletonMap("debug", "true"))};
            }
        });
//        System.setProperty("java.security.auth.login.config", AUTHWL_PATH);
        System.setProperty("APPSERVER_TYPE", APPSERVER_TYPE);

        // Create an instance of OIMClient with OIM environment information
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(OIMClient.JAVA_NAMING_FACTORY_INITIAL, FACTORY_INITIAL_TYPE);
        env.put(OIMClient.JAVA_NAMING_PROVIDER_URL, OIM_PROVIDER_URL);
        // Establish an OIM Client
        oimClient = new OIMClient(env);

        // Login to OIM with System Administrator Credentials
//        oimClient.login(OIM_ADMIN_USERNAME, OIM_ADMIN_PASSWORD.toCharArray());
        oimClient.login(OIM_ADMIN_USERNAME, OIM_ADMIN_PASSWORD.toCharArray(), env);
        formInstanceOperationsIntf = oimClient.getService(tcFormInstanceOperationsIntf.class);
        userOperationsintf = oimClient.getService(tcUserOperationsIntf.class);
        provisioningOperationsIntf = oimClient.getService(tcProvisioningOperationsIntf.class);
        formDefOperationIntf = oimClient.getService(tcFormDefinitionOperationsIntf.class);
        itResDefOperationIntf = oimClient.getService(tcITResourceInstanceOperationsIntf.class);
        usrService = oimClient.getService(UserManager.class);
        service = oimClient.getService(PlatformService.class);
        roleService = oimClient.getService(RoleManager.class);
        orgService = oimClient.getService(OrganizationManager.class);
        formInstanceIntf = oimClient.getService(tcFormInstanceOperationsIntf.class);

    }

    /**
     * method đăng xuất khỏi hệ thống OIG với user xelsysadm
     */
    public void logout() {

        if (oimClient != null) {
            oimClient.logout();
        }
    }

    /**
     * method lấy dữ liệu của IT Resource
     *
     * @param itResourceName tên IT Resource
     * @return HashMap chứa dữ liệu của IT Resource
     */
    public HashMap<String, String> getITResourceAttributeInMap(String itResourceName) {

        HashMap<String, String> adITResourcefileds = new HashMap<String, String>();
        try {
            tcITResourceInstanceOperationsIntf tcITResourceIntf = oimClient.getService(tcITResourceInstanceOperationsIntf.class);
            HashMap<String, String> searchcriteria = new HashMap<String, String>();
            searchcriteria.put("IT Resources.Name", itResourceName);
            tcResultSet resultSet = tcITResourceIntf.findITResourceInstances(searchcriteria);
            resultSet = tcITResourceIntf.getITResourceInstanceParameters(resultSet.getLongValue("IT Resources.Key"));
            for (int i = 0; i < resultSet.getRowCount(); i++) {
                resultSet.goToRow(i);
                String name = resultSet.getStringValue("IT Resources Type Parameter.Name");

                String value = resultSet.getStringValue("IT Resources Type Parameter Value.Value");
                adITResourcefileds.put(name, value);
            }
        } catch (Exception ae) {
        }
        return adITResourcefileds;

    }

    /**
     * method lấy dữ liệu API (credential để sử dụng API, API endpoint, ...) của hệ
     * thống đích thông qua IT Resource trên hệ thống OIG
     *
     * @param itResourceName tên ứng dụng hệ thống đích trên hệ thống OIG
     * @return HashMap chứa dữ liệu API của hệ thống đích
     */
    public HashMap<String, String> getTargetITResource(String itResourceName) {

        HashMap<String, String> adITResourcefileds = new HashMap<>();
        try {
            tcITResourceInstanceOperationsIntf tcITResourceIntf = oimClient.getService(tcITResourceInstanceOperationsIntf.class);
            HashMap<String, String> searchcriteria = new HashMap<String, String>();
            searchcriteria.put("IT Resources.Name", itResourceName);
            tcResultSet resultSet = tcITResourceIntf.findITResourceInstances(searchcriteria);
            resultSet = tcITResourceIntf.getITResourceInstanceParameters(resultSet.getLongValue("IT Resources.Key"));
            for (int i = 0; i < resultSet.getRowCount(); i++) {
                resultSet.goToRow(i);
                String name = resultSet.getStringValue("IT Resources Type Parameter.Name");

                String value = resultSet.getStringValue("IT Resources Type Parameter Value.Value");
                adITResourcefileds.put(name, value);
            }
            String relURI = adITResourcefileds.get("relURIs");
            if (relURI.contains(",")) {
                String[] endpoints = relURI.split(",");
                for (String epoint : endpoints) {
                    int idx = epoint.indexOf("=");
                    if (epoint.contains("CREATEOP")) {
                        adITResourcefileds.put("CREATEOP", epoint.substring(idx + 1, epoint.length() - 1));
                    }
                    if (epoint.contains("UPDATEOP")) {
                        adITResourcefileds.put("UPDATEOP", epoint.substring(idx + 1, epoint.length() - 1));
                    }
                }
            }
        } catch (Exception ae) {
            System.out.println(ae.getMessage());
        }
        return adITResourcefileds;

    }

    /**
     * method chuyển đổi ngày hiện tại sang format Date7 (năm + số ngày trong năm)
     * VD: 2019/01/01: 01/01 là ngày đầu tiên của năm 2019, vậy method sẽ trả về:
     * 2019001 <br>
     * tương tự, ngày 2019/01/02 sẽ trả về: 2019002
     *
     * @return ngày hiện tại dưới format Date7
     */
    public String getCurrentDate7() {
        int dayInt = LocalDate.now().getDayOfYear();
        String day = Integer.toString(dayInt);
        if (day.length() < 2) {
            day = "0" + day;
        }
        if (day.length() < 3) {
            day = "0" + day;
        }
        String year = Integer.toString(LocalDate.now().getYear());
        String date7 = year + day;

        System.out.println("current date7: " + date7);
        return date7;
    }

}
