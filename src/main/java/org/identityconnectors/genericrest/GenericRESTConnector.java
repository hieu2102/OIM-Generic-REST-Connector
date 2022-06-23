//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.identityconnectors.genericrest;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.genericrest.utils.GenericRESTConstants.CONNECTOR_OPERATION;
import org.identityconnectors.genericrest.utils.GenericRESTUtil;
import org.identityconnectors.restcommon.ClientHandler;
import org.identityconnectors.restcommon.parser.spi.ParserPlugin;
import org.identityconnectors.restcommon.utils.RESTCommonConstants;
import org.identityconnectors.restcommon.utils.RESTCommonConstants.HTTPOperationType;
import org.identityconnectors.restcommon.utils.RESTCommonUtils;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ConnectorClass(displayNameKey = "display_GenericRESTConnector", configurationClass = GenericRESTConfiguration.class, messageCatalogPaths = {"org/identityconnectors/genericrest/Messages"})
public class GenericRESTConnector implements Connector, CreateOp, UpdateOp, DeleteOp, SearchOp<String>, UpdateAttributeValuesOp, TestOp {
    public static final String OBJECT_CLASS_VALUE = "oclass = [";
    public static final String ATTRIBUTE_SET_IS_EMPTY = "Attribute set is empty";
    public static final String SPECIAL_ATTRIBUTE_SET = "special attribute set :{0}";
    private static final String AOB_TEST_CONN_USER = "AOB_TEST_CONN_USER";
    private static final Log log = Log.getLog(GenericRESTConnector.class);
    final String EMPTY_ATTRIBUTE_SET_EXCEPTION = "ex.emptyattrset";
    private GenericRESTConfiguration configuration;
    private GenericRESTConnection connection;
    private Map<String, List<String>> simpleMultivaluedAttributesMap;
    private Map<String, RESTCommonConstants.HTTPOperationType> opTypeMap;
    private Map<String, String> relURIsMap;
    private Map<String, Map<String, String>> namedAttributeMap;
    private ParserPlugin parser;
    private Map<String, String> parserConfigParamsMap;
    private Map<String, String> objectClasstoParserConfigMap;
    private Map<String, String> customPayloadMap;
    private Map<String, String> incrementalReconParamsMap;

    public GenericRESTConnector() {
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public void init(Configuration config) {
        log.ok("Method Entered");
        this.configuration = (GenericRESTConfiguration) config;
        this.configuration.validate();
        this.connection = new GenericRESTConnection(this.configuration);
        this.initConfigMaps();
        this.initParser();
        log.ok("Method Exiting");
    }

    public void dispose() {
        log.ok("Method Entered");
        log.info("disposing connection");
        if (this.connection != null) {
            this.connection.disposeConnection();
        }

        log.ok("Method Exiting");
    }

    private void initConfigMaps() {
        log.ok("Method Entered");
        log.info("initializing maps");
        this.simpleMultivaluedAttributesMap = GenericRESTUtil.getSimpleMultivaluedDetails(this.configuration);
        this.opTypeMap = GenericRESTUtil.getOpTypeMap(this.configuration);
        this.relURIsMap = GenericRESTUtil.getRelURIsMap(this.configuration);
        this.namedAttributeMap = GenericRESTUtil.getNamedAttributeMap(this.configuration);
        this.parserConfigParamsMap = GenericRESTUtil.formParserConfigParamsMap(this.configuration);
        this.objectClasstoParserConfigMap = GenericRESTUtil.formObjectClasstoParserConfigMap(this.configuration);
        this.customPayloadMap = GenericRESTUtil.getCustomPayloadMap(this.configuration);
        log.ok("Method Exiting");
    }

    private void initParser() {
        log.ok("Method Entered");

        try {
            this.parser = ClientHandler.getParserInstance(this.configuration.getCustomParserClassName());
        } catch (Exception var2) {
            log.error("Exception in initializing parser, {0}", var2);
            throw new ConnectorException(this.configuration.getMessage("ex.initParser", "Exception in initializing parser") + " " + var2.getMessage(), var2);
        }

        log.ok("Method Exiting");
    }

    public Uid create(
            ObjectClass oclass,
            Set<Attribute> attrSet,
            OperationOptions options
    ) {
        log.ok("Method Entered");
        log.info("oclass = [" + oclass.getObjectClassValue() + "], attrSet = [" + attrSet + "], " + "" + "(options)attributes to get = [" + options.getAttributesToGet() + "]");
        String jsonMapString = null;
        Object uidValue = null;
        Set<Attribute> specialAttributeset = new HashSet();
        Set<Attribute> normalAttributeSet = new HashSet();
        if (attrSet.isEmpty()) {
            log.error("Attribute set is empty");
            log.ok("Method Exiting");
            throw new ConnectorException(this.configuration.getMessage("ex.emptyattrset", "Attribute set is empty"));
        } else {
            GenericRESTUtil.separateSpecialAttributeSet(oclass, attrSet, this.relURIsMap, CONNECTOR_OPERATION.CREATEOP, specialAttributeset, normalAttributeSet);
            log.info("special attribute set :{0}", specialAttributeset);
            log.info(" attribute set :{0}", normalAttributeSet);
            jsonMapString = this.getRequestPayload(oclass, normalAttributeSet, null, oclass.getObjectClassValue(), CONNECTOR_OPERATION.CREATEOP.toString());
            this.maskPasswordinLog(jsonMapString, oclass.getObjectClassValue());
            List<Object> urlOpTypeList = GenericRESTUtil.getURIAndOpType(oclass, CONNECTOR_OPERATION.CREATEOP, null, this.relURIsMap, this.opTypeMap, this.configuration);
            urlOpTypeList = GenericRESTUtil.handleAttrNamePlaceHoldersInRelURI(oclass, normalAttributeSet, urlOpTypeList, this.namedAttributeMap, this.simpleMultivaluedAttributesMap, this.configuration);
            String jsonResponseString = this.executeRequest(oclass, null, urlOpTypeList, jsonMapString, null);
            GenericRESTUtil.addJsonResourcesTagToConfigParamsMap(this.parserConfigParamsMap, this.objectClasstoParserConfigMap, oclass.getObjectClassValue());
            jsonResponseString = this.maskPasswordinLog(jsonResponseString, oclass.getObjectClassValue());
            log.info(" response string :{0}", jsonResponseString);
            Map<String, Object> jsonResponseMap = this.parser.parseResponse(jsonResponseString, this.parserConfigParamsMap).get(0);
            Map<String, String> objClassNamedAttrMap = this.namedAttributeMap.get(oclass.getObjectClassValue());
            uidValue = jsonResponseMap.get(objClassNamedAttrMap.get("__UID__"));
            if (uidValue == null && objClassNamedAttrMap.get("__UID__").equals(objClassNamedAttrMap.get(Name.NAME))) {
                uidValue = AttributeUtil.getNameFromAttributes(normalAttributeSet).getNameValue();
            }

            log.info("uid:{0}", uidValue);
            if (uidValue != null && specialAttributeset.size() > 0) {
                try {
                    this.addAttributeValues(oclass, new Uid(uidValue.toString()), specialAttributeset, options);
                } catch (Exception var13) {
                    log.error("Adding special attributes failed, deleting the {0} :{1}", oclass.getObjectClassValue(), uidValue);
                    log.error("Exception in createOp, {0}", var13);
                    this.delete(oclass, new Uid(uidValue.toString()), null);
                    throw new ConnectorException(this.configuration.getMessage("ex.addSpecialAttrFailed", "Adding special attributes failed, deleted the " + oclass.getObjectClassValue() + " :" + uidValue, oclass.getObjectClassValue(), uidValue));
                }
            }

            log.info("returning uid:{0}", uidValue);
            log.info("Method Exiting");
            return uidValue == null ? null : new Uid(uidValue.toString());
        }
    }

    public Uid update(
            ObjectClass oclass,
            Uid uid,
            Set<Attribute> attrSet,
            OperationOptions options
    ) {
        log.ok("Method Entered");
        log.info("oclass = [" + oclass.getObjectClassValue() + "], attrSet = [" + attrSet + "], " + "" + "(options)attributes to get = [" + options.getAttributesToGet() + "],  " + "options key set: " + options.getOptions().keySet());
        Set<Attribute> currentAttrsSet = AttributeUtil.getCurrentAttributes(attrSet);
        if (currentAttrsSet != null) {
            attrSet = GenericRESTUtil.handleCurrentAttributes(attrSet, currentAttrsSet);
        }

        if (this.configuration.isEnableEmptyString()) {
            attrSet = GenericRESTUtil.handleBlankValue(attrSet);
        }

        Set<Attribute> specialAttributeset = new HashSet();
        Set<Attribute> normalAttributeSet = new HashSet();
        GenericRESTUtil.separateSpecialAttributeSet(oclass, attrSet, this.relURIsMap, CONNECTOR_OPERATION.UPDATEOP, specialAttributeset, normalAttributeSet);
        String uidValue = null;
        if (!normalAttributeSet.isEmpty()) {
            log.info("Updating {0} with {1}", uid.getUidValue(), normalAttributeSet);
            uidValue = this.executeUpdate(oclass, uid, normalAttributeSet);
        }

        uidValue = uidValue != null ? uidValue : uid.getUidValue();
        log.info("special attribute set :{0}", specialAttributeset);
        if (specialAttributeset.size() > 0) {
            try {
                this.addAttributeValues(oclass, new Uid(uidValue), specialAttributeset, options);
            } catch (Exception var10) {
                log.error("Adding special attributes failed for uid : {0}", uid.getUidValue());
                log.error("Exception in updateOp, {0}", var10);
                throw new ConnectorException(this.configuration.getMessage("ex.addSpecialAttrFailed", "Adding special attributes failed" + oclass.getObjectClassValue() + "" + " :" + uidValue, oclass.getObjectClassValue(), uidValue));
            }
        }

        log.info("returning uid:{0}", uidValue);
        log.ok("Method Exiting");
        return new Uid(uidValue);
    }

    public Uid addAttributeValues(
            ObjectClass oclass,
            Uid uid,
            Set<Attribute> attrSet,
            OperationOptions options
    ) {
        log.ok("Method Entered");
        Set<Attribute> specialAttributeset = new HashSet();
        Set<Attribute> normalAttributeSet = new HashSet();
        if (attrSet.isEmpty()) {
            log.error("Attribute set to update is empty");
            log.ok("Method Exiting");
            throw new ConnectorException(this.configuration.getMessage("ex.emptyattrset", "Attribute set is empty"));
        } else {
            GenericRESTUtil.separateSpecialAttributeSet(oclass, attrSet, this.relURIsMap, CONNECTOR_OPERATION.ADDATTRIBUTE, specialAttributeset, normalAttributeSet);
            log.info("special attribute set :{0}", specialAttributeset);
            Attribute currentAttrs = AttributeUtil.find(OperationalAttributes.CURRENT_ATTRIBUTES, normalAttributeSet);
            if (currentAttrs != null) {
                log.ok("Found Current attributes in normalAttributeSet, currentAttrs : " + currentAttrs);
                log.ok("Removing Current attributes from normalAttributeSet");
                normalAttributeSet.remove(currentAttrs);
            }

            log.info("normal attribute set :{0}", normalAttributeSet);
            if (normalAttributeSet.size() > 0) {
                this.executeUpdate(oclass, uid, normalAttributeSet);
            }

            if (specialAttributeset.size() > 0) {
                Iterator var8 = specialAttributeset.iterator();

                while (var8.hasNext()) {
                    Attribute attr = (Attribute) var8.next();
                    Map<String, String> attributeHandlingMap = new HashMap();
                    String specialAttrSearchKey = GenericRESTUtil.getSearchKey(oclass.getObjectClassValue(), attr.getName());
                    List<Object> urlOpTypeList = GenericRESTUtil.getURIAndOpType(oclass, CONNECTOR_OPERATION.ADDATTRIBUTE, attr.getName(), this.relURIsMap, this.opTypeMap, this.configuration);
                    urlOpTypeList = GenericRESTUtil.handleAttrNamePlaceHoldersInRelURI(oclass, attrSet, urlOpTypeList, this.namedAttributeMap, this.simpleMultivaluedAttributesMap, this.configuration);
                    if (this.configuration.getSpecialAttributeHandling() != null) {
                        attributeHandlingMap = GenericRESTUtil.getSpecialAttributeMap(this.configuration.getSpecialAttributeHandling());
                    }

                    try {
                        this.addSpecialAttributeValue(oclass, uid.getUidValue(), attributeHandlingMap, specialAttrSearchKey, attr, urlOpTypeList, CONNECTOR_OPERATION.ADDATTRIBUTE.toString(), options.getRunAsUser());
                    } catch (Exception var14) {
                        throw new ConnectorException(this.configuration.getMessage("ex.addAttrFailed", "add attribute operation failed" + var14, var14), var14);
                    }
                }
            }

            log.ok("Method Exiting");
            return uid;
        }
    }

    public Uid removeAttributeValues(
            ObjectClass oclass,
            Uid uid,
            Set<Attribute> attrSet,
            OperationOptions options
    ) {
        log.ok("Method Entered");
        Set<Attribute> specialAttributeset = new HashSet();
        Set<Attribute> normalAttributeSet = new HashSet();
        if (attrSet.isEmpty()) {
            log.error(this.configuration.getMessage("ex.emptyattrset", "emptyattrset"));
            log.ok("Method Exiting");
            throw new ConnectorException(this.configuration.getMessage("ex.emptyattrset", "emptyattrset"));
        } else {
            GenericRESTUtil.separateSpecialAttributeSet(oclass, attrSet, this.relURIsMap, CONNECTOR_OPERATION.REMOVEATTRIBUTE, specialAttributeset, normalAttributeSet);
            if (specialAttributeset.size() > 0) {
                Iterator var7 = specialAttributeset.iterator();

                while (var7.hasNext()) {
                    Attribute attr = (Attribute) var7.next();
                    List<Object> urlOpTypeList = GenericRESTUtil.getURIAndOpType(oclass, CONNECTOR_OPERATION.REMOVEATTRIBUTE, attr.getName(), this.relURIsMap, this.opTypeMap, this.configuration);
                    urlOpTypeList = GenericRESTUtil.handleAttrNamePlaceHoldersInRelURI(oclass, attrSet, urlOpTypeList, this.namedAttributeMap, this.simpleMultivaluedAttributesMap, this.configuration);

                    try {
                        this.removeAttributeValue(oclass, uid.getUidValue(), attr, urlOpTypeList, options.getRunAsUser());
                    } catch (Exception var11) {
                        throw new ConnectorException(this.configuration.getMessage("ex.removeAttrFailed", "remove attribute operation failed" + var11, var11), var11);
                    }
                }
            }

            log.ok("Method Exiting");
            return uid;
        }
    }

    private String removeAttributeValue(
            ObjectClass oclass,
            String uid,
            Attribute attr,
            List<Object> urlOpTypeList,
            String runAsUser
    ) throws Exception {
        log.ok("Method Entered");
        log.info("removeAttributeValue for uid :{0}", uid);

        String attrValToBeRemoved;
        String jsonMapString;
        for (Iterator var6 = attr.getValue().iterator(); var6.hasNext(); this.executeSpecialAttribute(oclass.getObjectClassValue(), attr.getName(), uid, attrValToBeRemoved, jsonMapString, (String) urlOpTypeList.get(0), (RESTCommonConstants.HTTPOperationType) urlOpTypeList.get(1), runAsUser)) {
            Object attrVal = var6.next();
            attrValToBeRemoved = GenericRESTUtil.getValueToBeRemoved(attrVal, this.namedAttributeMap);
            jsonMapString = null;
            if (urlOpTypeList.get(1) != HTTPOperationType.DELETE) {
                Set<Attribute> attSet = new HashSet();
                if (attrVal instanceof EmbeddedObject) {
                    Iterator var11 = ((EmbeddedObject) attrVal).getAttributes().iterator();

                    while (var11.hasNext()) {
                        Attribute attrFromEmbObject = (Attribute) var11.next();
                        attSet.add(attrFromEmbObject);
                    }
                }

                jsonMapString = this.getRequestPayload(oclass, attSet, uid, oclass.getObjectClassValue() + "." + attr.getName(), CONNECTOR_OPERATION.REMOVEATTRIBUTE.toString());
            }
        }

        log.ok("Method Exiting");
        return uid;
    }

    private List<String> executeSpecialAttribute(
            String oclass,
            String attrName,
            String uid,
            String attrId,
            String jsonMapString,
            String relUrl,
            RESTCommonConstants.HTTPOperationType operation,
            String runAsUser
    ) throws Exception {
        String updatedRelUrl = relUrl;
        int pageSize = 0;
        String pagePlaceHolder = null;
        List<String> specialAttributeResponseList = new ArrayList();
        if (GenericRESTUtil.isUidPlaceHolderPresent(relUrl)) {
            updatedRelUrl = relUrl.replace("$(__UID__)$", uid);
        }

        if (operation != null && operation.getValue().equals(HTTPOperationType.GET.name())) {
            pageSize = this.configuration.getPageSize();
            if (updatedRelUrl.contains("$(PAGE_SIZE)$")) {
                if (pageSize > 0) {
                    updatedRelUrl = updatedRelUrl.replace("$(PAGE_SIZE)$", String.valueOf(pageSize));
                } else {
                    updatedRelUrl = GenericRESTUtil.removeQueryPlaceholderFrmUrl(updatedRelUrl, "$(PAGE_SIZE)$");
                }
            }

            if (pageSize > 0 && !StringUtil.isBlank(this.configuration.getPageTokenAttribute()) && updatedRelUrl.contains("$(PAGE_TOKEN)$")) {
                pagePlaceHolder = GenericRESTUtil.getQueryPlaceholderFrmUrl(updatedRelUrl, "$(PAGE_TOKEN)$");
                updatedRelUrl = GenericRESTUtil.removeQueryPlaceholderFrmUrl(updatedRelUrl, "$(PAGE_TOKEN)$");
            } else if (pageSize > 0 && updatedRelUrl.contains("$(PAGE_OFFSET)$")) {
                pagePlaceHolder = GenericRESTUtil.getQueryPlaceholderFrmUrl(updatedRelUrl, "$(PAGE_OFFSET)$");
                updatedRelUrl = GenericRESTUtil.removeQueryPlaceholderFrmUrl(updatedRelUrl, "$(PAGE_OFFSET)$");
            } else if (pageSize > 0 && updatedRelUrl.contains("$(PAGE_INCREMENT)$")) {
                pagePlaceHolder = GenericRESTUtil.getQueryPlaceholderFrmUrl(updatedRelUrl, "$(PAGE_INCREMENT)$");
                updatedRelUrl = GenericRESTUtil.removeQueryPlaceholderFrmUrl(updatedRelUrl, "$(PAGE_INCREMENT)$");
            }
        }

        String specialAttributeResponse;
        if (GenericRESTUtil.isPlaceHolderPresent(updatedRelUrl)) {
            if (attrId == null) {
                throw new ConnectorException(this.configuration.getMessage("ex.invalidArgument", "Invalid argument {0} passed to method {1}", attrId, "executeSpecialAttribute"));
            }

            String objectClassWithAttrPlaceHolder = updatedRelUrl.substring(updatedRelUrl.indexOf("$("), updatedRelUrl.indexOf(")$") + ")$".length());
            specialAttributeResponse = updatedRelUrl.substring(updatedRelUrl.indexOf("$(") + "$(".length(), updatedRelUrl.indexOf(")$"));
            String[] objectClassAttrArray = specialAttributeResponse.split("\\.");
            if (objectClassAttrArray[0].equalsIgnoreCase("__MEMBERSHIP__")) {
                Map<String, String> jsonStringMap = new HashMap();
                String membershipRelUrl = GenericRESTUtil.getMembershipSearchUrl(oclass, uid, attrName, attrId, this.relURIsMap, this.configuration);
                jsonStringMap.put(GenericRESTUtil.getSearchKey("__MEMBERSHIP__", "URL"), membershipRelUrl);
                jsonStringMap.put("__UID__", uid);
                Map<String, Map<String, Object>> conditionMap = GenericRESTUtil.getConditionMapForMembershipSearch(oclass, attrName, attrId, specialAttributeResponse, this.configuration);
                GenericRESTUtil.addJsonResourcesTagToConfigParamsMap(this.parserConfigParamsMap, this.objectClasstoParserConfigMap, GenericRESTUtil.getSearchKey(oclass, "__MEMBERSHIP__", attrName));
                updatedRelUrl = ClientHandler.handlePlaceHolders(jsonStringMap, updatedRelUrl, this.parser, this.connection.getConnection(), this.parserConfigParamsMap, conditionMap);
            } else {
                updatedRelUrl = updatedRelUrl.replace(objectClassWithAttrPlaceHolder, attrId);
            }
        }

        StringBuilder requestUrl = (new StringBuilder(this.configuration.isSslEnabled() ? "https://" : "http://")).append(this.configuration.getHost());
        if (this.configuration.getPort() > 0) {
            requestUrl.append(":").append(this.configuration.getPort());
        }

        requestUrl.append(updatedRelUrl);
        log.info("requestUrl:{0}", requestUrl);
        jsonMapString = this.maskPasswordinLog(jsonMapString, oclass);
        if (operation != null && operation.getValue().equals(HTTPOperationType.GET.name())) {
            if (pagePlaceHolder != null && StringUtil.isNotBlank(pagePlaceHolder)) {
                requestUrl.replace(0, requestUrl.length(), requestUrl + "&" + pagePlaceHolder);
                if (pageSize > 0 && !StringUtil.isBlank(this.configuration.getPageTokenAttribute()) && requestUrl.toString().contains("$(PAGE_TOKEN)$")) {
                    return this.handlePaginationUsingPageTknForSpecialAttribute(oclass, requestUrl.toString(), operation, jsonMapString, runAsUser);
                }

                if ((pageSize <= 0 || !requestUrl.toString().contains("$(PAGE_OFFSET)$")) && !requestUrl.toString().contains("$(PAGE_INCREMENT)$")) {
                    return specialAttributeResponseList;
                }

                return this.handlePaginationUsingOffsetForSpecialAttribute(oclass, requestUrl.toString(), operation, jsonMapString, pageSize, runAsUser);
            }

            requestUrl.replace(0, requestUrl.length(), GenericRESTUtil.removeQueryPlaceholderFrmUrl(requestUrl.toString(), "$(PAGE_SIZE)$"));
            specialAttributeResponse = ClientHandler.executeRequest(this.connection.getConnection(), requestUrl.toString(), operation, jsonMapString, null);
            if (specialAttributeResponse != null) {
                specialAttributeResponseList.add(specialAttributeResponse);
            }

            return specialAttributeResponseList;
        } else {
            specialAttributeResponse = ClientHandler.executeRequest(this.connection.getConnection(), requestUrl.toString(), operation, jsonMapString, null);
            if (specialAttributeResponse != null) {
                specialAttributeResponseList.add(specialAttributeResponse);
            }
        }

        return specialAttributeResponseList;
    }

    private List<String> handlePaginationUsingOffsetForSpecialAttribute(
            String oclass,
            String requestUrl,
            RESTCommonConstants.HTTPOperationType operation,
            String jsonMapString,
            int pageSize,
            String runAsUser
    ) {
        log.info("Begin pagination with offset for special attribute");
        log.info("oclass = [" + oclass + "], requestUrl = [" + requestUrl + "], operation = [" + operation + "], jsonMapString = [" + jsonMapString + "], pageSize = [" + pageSize + "], runAsUser = [" + runAsUser + "]");
        int offset = 0;
        String placeHolder = "$(PAGE_OFFSET)$";
        int searchIndex = 0;
        String actualSearchUrl = null;
        String offSetValueFromJsonResp = null;
        String updatedSearchUrl = requestUrl;
        String jsonResponseString = null;
        boolean isPage = requestUrl.contains("$(PAGE_INCREMENT)$");
        List<Map<String, Object>> jsonResponseMap = null;
        actualSearchUrl = requestUrl;
        String pageOffsetAttribute = this.configuration.getPageOffsetAttribute();
        List<String> specialAttributeResponseList = new ArrayList();
        Map<String, String> pageTokenParserConfigMap = new HashMap();
        GenericRESTUtil.addJsonResourcesTagToConfigParamsMap(pageTokenParserConfigMap, this.objectClasstoParserConfigMap, oclass);
        if (isPage) {
            placeHolder = "$(PAGE_INCREMENT)$";
            offset = 1;
        }

        GenericRESTUtil.addPageAttrToConfigParamsMap(pageTokenParserConfigMap, pageOffsetAttribute);

        do {
            if (pageOffsetAttribute != null && offSetValueFromJsonResp == null) {
                updatedSearchUrl = GenericRESTUtil.removeQueryPlaceholderFrmUrl(updatedSearchUrl, placeHolder);
            } else {
                updatedSearchUrl = updatedSearchUrl.replace(placeHolder, String.valueOf(offset));
            }

            jsonResponseString = ClientHandler.executeRequest(this.connection.getConnection(), updatedSearchUrl, operation, jsonMapString, null);
            if (jsonResponseString != null) {
                specialAttributeResponseList.add(jsonResponseString);
            }

            jsonResponseMap = this.parser.parseResponse(jsonResponseString, pageTokenParserConfigMap);
            searchIndex = this.getPageAttributeSearchIndex(jsonResponseMap, pageOffsetAttribute);
            log.info("searchIndex = " + searchIndex);
            if (searchIndex >= 0) {
                offSetValueFromJsonResp = (String) jsonResponseMap.get(searchIndex).get(pageOffsetAttribute);
            } else {
                offSetValueFromJsonResp = null;
            }

            log.info("ForSpecialAttribute, pageOffsetAttribute = " + pageOffsetAttribute + " " + "offSetValueFromJsonResp = " + offSetValueFromJsonResp);
            if (StringUtil.isNotBlank(pageOffsetAttribute) && jsonResponseMap != null && jsonResponseMap.size() > 0 && offSetValueFromJsonResp != null) {
                offset = Integer.parseInt(offSetValueFromJsonResp);
            } else if (isPage) {
                ++offset;
            } else {
                offset += pageSize;
            }

            updatedSearchUrl = actualSearchUrl;
            log.ok(jsonResponseMap != null ? " For SpecialAttribute, jsonResponseMap.size() = " + jsonResponseMap.size() : "Received null jsonResponseMap for Special Attribute");
            log.info(" ForSpecialAttribute, New offset = " + offset + ", pageSize= " + pageSize + " && " + "offSetValueFromJsonResp = " + offSetValueFromJsonResp);
        } while (!"AOB_TEST_CONN_USER".equals(runAsUser) && jsonResponseMap != null && !jsonResponseMap.isEmpty() && pageSize != 0 && jsonResponseMap.size() == pageSize);

        log.info("End pagination with offset for special attribute");
        return specialAttributeResponseList;
    }

    private List<String> handlePaginationUsingPageTknForSpecialAttribute(
            String oclass,
            String requestUrl,
            RESTCommonConstants.HTTPOperationType operation,
            String jsonMapString,
            String runAsUser
    ) {
        log.info("Begin pagination with page token for special attribute");
        String pageToken = null;
        String actualSearchUrl = null;
        String jsonResponseString = null;
        int searchIndex = 0;
        List<Map<String, Object>> jsonResponseMap = null;
        String updatedSearchUrl = requestUrl;
        String pageTokenRegex = this.configuration.getPageTokenRegex();
        List<String> specialAttributeResponseList = new ArrayList();
        Map<String, String> pageTokenParserConfigMap = new HashMap();
        GenericRESTUtil.addJsonResourcesTagToConfigParamsMap(pageTokenParserConfigMap, this.objectClasstoParserConfigMap, oclass);
        GenericRESTUtil.addPageAttrToConfigParamsMap(pageTokenParserConfigMap, this.configuration.getPageTokenAttribute());

        do {
            if (pageToken == null) {
                actualSearchUrl = updatedSearchUrl;
                updatedSearchUrl = GenericRESTUtil.removeQueryPlaceholderFrmUrl(updatedSearchUrl, "$(PAGE_TOKEN)$");
            } else {
                updatedSearchUrl = updatedSearchUrl.replace("$(PAGE_TOKEN)$", pageToken);
            }

            jsonResponseString = ClientHandler.executeRequest(this.connection.getConnection(), updatedSearchUrl, operation, jsonMapString, null);
            if (jsonResponseString != null) {
                specialAttributeResponseList.add(jsonResponseString);
            }

            jsonResponseMap = this.parser.parseResponse(jsonResponseString, pageTokenParserConfigMap);
            searchIndex = this.getPageAttributeSearchIndex(jsonResponseMap, this.configuration.getPageTokenAttribute());
            if (searchIndex >= 0) {
                pageToken = (String) jsonResponseMap.get(searchIndex).get(this.configuration.getPageTokenAttribute());
            } else {
                pageToken = null;
            }

            if (pageToken != null && pageTokenRegex != null) {
                Pattern regex = Pattern.compile(pageTokenRegex);
                Matcher regexMatcher = regex.matcher(pageToken);
                if (!regexMatcher.find()) {
                    log.error("For the given pageTokenRegex, match can't be found in the target response.");
                    throw new ConfigurationException(this.configuration.getMessage("ex.pageTokenRegexNoMatch", "For the given pagination token regular expression, match can't be found in the target response."));
                }

                pageToken = regexMatcher.group();
            }

            log.info("special attribute page token:{0}", pageToken);
            updatedSearchUrl = actualSearchUrl;
        } while (pageToken != null && !"AOB_TEST_CONN_USER".equals(runAsUser));

        log.info("End pagination with page token for special attribute");
        return specialAttributeResponseList;
    }

    private int getPageAttributeSearchIndex(
            List<Map<String, Object>> jsonResponseMap,
            String pageAttribute
    ) {
        int searchIndex = -1;
        if (pageAttribute != null && StringUtil.isNotBlank(pageAttribute)) {
            for (int i = 0; i < jsonResponseMap.size(); ++i) {
                Set<String> keySet = ((Map) jsonResponseMap.get(i)).keySet();
                if (keySet.contains(pageAttribute)) {
                    searchIndex = i;
                    break;
                }
            }
        }

        return searchIndex;
    }

    private String maskPasswordinLog(
            String jsonMapString,
            String oclass
    ) {
        String logPayload = null;
        if (StringUtil.isNotBlank(jsonMapString)) {
            if (this.configuration.getHttpHeaderContentType().equalsIgnoreCase("application/json")) {
                List<Map<String, Object>> payload = this.parser.parseResponse(jsonMapString, this.parserConfigParamsMap);
                if (payload.size() > 0) {
                    logPayload = ClientHandler.handlePasswordInLogs(this.parser, payload.get(0), (String) ((Map) this.namedAttributeMap.get(oclass)).get(OperationalAttributes.PASSWORD_NAME), this.parserConfigParamsMap);
                }
            } else {
                logPayload = ClientHandler.handlePasswordInLogs(jsonMapString);
                if (jsonMapString.indexOf("##") != -1) {
                    jsonMapString = jsonMapString.replaceAll("##", "");
                }
            }

            log.info("json payload :{0}", logPayload);
        }

        return jsonMapString;
    }

    private String getRequestPayload(
            ObjectClass oclass,
            Set<Attribute> attrSet,
            String uid,
            String searchKey,
            String operation
    ) {
        log.info("Method Entered");
        log.info("Creating request payload from attrSet, {0}", attrSet);
        String requestPayload = null;
        List<String> multiVal = new ArrayList();
        if (this.simpleMultivaluedAttributesMap != null && this.simpleMultivaluedAttributesMap.containsKey(oclass.getObjectClassValue())) {
            multiVal = this.simpleMultivaluedAttributesMap.get(oclass.getObjectClassValue());
        }

        log.info(" simple Multi-valued Attributes are, {0}", multiVal);
        Map<String, String> namedAttributes = this.namedAttributeMap.get(oclass.getObjectClassValue());
        if (namedAttributes == null) {
            log.error("Unable to find UID and Name attributes for the given object class {0}.", oclass.getObjectClassValue());
            throw new ConnectorException(this.configuration.getMessage("ex.nullNamedAttributes", "Unable to find UID and Name attributes for the given object class " + oclass.getObjectClassValue() + ".", oclass.getObjectClassValue()));
        } else {
            log.info(" namedAttributes are, {0}", namedAttributes);
            Set<Attribute> updAttrSet = new HashSet(attrSet);
            log.info(" updAttrSet from attrset =  " + updAttrSet);
            log.info(" Processing status attributes ..");
            log.info("configuration : StatusEnableValue={0}, StatusDisableValue={1}, StatusAttributes={2} ", this.configuration.getStatusEnableValue(), this.configuration.getStatusDisableValue(), Arrays.toString(this.configuration.getStatusAttributes()));
            Attribute passwordAttr;
            boolean checkForUpdateOp;
            AttributeBuilder attrBuilder;
            if (!StringUtil.isBlank(this.configuration.getStatusEnableValue()) && !StringUtil.isBlank(this.configuration.getStatusDisableValue())) {
                passwordAttr = AttributeUtil.find("__ENABLE__", updAttrSet);
                if (passwordAttr != null) {
                    updAttrSet.remove(passwordAttr);
                    checkForUpdateOp = AttributeUtil.getBooleanValue(passwordAttr);
                    attrBuilder = new AttributeBuilder();
                    attrBuilder.setName("__ENABLE__");
                    if (checkForUpdateOp) {
                        attrBuilder.addValue(this.configuration.getStatusEnableValue());
                    } else {
                        attrBuilder.addValue(this.configuration.getStatusDisableValue());
                    }

                    updAttrSet.add(attrBuilder.build());
                }
            }

            log.info(" updAttrSet after processing status attributes =  " + updAttrSet);
            log.info(" Processing password attribute ..");
            if (!StringUtil.isBlank(this.configuration.getPasswordAttribute())) {
                passwordAttr = AttributeUtil.find("__PASSWORD__", updAttrSet);
                if (passwordAttr != null) {
                    updAttrSet.remove(passwordAttr);
                    String passwordValue = GenericRESTUtil.decryptPassword((GuardedString) passwordAttr.getValue().get(0));
                    attrBuilder = new AttributeBuilder();
                    attrBuilder.setName(namedAttributes.get(OperationalAttributes.PASSWORD_NAME));
                    attrBuilder.addValue(passwordValue);
                    updAttrSet.add(attrBuilder.build());
                }
            }

            log.info(" Processed status and password attributes in updAttrSet");
            Map<String, Object> jsonMap = ClientHandler.convertAttrSetToMap(oclass, updAttrSet, null, multiVal, namedAttributes);

            try {
                checkForUpdateOp = operation.equalsIgnoreCase(CONNECTOR_OPERATION.ADDATTRIBUTE.toString()) || operation.equalsIgnoreCase(CONNECTOR_OPERATION.REMOVEATTRIBUTE.toString());

                String mapKey = GenericRESTUtil.getSearchKey(searchKey, operation);
                String customPayload = this.customPayloadMap.get(mapKey);
                if (StringUtil.isBlank(customPayload) && checkForUpdateOp) {
                    mapKey = GenericRESTUtil.getSearchKey(searchKey, CONNECTOR_OPERATION.UPDATEOP.toString());
                    customPayload = this.customPayloadMap.get(mapKey);
                }

                log.info(" customPayload before processing place holders is, {0}", customPayload);
                if (StringUtil.isBlank(customPayload)) {
                    requestPayload = this.parser.parseRequest(jsonMap, this.parserConfigParamsMap);
                } else {
                    if (customPayload.contains("$(") && customPayload.contains(")$")) {
                        Map<String, String> jsonStringMap = new HashMap();
                        jsonStringMap.put("__UID__", uid);
                        Iterator var15 = jsonMap.keySet().iterator();

                        while (var15.hasNext()) {
                            String key = (String) var15.next();
                            jsonStringMap.put(RESTCommonUtils.handleSpecialAttributeInCustomPayload(key, namedAttributes), jsonMap.get(key) != null ? jsonMap.get(key).toString() : null);
                        }

                        customPayload = ClientHandler.handlePlaceHolders(jsonStringMap, customPayload, this.parser, null, null, null);
                    }

                    requestPayload = customPayload;
                }
            } catch (Exception var17) {
                log.error("Exception occurred while creating request payload, {0}", var17);
                throw new ConnectorException(this.configuration.getMessage("ex.requestPayload", "Exception occurred while creating request payload.") + " " + var17.getMessage(), var17);
            }

            log.info("Method Exiting");
            return requestPayload;
        }
    }

    public void delete(
            ObjectClass oclass,
            Uid uid,
            OperationOptions options
    ) {
        log.ok("Method Entered");
        if (uid != null) {
            log.info("Deleting {0} with uid value:{1}", oclass.getObjectClassValue(), uid.getUidValue());
            List<Object> urlOpTypeList = GenericRESTUtil.getURIAndOpType(oclass, CONNECTOR_OPERATION.DELETEOP, null, this.relURIsMap, this.opTypeMap, this.configuration);
            String jsonResponseString = this.executeRequest(oclass, uid, urlOpTypeList, null, null);
            log.info("delete response :{0}", jsonResponseString);
            log.info("Method Exiting");
        } else {
            log.ok("Method Exiting");
            throw new UnknownUidException();
        }
    }

    public FilterTranslator<String> createFilterTranslator(
            ObjectClass oclass,
            OperationOptions operationOptions
    ) {
        log.ok("Method Entered");
        log.info("Checking the ObjectClass  ");
        if (oclass == null) {
            log.info("Method Exiting");
            throw new IllegalArgumentException(this.configuration.getMessage("object.class.required", "Operation requires an 'ObjectClass'."));
        } else {
            log.ok("The ObjectClass is fine");
            log.ok("Method Exiting");
            return new GenericRESTFilterTranslator(this, String.class);
        }
    }

    public void executeQuery(
            ObjectClass oclass,
            String query,
            ResultsHandler handler,
            OperationOptions options
    ) {
        log.info("Method Entered");
        log.info("query string = {0} attributes to get = {1} ", query, options.getAttributesToGet());
        log.error("Perf: executeQuery Entered for objectClass {0}", oclass.getObjectClassValue());
        String[] attributesToGet = options.getAttributesToGet();
        if (attributesToGet == null) {
            log.error("Attributes to get from the target is empty.");
            throw new ConnectorException(this.configuration.getMessage("ex.nullAttributesToGet", "Attributes to get from the target is empty."));
        } else {
            List<Object> urlOpTypeList = GenericRESTUtil.getURIAndOpType(oclass, CONNECTOR_OPERATION.SEARCHOP, null, this.relURIsMap, this.opTypeMap, this.configuration);
            Set<String> specialAttributeset = GenericRESTUtil.getSpecialAttributeSet(oclass, attributesToGet, this.relURIsMap, CONNECTOR_OPERATION.SEARCHOP);
            String filterSuffix;
            if (query != null && query.indexOf("__UID__=") == 0) {
                filterSuffix = query.substring("__UID__=".length());
                log.ok("uidValue in UID equals filter query: " + filterSuffix);
                urlOpTypeList = GenericRESTUtil.getURIAndOpType(oclass, CONNECTOR_OPERATION.SEARCHOP, "__UID__", this.relURIsMap, this.opTypeMap, this.configuration);
                log.ok("UID SearchOp, urlOpTypeList: " + urlOpTypeList);
                this.handleUIDPlaceHolder(urlOpTypeList, filterSuffix);
            }

            filterSuffix = (String) options.getOptions().get("Filter Suffix");
            int pageSize = this.configuration.getPageSize();
            this.handleBatchSizePlaceHolder(urlOpTypeList, pageSize);
            log.info("pageSize: {0}", pageSize);
            if (pageSize > 0 && !StringUtil.isBlank(this.configuration.getPageTokenAttribute()) && ((String) urlOpTypeList.get(0)).contains("$(PAGE_TOKEN)$")) {
                this.handlePaginationUsingPageTkn(urlOpTypeList, filterSuffix, oclass, options, handler, specialAttributeset);
            } else if ((pageSize <= 0 || !((String) urlOpTypeList.get(0)).contains("$(PAGE_OFFSET)$")) && !((String) urlOpTypeList.get(0)).contains("$(PAGE_INCREMENT)$")) {
                if (((String) urlOpTypeList.get(0)).contains("$(PAGE_TOKEN)$")) {
                    urlOpTypeList.set(0, GenericRESTUtil.removeQueryPlaceholderFrmUrl((String) urlOpTypeList.get(0), "$(PAGE_TOKEN)$"));
                }

                if (((String) urlOpTypeList.get(0)).contains("$(PAGE_OFFSET)$")) {
                    urlOpTypeList.set(0, GenericRESTUtil.removeQueryPlaceholderFrmUrl((String) urlOpTypeList.get(0), "$(PAGE_OFFSET)$"));
                }

                if (((String) urlOpTypeList.get(0)).contains("$(PAGE_INCREMENT)$")) {
                    urlOpTypeList.set(0, GenericRESTUtil.removeQueryPlaceholderFrmUrl((String) urlOpTypeList.get(0), "$(PAGE_INCREMENT)$"));
                }

                this.handleSearchResult(oclass, options, handler, urlOpTypeList, filterSuffix, specialAttributeset);
            } else {
                this.handlePaginationUsingOffset(urlOpTypeList, pageSize, filterSuffix, oclass, options, handler, specialAttributeset);
            }

            log.error("Perf: executeQuery Exiting for objectClass {0}", oclass.getObjectClassValue());
            log.info("Method Exiting");
        }
    }

    private void handlePaginationUsingOffset(
            List<Object> urlOpTypeList,
            int pageSize,
            String filterSuffix,
            ObjectClass oclass,
            OperationOptions options,
            ResultsHandler handler,
            Set<String> specialAttributeset
    ) {
        log.info("Begin pagination with offset for object class " + oclass.getObjectClassValue());
        log.info("Input pageSize = [" + pageSize + "], filterSuffix = [" + filterSuffix + "], options Set= [" + options.getOptions().entrySet() + " attributes to get=[" + options.getAttributesToGet() + "], handler = [" + handler + "], specialAttributeSet = [" + specialAttributeset + "]");
        int offset = 0;
        String placeHolder = "$(PAGE_OFFSET)$";
        int searchIndex = 0;
        String actualSearchUrl = null;
        String offSetValueFromJsonResp = null;
        boolean isPage = ((String) urlOpTypeList.get(0)).contains("$(PAGE_INCREMENT)$");
        String updatedSearchUrl = (String) urlOpTypeList.get(0);
        List<Map<String, Object>> jsonResponseMap = null;
        actualSearchUrl = updatedSearchUrl;
        Map<String, String> pageTokenParserConfigMap = new HashMap();
        String pageOffsetAttribute = this.configuration.getPageOffsetAttribute();
        GenericRESTUtil.addJsonResourcesTagToConfigParamsMap(pageTokenParserConfigMap, this.objectClasstoParserConfigMap, oclass.getObjectClassValue());
        if (isPage) {
            placeHolder = "$(PAGE_INCREMENT)$";
            offset = 1;
        }

        GenericRESTUtil.addPageAttrToConfigParamsMap(pageTokenParserConfigMap, pageOffsetAttribute);

        do {
            if (pageOffsetAttribute != null && offSetValueFromJsonResp == null) {
                updatedSearchUrl = GenericRESTUtil.removeQueryPlaceholderFrmUrl(updatedSearchUrl, placeHolder);
            } else {
                updatedSearchUrl = updatedSearchUrl.replace(placeHolder, String.valueOf(offset));
            }

            urlOpTypeList.set(0, updatedSearchUrl);
            log.info("updatedSearchUrl = " + updatedSearchUrl);
            String jsonResponseString = this.handleSearchResult(oclass, options, handler, urlOpTypeList, filterSuffix, specialAttributeset);
            jsonResponseMap = this.parser.parseResponse(jsonResponseString, pageTokenParserConfigMap);
            if (filterSuffix != null && filterSuffix.startsWith("/")) {
                break;
            }
            searchIndex = this.getPageAttributeSearchIndex(jsonResponseMap, pageOffsetAttribute);
            log.info("searchIndex = " + searchIndex);
            if (searchIndex >= 0) {
                offSetValueFromJsonResp = (String) jsonResponseMap.get(searchIndex).get(pageOffsetAttribute);
            } else {
                offSetValueFromJsonResp = null;
            }

            log.info("pageOffsetAttribute = " + pageOffsetAttribute + " offSetValueFromJsonResp = " + offSetValueFromJsonResp);
            if (StringUtil.isNotBlank(pageOffsetAttribute) && jsonResponseMap != null && jsonResponseMap.size() > 0 && offSetValueFromJsonResp != null) {
                offset = Integer.parseInt(offSetValueFromJsonResp);
            } else if (isPage) {
                ++offset;
            } else {
                offset += pageSize;
            }

            log.ok(jsonResponseMap != null ? " jsonResponseMap.size() = " + jsonResponseMap.size() : "Received null jsonResponseMap");
            log.info(" New offset = " + offset + ", pageSize= " + pageSize + " && " + "offSetValueFromJsonResp" + " " + "= " + offSetValueFromJsonResp);
            updatedSearchUrl = actualSearchUrl;
        } while (!"AOB_TEST_CONN_USER".equals(options.getRunAsUser()) && jsonResponseMap != null && !jsonResponseMap.isEmpty() && pageSize != 0 && jsonResponseMap.size() == pageSize);

        log.info("End pagination with offset for object Class : " + oclass.getObjectClassValue());
    }

    private void handlePaginationUsingPageTkn(
            List<Object> urlOpTypeList,
            String filterSuffix,
            ObjectClass oclass,
            OperationOptions options,
            ResultsHandler handler,
            Set<String> specialAttributeset
    ) {
        log.info("Begin pagination with page token");
        String pageToken = null;
        String actualSearchUrl = null;
        int searchIndex = 0;
        List<Map<String, Object>> jsonResponseMap = null;
        String updatedSearchUrl = (String) urlOpTypeList.get(0);
        String pageTokenRegex = this.configuration.getPageTokenRegex();
        Map<String, String> pageTokenParserConfigMap = new HashMap();
        GenericRESTUtil.addJsonResourcesTagToConfigParamsMap(pageTokenParserConfigMap, this.objectClasstoParserConfigMap, oclass.getObjectClassValue());
        GenericRESTUtil.addPageAttrToConfigParamsMap(pageTokenParserConfigMap, this.configuration.getPageTokenAttribute());

        do {
            if (pageToken == null) {
                actualSearchUrl = updatedSearchUrl;
                updatedSearchUrl = GenericRESTUtil.removeQueryPlaceholderFrmUrl(updatedSearchUrl, "$(PAGE_TOKEN)$");
            } else {
                updatedSearchUrl = updatedSearchUrl.replace("$(PAGE_TOKEN)$", pageToken);
            }

            urlOpTypeList.set(0, updatedSearchUrl);
            log.info("Updated search URL is {0}", updatedSearchUrl);
            String jsonResponseString = this.handleSearchResult(oclass, options, handler, urlOpTypeList, filterSuffix, specialAttributeset);
            jsonResponseMap = this.parser.parseResponse(jsonResponseString, pageTokenParserConfigMap);
            log.info("handlePaginationUsingPageTkn : jsonResponseMap: {0}", jsonResponseMap);
            searchIndex = this.getPageAttributeSearchIndex(jsonResponseMap, this.configuration.getPageTokenAttribute());
            if (searchIndex >= 0) {
                pageToken = (String) jsonResponseMap.get(searchIndex).get(this.configuration.getPageTokenAttribute());
            } else {
                pageToken = null;
            }

            if (pageToken != null && pageTokenRegex != null) {
                Pattern regex = Pattern.compile(pageTokenRegex);
                Matcher regexMatcher = regex.matcher(pageToken);
                if (!regexMatcher.find()) {
                    log.error("For the given pageTokenRegex, match can't be found in the target response.");
                    throw new ConfigurationException(this.configuration.getMessage("ex.pageTokenRegexNoMatch", "For the given pagination token regular expression, match can't be found in the target response."));
                }

                pageToken = regexMatcher.group();
            }

            log.info("page token:{0}", pageToken);
            updatedSearchUrl = actualSearchUrl;
        } while (pageToken != null && !"AOB_TEST_CONN_USER".equals(options.getRunAsUser()));

        log.info("End pagination with page token");
    }

    private void handleBatchSizePlaceHolder(
            List<Object> urlOpTypeList,
            int pageSize
    ) {
        String searchUrl = (String) urlOpTypeList.get(0);
        if (searchUrl.contains("$(PAGE_SIZE)$")) {
            if (pageSize > 0) {
                searchUrl = searchUrl.replace("$(PAGE_SIZE)$", String.valueOf(pageSize));
            } else {
                searchUrl = GenericRESTUtil.removeQueryPlaceholderFrmUrl(searchUrl, "$(PAGE_SIZE)$");
            }

            urlOpTypeList.set(0, searchUrl);
        }

    }

    private void handleUIDPlaceHolder(
            List<Object> urlOpTypeList,
            String uidValue
    ) {
        String searchUrl = (String) urlOpTypeList.get(0);
        log.ok("searchUrl: " + searchUrl);
        log.ok("uidValue: " + uidValue);
        if (GenericRESTUtil.isUidPlaceHolderPresent(searchUrl)) {
            if (uidValue == null) {
                log.error("Unable to replace the UId place holder in configured URL.");
                throw new ConnectorException(this.configuration.getMessage("ex.replaceUIdPlaceholder", "Unable to replace the UId place holder in configured URL."));
            }

            searchUrl = searchUrl.replace("$(__UID__)$", uidValue);
            log.ok("After replacing UID place holder, searchUrl: " + searchUrl);
            urlOpTypeList.set(0, searchUrl);
        }

    }

    private String handleSearchResult(
            ObjectClass oclass,
            OperationOptions options,
            ResultsHandler handler,
            List<Object> urlOpTypeList,
            String filterSuffix,
            Set<String> specialAttributeset
    ) {
        String jsonResponseString = this.executeRequest(oclass, null, urlOpTypeList, null, filterSuffix);
        jsonResponseString = this.maskPasswordinLog(jsonResponseString, oclass.getObjectClassValue());
        GenericRESTUtil.addJsonResourcesTagToConfigParamsMap(this.parserConfigParamsMap, this.objectClasstoParserConfigMap, oclass.getObjectClassValue());
        log.info("jsonResponseString: {0}", jsonResponseString);
        List<Map<String, Object>> jsonResponseMap = this.parser.parseResponse(jsonResponseString, this.parserConfigParamsMap);

        try {
            Iterator var9 = jsonResponseMap.iterator();

            while (var9.hasNext()) {
                Map<String, Object> entityMap = (Map) var9.next();
                Map<String, String> namedAttributes = this.namedAttributeMap.get(oclass.getObjectClassValue());
                Object uidValue = GenericRESTUtil.getAttributeValue(namedAttributes.get(Uid.NAME), entityMap);
                if (uidValue != null) {
                    this.makeConnectorObject(handler, options, oclass, entityMap, specialAttributeset);
                }
            }
        } catch (Exception var14) {
            log.error("Exception in executeQuery, {0}", var14);
            throw new ConnectorException(this.configuration.getMessage("ex.executeQuery", "Exception in executeQuery.") + " " + var14.getMessage(), var14);
        }

        try {
            Instant tokenExpireTime = this.connection.getConnectionEndTime();
            Instant currentTime = Instant.now();
            log.info("token Expire Time: {0} and current time: {1}", tokenExpireTime, currentTime);
            if (tokenExpireTime != null && currentTime.isAfter(tokenExpireTime)) {
                this.dispose();
                log.info("requesting new connection");
                this.connection = new GenericRESTConnection(this.configuration);
            }
        } catch (Exception var13) {
            log.error("Exception in fetching the new access token after token validity expired: {0}", var13);
        }

        return jsonResponseString;
    }

    private String executeUpdate(
            ObjectClass oclass,
            Uid uid,
            Set<Attribute> attrSet
    ) {
        log.ok("Method Entered");
        List<Object> urlOpTypeList = GenericRESTUtil.getURIAndOpType(oclass, CONNECTOR_OPERATION.UPDATEOP, null, this.relURIsMap, this.opTypeMap, this.configuration);
        if (!GenericRESTUtil.isUidPlaceHolderPresent(urlOpTypeList.get(0).toString())) {
            attrSet.add(uid);
        }

        String jsonMapString = this.getRequestPayload(oclass, attrSet, uid.getUidValue(), oclass.getObjectClassValue(), CONNECTOR_OPERATION.UPDATEOP.toString());
        urlOpTypeList = GenericRESTUtil.handleAttrNamePlaceHoldersInRelURI(oclass, attrSet, urlOpTypeList, this.namedAttributeMap, this.simpleMultivaluedAttributesMap, this.configuration);
        String jsonResponseString = this.executeRequest(oclass, uid, urlOpTypeList, jsonMapString, null);
        log.info("update response :{0}", jsonResponseString);
        if (jsonResponseString != null) {
            GenericRESTUtil.addJsonResourcesTagToConfigParamsMap(this.parserConfigParamsMap, this.objectClasstoParserConfigMap, oclass.getObjectClassValue());
            jsonResponseString = this.maskPasswordinLog(jsonResponseString, oclass.getObjectClassValue());
            Map<String, Object> jsonResponseMap = this.parser.parseResponse(jsonResponseString, this.parserConfigParamsMap).get(0);
            Object returnValue = jsonResponseMap.get(((Map) this.namedAttributeMap.get(oclass.getObjectClassValue())).get("__UID__"));
            if (returnValue != null) {
                log.ok("Method Exiting");
                return String.valueOf(returnValue);
            }
        }

        log.ok("Method Exiting");
        return null;
    }

    private void addSpecialAttributeValue(
            ObjectClass oclass,
            String uid,
            Map<String, String> specialAttributeHandlingMap,
            String searchKey,
            Attribute attr,
            List<Object> urlOpTypeList,
            String operation,
            String runAsUser
    ) throws Exception {
        log.info("addSpecialAttributeValue of :{0}", uid);
        Set<Attribute> attributeSet = new HashSet();
        String attrId = null;
        String jsonMapString = null;
        String specialAttrURL = (String) urlOpTypeList.get(0);

        try {
            boolean checkForUpdateOp = operation.equalsIgnoreCase(CONNECTOR_OPERATION.ADDATTRIBUTE.toString()) || operation.equalsIgnoreCase(CONNECTOR_OPERATION.REMOVEATTRIBUTE.toString());

            String key = GenericRESTUtil.getSearchKey(searchKey, operation);
            boolean isSingleHandling = specialAttributeHandlingMap.get(key) != null && specialAttributeHandlingMap.get(key).contains("SINGLE");
            if (!isSingleHandling && checkForUpdateOp) {
                key = GenericRESTUtil.getSearchKey(searchKey, CONNECTOR_OPERATION.UPDATEOP.toString());
                isSingleHandling = specialAttributeHandlingMap.get(key) != null && specialAttributeHandlingMap.get(key).contains("SINGLE");
            }

            if (isSingleHandling) {
                List<Object> attrList = attr.getValue();
                Iterator<Object> listIterator = attrList.iterator();

                while (listIterator.hasNext()) {
                    Object attributeValue = listIterator.next();
                    AttributeBuilder attrBuilder = new AttributeBuilder();
                    String uidAttrName = (String) ((Map) this.namedAttributeMap.get(oclass.getObjectClassValue())).get("__UID__");
                    if (!GenericRESTUtil.isUidPlaceHolderPresent(specialAttrURL) && GenericRESTUtil.isPlaceHolderPresent(specialAttrURL)) {
                        attrBuilder.setName(uidAttrName);
                        attrBuilder.addValue(uid);
                        attributeSet.add(attrBuilder.build());
                        attrId = GenericRESTUtil.getAttributeIdFromURL(specialAttrURL, attributeValue, attributeSet);
                    } else if (GenericRESTUtil.isUidPlaceHolderPresent(specialAttrURL) && GenericRESTUtil.isNonUidPlaceHolderPresent(specialAttrURL)) {
                        String tempAttURL = specialAttrURL.replace("$(__UID__)$", "");
                        attrId = GenericRESTUtil.getAttributeIdFromURL(tempAttURL, attributeValue, attributeSet);
                    } else {
                        if (attributeValue instanceof EmbeddedObject) {
                            attributeSet.addAll(((EmbeddedObject) attributeValue).getAttributes());
                        } else {
                            attrBuilder.setName(attr.getName());
                            attrBuilder.addValue(attributeValue);
                            attributeSet.add(attrBuilder.build());
                        }

                        if (!GenericRESTUtil.isPlaceHolderPresent(specialAttrURL)) {
                        }
                    }

                    jsonMapString = this.getRequestPayload(oclass, attributeSet, uid, searchKey, operation);
                    this.executeSpecialAttribute(oclass.getObjectClassValue(), attr.getName(), uid, attrId, jsonMapString, specialAttrURL, (RESTCommonConstants.HTTPOperationType) urlOpTypeList.get(1), runAsUser);
                    attributeSet.clear();
                }
            } else {
                attributeSet.add(attr);
                jsonMapString = this.getRequestPayload(oclass, attributeSet, uid, searchKey, operation);
                this.executeSpecialAttribute(oclass.getObjectClassValue(), attr.getName(), uid, null, jsonMapString, specialAttrURL, (RESTCommonConstants.HTTPOperationType) urlOpTypeList.get(1), runAsUser);
            }

        } catch (Exception var22) {
            log.error("Exception while adding special attribute, {0}", var22);
            throw new ConnectorException(this.configuration.getMessage("ex.addAttrFailed", "add attribute operation failed") + " " + var22.getMessage(), var22);
        }
    }

    private String executeRequest(
            ObjectClass oclass,
            Uid uid,
            List<Object> urlOpTypeList,
            String jsonMapString,
            String query
    ) {
        StringBuilder requestUrl = (new StringBuilder(this.configuration.isSslEnabled() ? "https://" : "http://")).append(this.configuration.getHost());
        if (this.configuration.getPort() > 0) {
            requestUrl.append(":").append(this.configuration.getPort());
        }

        requestUrl.append(urlOpTypeList.get(0));
        String requestUrlStr = requestUrl.toString();
        int charIndex;
        if (query != null && requestUrlStr.contains("?$(Filter Suffix)$") && query.startsWith("/")) {
            requestUrlStr = requestUrlStr.replace("?$(Filter Suffix)$", query);
            charIndex = requestUrlStr.indexOf(query) + query.length();
            if (charIndex + 1 < requestUrlStr.length() && requestUrlStr.charAt(charIndex) == '&') {
                requestUrlStr = requestUrlStr.substring(0, charIndex) + "?" + requestUrlStr.substring(charIndex + 1);
            }
        } else if (query != null && !query.trim().equalsIgnoreCase("") && requestUrlStr.contains("$(Filter Suffix)$")) {
            requestUrlStr = requestUrlStr.replace("/$(Filter Suffix)$", query);
            requestUrlStr = requestUrlStr.replace("$(Filter Suffix)$", query);
        } else if ((query == null || query.trim().equalsIgnoreCase("")) && requestUrlStr.contains("$(Filter Suffix)$")) {
            requestUrlStr = requestUrlStr.replace("/$(Filter Suffix)$", "");
            requestUrlStr = requestUrlStr.replace("?$(Filter Suffix)$", "?");
            requestUrlStr = requestUrlStr.replace("$(Filter Suffix)$", "");
            charIndex = requestUrlStr.indexOf("?");
            if (charIndex + 1 < requestUrlStr.length() && requestUrlStr.charAt(charIndex + 1) == '&') {
                requestUrlStr = requestUrlStr.substring(0, charIndex + 1) + requestUrlStr.substring(charIndex + 2);
            }

            if (requestUrlStr.endsWith("?")) {
                requestUrlStr = requestUrlStr.substring(0, requestUrlStr.length() - 1);
            }
        }

        String incrementalReconAttribute;
        if (GenericRESTUtil.isUidPlaceHolderPresent(requestUrlStr)) {
            incrementalReconAttribute = uid != null ? uid.getUidValue() : null;
            if (incrementalReconAttribute == null) {
                log.error("Unable to replace the UId place holder in configured URL.");
                throw new ConnectorException(this.configuration.getMessage("ex.replaceUIdPlaceholder", "Unable to replace the Uid place holder in configured URL."));
            }

            requestUrlStr = requestUrlStr.replace("$(__UID__)$", incrementalReconAttribute);
        }

        if (GenericRESTUtil.isAnyIncrementalReconPlaceHolderPresent(requestUrlStr)) {
            incrementalReconAttribute = null;
            String latestToken = null;
            if (this.incrementalReconParamsMap != null) {
                incrementalReconAttribute = this.incrementalReconParamsMap.get("Incremental Recon Attribute");
                latestToken = this.incrementalReconParamsMap.get("Latest Token");
                log.info("incrementalReconAttribute is {0}, latestToken is {1} , request URL is {2}", incrementalReconAttribute, latestToken, requestUrlStr);
                if (requestUrlStr.contains("$(Incremental Recon Attribute)$") && StringUtil.isNotEmpty(incrementalReconAttribute)) {
                    requestUrlStr = requestUrlStr.replace("$(Incremental Recon Attribute)$", incrementalReconAttribute);
                }

                if (requestUrlStr.contains("$(Latest Token)$") && StringUtil.isNotEmpty(latestToken)) {
                    requestUrlStr = requestUrlStr.replace("$(Latest Token)$", latestToken);
                }

                log.info(" Replaced incremental recon attributes requestURL is {0}", requestUrlStr);
            }

            if (GenericRESTUtil.isAnyIncrementalReconPlaceHolderPresent(requestUrlStr)) {
                log.error("Unable to replace incremental reconciliation specific place holder(s) in configured URL.");
                throw new ConnectorException(this.configuration.getMessage("ex.replaceIncrementalReconPlaceholder", "Unable to replace incremental reconciliation specific place holder(s) in configured URL."));
            }
        }

        log.info("requestUrl:{0}", requestUrlStr);

        try {
            jsonMapString = this.maskPasswordinLog(jsonMapString, oclass.getObjectClassValue());
            return ClientHandler.executeRequest(this.connection.getConnection(), requestUrlStr, (RESTCommonConstants.HTTPOperationType) urlOpTypeList.get(1), jsonMapString, null);
        } catch (Exception var10) {
            log.error("Exception in getJsonResponseString, {0}", var10);
            throw new ConnectorException(this.configuration.getMessage("ex.executeRequest", "Exception occurred while executing request.") + " " + var10.getMessage(), var10);
        }
    }

    private boolean makeConnectorObject(
            ResultsHandler handler,
            OperationOptions options,
            ObjectClass oclass,
            Map<String, Object> jsonResponseMap,
            Set<String> specialAttributeset
    ) throws Exception {
        log.info("Method Entered");
        ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
        new HashSet();
        Map<String, String> namedAttributes = this.namedAttributeMap.get(oclass.getObjectClassValue());
        String organization = null;
        Map<String, Object> attrmap = options.getOptions();
        String uidValue = String.valueOf(GenericRESTUtil.getAttributeValue(namedAttributes.get(Uid.NAME), jsonResponseMap));
        String[] targetIdentifier = null;
        if (options.getAttributesToGet() != null) {
            String[] attrsToGet = options.getAttributesToGet();
            Set<String> parentChildCommonAttr = GenericRESTUtil.getCommonParentChildAttr(Arrays.asList(attrsToGet));
            log.info("uid:{0}", uidValue);
            builder.setObjectClass(oclass);
            builder.setUid(new Uid(uidValue));
            builder.setName((String) jsonResponseMap.get(namedAttributes.get(Name.NAME)));
            Map<String, String> targetObjectIdentifierMap = null;
            if (this.configuration.getTargetObjectIdentifier() != null) {
                targetObjectIdentifierMap = GenericRESTUtil.getSpecialAttributeMap(this.configuration.getTargetObjectIdentifier());
            }

            String[] var15 = attrsToGet;
            int var16 = attrsToGet.length;

            String attr;
            Object specialAttributeDetailsResponseList;
            for (int var17 = 0; var17 < var16; ++var17) {
                attr = var15[var17];
                if (!attr.equals(Name.NAME) && !attr.equals(Uid.NAME)) {
                    if (attr.equalsIgnoreCase("OIM Organization Name")) {
                        organization = (String) attrmap.get("OIM Organization Name");
                        log.info("OIM Organization name is :{0}", organization);
                    } else if (attr.equals("__ENABLE__")) {
                        String status = String.valueOf(jsonResponseMap.get(namedAttributes.get("__ENABLE__")));
                        boolean statusValue = GenericRESTUtil.getStatusValue(status, this.configuration);
                        log.info("Setting status as:{0}", statusValue);
                        builder.addAttribute(AttributeBuilder.buildEnabled(statusValue));
                    } else if (!specialAttributeset.contains(attr) && !parentChildCommonAttr.contains(attr)) {
                        if (targetObjectIdentifierMap != null && targetObjectIdentifierMap.get(oclass.getObjectClassValue() + "." + attr) != null && targetObjectIdentifierMap.get(oclass.getObjectClassValue() + "." + attr).contains(";")) {
                            targetIdentifier = targetObjectIdentifierMap.get(oclass.getObjectClassValue() + "." + attr).split(";");
                        }

                        specialAttributeDetailsResponseList = this.setConnectorObjectValue(attr, attr, jsonResponseMap, targetIdentifier);
                        if (specialAttributeDetailsResponseList != null) {
                            if (specialAttributeDetailsResponseList instanceof List) {
                                builder.addAttribute(AttributeBuilder.build(attr, (List) specialAttributeDetailsResponseList));
                            } else {
                                builder.addAttribute(AttributeBuilder.build(attr, specialAttributeDetailsResponseList));
                            }
                        } else {
                            log.info("Null value retrieved for attribute {0} from the response, adding it to connector object", attr);
                            builder.addAttribute(AttributeBuilder.build(attr, specialAttributeDetailsResponseList));
                        }
                    }
                }
            }

            if (specialAttributeset != null && specialAttributeset.size() > 0) {
                log.info("special attributes present");
                List<String> groupAttributeDetailsResponseList = new ArrayList();
                List<String> childlist = new ArrayList();
                if (this.configuration.getchildFieldsWithSingleEndpoint() != null) {
                    childlist = Arrays.asList(this.configuration.getchildFieldsWithSingleEndpoint());
                }

                Iterator var34 = specialAttributeset.iterator();

                label140:
                while (true) {
                    while (true) {
                        if (!var34.hasNext()) {
                            break label140;
                        }

                        attr = (String) var34.next();
                        specialAttributeDetailsResponseList = new ArrayList();
                        if (groupAttributeDetailsResponseList.size() > 0 && childlist.contains(attr)) {
                            ((List) specialAttributeDetailsResponseList).addAll(groupAttributeDetailsResponseList);
                            break;
                        }

                        if (groupAttributeDetailsResponseList.size() == 0 && childlist.contains(attr)) {
                            groupAttributeDetailsResponseList = this.getSpecialAttributeDetails(oclass, attr, uidValue, options.getRunAsUser());
                            ((List) specialAttributeDetailsResponseList).addAll(groupAttributeDetailsResponseList);
                            break;
                        }

                        try {
                            if (!"__UID__".equals(attr)) {
                                specialAttributeDetailsResponseList = this.getSpecialAttributeDetails(oclass, attr, uidValue, options.getRunAsUser());
                            }
                            break;
                        } catch (Exception var30) {
                            log.error("Getting special attributes: {0} failed, for :{1},{2}", attr, uidValue, var30.getMessage());
                        }
                    }

                    Iterator var39 = ((List) specialAttributeDetailsResponseList).iterator();

                    while (var39.hasNext()) {
                        String specialAttributeResponse = (String) var39.next();
                        log.info("specialAttributeResponse:{0}", specialAttributeResponse);
                        String searchKey = oclass.getObjectClassValue() + "." + attr;
                        GenericRESTUtil.addJsonResourcesTagToConfigParamsMap(this.parserConfigParamsMap, this.objectClasstoParserConfigMap, searchKey);
                        List<Map<String, Object>> specialAttributeResponseList = this.parser.parseResponse(specialAttributeResponse, this.parserConfigParamsMap);
                        if (targetObjectIdentifierMap != null && targetObjectIdentifierMap.get(oclass.getObjectClassValue() + "." + attr) != null && targetObjectIdentifierMap.get(oclass.getObjectClassValue() + "." + attr).contains(";")) {
                            targetIdentifier = targetObjectIdentifierMap.get(oclass.getObjectClassValue() + "." + attr).split(";");
                        }

                        Map<String, String> specialAttributeTargetFormatMap = GenericRESTUtil.getSpecialAttributeMap(this.configuration.getSpecialAttributeTargetFormat());
                        String targetAttr = specialAttributeTargetFormatMap.get(oclass.getObjectClassValue() + "." + attr);
                        targetAttr = targetAttr != null ? targetAttr : attr;
                        List<Object> attributeValueList = new ArrayList();
                        Iterator var27 = specialAttributeResponseList.iterator();

                        while (var27.hasNext()) {
                            Map<String, Object> specialAttributeResponseMap = (Map) var27.next();
                            Object attributeVal = this.setConnectorObjectValue(attr, targetAttr, specialAttributeResponseMap, targetIdentifier);
                            if (parentChildCommonAttr.contains(attr)) {
                                builder = GenericRESTUtil.handleCommonParentChildAttr(builder, attributeVal, attr);
                                parentChildCommonAttr.remove(attr);
                            } else if (attributeVal != null) {
                                if (attributeVal instanceof List) {
                                    attributeValueList.addAll((List) attributeVal);
                                } else {
                                    attributeValueList.add(attributeVal);
                                }
                            }
                        }

                        if (!attributeValueList.isEmpty()) {
                            builder.addAttribute(AttributeBuilder.build(attr, attributeValueList));
                        }
                    }
                }
            }

            String attribute;
            Object attrValObject;
            if (!parentChildCommonAttr.isEmpty()) {
                for (Iterator var32 = parentChildCommonAttr.iterator(); var32.hasNext(); builder = GenericRESTUtil.handleCommonParentChildAttr(builder, attrValObject, attribute)) {
                    attribute = (String) var32.next();
                    String attrVal = attribute.substring(attribute.lastIndexOf(46) + 1);
                    attrValObject = jsonResponseMap.get(attrVal);
                }
            }
        }

        if (organization != null) {
            builder.addAttribute(AttributeBuilder.build("OIM Organization Name", organization));
            log.info("Organization added with value :{0}", organization);
        }

        log.info("Method Exiting");
        return handler.handle(builder.build());
    }

    private Object setConnectorObjectValue(
            String sourceAttribute,
            String targetAttribute,
            Map<String, Object> attributeResponseMap,
            String[] targetIdentifier
    ) {
        Object attrValue = null;
        attrValue = GenericRESTUtil.getAttributeValue(targetAttribute, attributeResponseMap);
        log.info("setting attribute: {0} with value: {1}", targetAttribute, attrValue);
        new HashMap();
        if (attrValue != null) {
            if (!(attrValue instanceof List)) {
                return attrValue;
            } else {
                log.info("list");
                if (((List) attrValue).size() > 0 && ((List) attrValue).get(0) instanceof Map) {
                    List<Object> embeddedObjectList = new ArrayList();
                    log.info("Map");
                    Iterator it = ((List) attrValue).iterator();

                    label45:
                    while (true) {
                        Map attrValueMap;
                        EmbeddedObjectBuilder embObjBldr;
                        do {
                            if (!it.hasNext()) {
                                return embeddedObjectList;
                            }

                            embObjBldr = new EmbeddedObjectBuilder();
                            embObjBldr.setObjectClass(new ObjectClass(sourceAttribute));
                            attrValueMap = (Map) it.next();
                        } while (!GenericRESTUtil.isValidObject(attrValueMap, targetIdentifier));

                        Iterator var10 = attrValueMap.keySet().iterator();

                        while (true) {
                            String key;
                            do {
                                if (!var10.hasNext()) {
                                    embeddedObjectList.add(embObjBldr.build());
                                    continue label45;
                                }

                                key = (String) var10.next();
                            } while (!(attrValueMap.get(key) instanceof String) && !(attrValueMap.get(key) instanceof Boolean) && !(attrValueMap.get(key) instanceof Integer));

                            embObjBldr.addAttribute(AttributeBuilder.build(key, attrValueMap.get(key)));
                        }
                    }
                } else {
                    return attrValue;
                }
            }
        } else {
            return null;
        }
    }

    private List<String> getSpecialAttributeDetails(
            ObjectClass oclass,
            String attr,
            String uid,
            String runAsUser
    ) throws Exception {
        String searchKey = oclass.getObjectClassValue() + "." + attr + "." + CONNECTOR_OPERATION.SEARCHOP;
        log.info("searchKey:{0}", searchKey);
        List<Object> urlOpTypeList = GenericRESTUtil.getURIAndOpType(oclass, CONNECTOR_OPERATION.SEARCHOP, attr, this.relURIsMap, this.opTypeMap, this.configuration);
        List<String> specialAttributeResponseList = this.executeSpecialAttribute(oclass.getObjectClassValue(), attr, uid, null, null, String.valueOf(urlOpTypeList.get(0)), HTTPOperationType.GET, runAsUser);
        return specialAttributeResponseList;
    }

    public void setIncrementalReconParamsMap(Map<String, String> incrementalReconParamsMap) {
        this.incrementalReconParamsMap = incrementalReconParamsMap;
    }

    public void test() {
        log.error("Perf: Test Connection to REST target started.");
        log.info("Querying target to check connectivity\t");
        ObjectClass objectclass = ObjectClass.ACCOUNT;

        try {
            OperationOptions minimalAttrSetOperationOpts = this.getMinimalAttributeSetOperationOptions();
            this.executeQuery(ObjectClass.ACCOUNT, null, new ResultsHandler() {
                public boolean handle(ConnectorObject connectorObject) {
                    return true;
                }
            }, minimalAttrSetOperationOpts);
        } catch (Exception var6) {
            log.error("Error in connecting to REST target", var6.getMessage());
            throw ConnectorException.wrap(var6);
        } finally {
            log.error("Perf: Test Connection to REST target finished");
        }

    }

    private OperationOptions getMinimalAttributeSetOperationOptions() {
        OperationOptionsBuilder oob = new OperationOptionsBuilder();
        List<String> attributesToGet = new ArrayList();
        attributesToGet.add("__NAME__");
        oob.setAttributesToGet(attributesToGet);
        oob.setRunAsUser("AOB_TEST_CONN_USER");
        return oob.build();
    }
}
