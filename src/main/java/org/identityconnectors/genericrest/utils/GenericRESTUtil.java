//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.identityconnectors.genericrest.utils;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.genericrest.GenericRESTConfiguration;
import org.identityconnectors.genericrest.utils.GenericRESTConstants.AuthenticationTypes;
import org.identityconnectors.genericrest.utils.GenericRESTConstants.CONNECTOR_OPERATION;
import org.identityconnectors.restcommon.ClientHandler;
import org.identityconnectors.restcommon.utils.RESTCommonConstants;
import org.identityconnectors.restcommon.utils.RESTCommonConstants.HTTPOperationType;
import org.identityconnectors.restcommon.utils.RESTCommonUtils;

import java.lang.reflect.Field;
import java.util.*;

public class GenericRESTUtil {
    private static final Log log = Log.getLog(GenericRESTUtil.class);

    public GenericRESTUtil() {
    }

    public static Map<String, List<String>> getSimpleMultivaluedDetails(GenericRESTConfiguration config) {
        log.ok("Method Entered");
        log.info("getting SimpleMultivaluedDetails");
        Map<String, List<String>> simpleMultivaluedDetails = new HashMap();
        if (config.getSimpleMultivaluedAttributes() == null) {
            return null;
        } else {
            String[] simpleMultivalued = config.getSimpleMultivaluedAttributes();
            String[] var3 = simpleMultivalued;
            int var4 = simpleMultivalued.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                String simpleMulti = var3[var5];
                String[] keys = simpleMulti.split("=");
                List<String> simpleMultiList = Arrays.asList(keys[1].split(";"));
                List<String> list = null;
                if (simpleMultivaluedDetails.containsKey(keys[0])) {
                    list = simpleMultivaluedDetails.get(keys[0]);
                    list.addAll(simpleMultiList);
                    simpleMultivaluedDetails.put(keys[0], list);
                } else {
                    list = new ArrayList();
                    list.addAll(simpleMultiList);
                    simpleMultivaluedDetails.put(keys[0], list);
                }
            }

            log.ok("Method Exiting");
            return simpleMultivaluedDetails;
        }
    }

    public static String removeQueryPlaceholderFrmUrl(
            String url,
            String placeHolder
    ) {
        String[] splitUrls = url.split("\\?");
        StringBuilder updatedUrl = new StringBuilder(splitUrls[0]);
        if (splitUrls.length > 1 && splitUrls[1] != null) {
            String[] queries = splitUrls[1].split("&");
            boolean firstFilter = true;
            String[] var6 = queries;
            int var7 = queries.length;

            for (int var8 = 0; var8 < var7; ++var8) {
                String query = var6[var8];
                if (!query.contains("=") || !query.split("\\=")[1].contentEquals(placeHolder)) {
                    if (firstFilter) {
                        updatedUrl.append("?").append(query);
                        firstFilter = false;
                    } else {
                        updatedUrl.append("&").append(query);
                    }
                }
            }
        }

        return updatedUrl.toString();
    }

    public static String getQueryPlaceholderFrmUrl(
            String url,
            String placeHolder
    ) {
        String[] splitUrls = url.split("\\?");
        String searchString = null;
        if (splitUrls.length > 1 && splitUrls[1] != null) {
            String[] queries = splitUrls[1].split("&");
            String[] var5 = queries;
            int var6 = queries.length;

            for (int var7 = 0; var7 < var6; ++var7) {
                String query = var5[var7];
                if (query.contains("=") && query.split("\\=")[1].contentEquals(placeHolder)) {
                    searchString = query;
                    break;
                }
            }
        }

        return searchString;
    }

    public static Map<String, Object> formAuthConfigParamsMap(GenericRESTConfiguration config) {
        log.ok("Method Entered");
        Map<String, Object> authConfigParamsMap = new HashMap();
        Field[] fields = config.getClass().getDeclaredFields();

        try {
            Field[] var3 = fields;
            int var4 = fields.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                Field field = var3[var5];
                String fieldName = field.getName();
                if (GenericRESTConstants.AUTH_CONFIG_PARAMS.contains(fieldName)) {
                    field.setAccessible(true);
                    Object value = field.get(config);
                    if (value instanceof GuardedString) {
                        value = decryptPassword((GuardedString) value);
                    }

                    if (value != null && !String.valueOf(value).equals("0")) {
                        authConfigParamsMap.put(fieldName, String.valueOf(value));
                    }
                }
            }
        } catch (IllegalArgumentException var9) {
            throw new ConnectorIOException(var9);
        } catch (IllegalAccessException var10) {
            throw new ConnectorIOException(var10);
        }

        if (config.getCustomAuthHeaders() != null && config.getCustomAuthHeaders().length > 0) {
            authConfigParamsMap.put("customAuthHeaders", convertConfigArrayToMap(config.getCustomAuthHeaders()));
        }

        if (config.getCustomAuthConfigParams() != null && config.getCustomAuthConfigParams().length > 0) {
            authConfigParamsMap.putAll(convertConfigArrayToMap(config.getCustomAuthConfigParams()));
        }

        log.ok("Method Exiting");
        return authConfigParamsMap;
    }

    public static Map<String, String> convertConfigArrayToMap(String[] configParams) {
        log.ok("Method Entered");
        Map<String, String> configParamsMap = new HashMap();
        if (configParams == null) {
            return null;
        } else {
            String[] var2 = configParams;
            int var3 = configParams.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                String configParam = var2[var4];
                if (configParam.contains("=")) {
                    configParamsMap.put(configParam.substring(0, configParam.indexOf("=")).trim(), configParam.substring(configParam.indexOf("=") + 1).trim());
                }
            }

            log.ok("Method Exiting");
            return configParamsMap;
        }
    }

    public static RESTCommonConstants.HTTPOperationType getDefaultHTTPOp(GenericRESTConstants.CONNECTOR_OPERATION operation) {
        RESTCommonConstants.HTTPOperationType defaultOp = operation.getDefaultHTTPOperation();
        log.info("default http operation type is:{0},for operation:{1}", defaultOp, operation);
        return defaultOp;
    }

    public static Map<String, String> getRelURIsMap(GenericRESTConfiguration configuration) {
        Map<String, String> relURIsOpTypeMap = new HashMap();
        String[] individualURIs = configuration.getRelURIs();
        String[] uriPlaceHolders = configuration.getUriPlaceHolder();
        Map<String, String> uriPlaceHolderseMap = new HashMap();
        String[] var5;
        int var6;
        int var7;
        String entry;
        String[] keys;
        if (uriPlaceHolders != null) {
            var5 = uriPlaceHolders;
            var6 = uriPlaceHolders.length;

            for (var7 = 0; var7 < var6; ++var7) {
                entry = var5[var7];
                keys = entry.split(";");
                uriPlaceHolderseMap.put(keys[0], keys[1]);
            }
        }

        var5 = individualURIs;
        var6 = individualURIs.length;

        for (var7 = 0; var7 < var6; ++var7) {
            entry = var5[var7];
            keys = entry.split("=", 2);
            String relUri = keys[1];
            List<String> attrListInPayload = new ArrayList();
            if (relUri.indexOf("$(") != -1) {
                Map<Integer, Integer> indexMap = RESTCommonUtils.searchPlaceHolders(relUri);
                Iterator var13 = indexMap.keySet().iterator();

                while (var13.hasNext()) {
                    Integer startIndx = (Integer) var13.next();
                    attrListInPayload.add(relUri.substring(startIndx + "$(".length(), indexMap.get(startIndx)));
                }
            }

            if (uriPlaceHolders != null) {
                Iterator var15 = attrListInPayload.iterator();

                while (var15.hasNext()) {
                    String key = (String) var15.next();
                    if (uriPlaceHolderseMap.containsKey(key)) {
                        relUri = relUri.replace("$(" + key + ")$", uriPlaceHolderseMap.get(key));
                    }
                }
            }

            relURIsOpTypeMap.put(keys[0], relUri);
        }

        return relURIsOpTypeMap;
    }

    public static List<Object> getURIAndOpType(
            ObjectClass oclass,
            GenericRESTConstants.CONNECTOR_OPERATION operation,
            String specialAttrName,
            Map<String, String> relURIMap,
            Map<String, RESTCommonConstants.HTTPOperationType> opTypeMap,
            GenericRESTConfiguration configuration
    ) {
        List<Object> listUrlOpType = new ArrayList();
        String baseSearchKey = null;
        boolean checkForUpdateOp = false;
        boolean checkForDeleteOp = false;
        if (operation.toString().equalsIgnoreCase(CONNECTOR_OPERATION.ADDATTRIBUTE.toString())) {
            checkForUpdateOp = true;
        } else if (operation.toString().equalsIgnoreCase(CONNECTOR_OPERATION.REMOVEATTRIBUTE.toString())) {
            checkForDeleteOp = true;
        }

        if (specialAttrName == null) {
            baseSearchKey = oclass.getObjectClassValue();
        } else {
            baseSearchKey = getSearchKey(oclass.getObjectClassValue(), specialAttrName);
        }

        log.info("getting UrlOpTypeList for:{0}", baseSearchKey);
        String relURI = relURIMap.get(baseSearchKey + "." + operation);
        if (StringUtil.isBlank(relURI) && checkForUpdateOp) {
            relURI = relURIMap.get(baseSearchKey + "." + CONNECTOR_OPERATION.UPDATEOP);
        }

        if (StringUtil.isBlank(relURI) && checkForDeleteOp) {
            relURI = relURIMap.get(baseSearchKey + "." + CONNECTOR_OPERATION.DELETEOP);
        }

        if (StringUtil.isBlank(relURI)) {
            relURI = relURIMap.get(baseSearchKey);
        }

        if (StringUtil.isBlank(relURI)) {
            log.error("Unable to find {0} end point URI for the given object class {1}.", operation, oclass.getObjectClassValue());
            throw new ConnectorException(configuration.getMessage("ex.nullRequestURI", "Unable to find {0} end point URI for the given object class " + oclass.getObjectClassValue() + ".", operation, oclass.getObjectClassValue()));
        } else {
            RESTCommonConstants.HTTPOperationType httpOpType = opTypeMap.get(baseSearchKey + "." + operation);
            if (httpOpType == null && checkForUpdateOp) {
                httpOpType = opTypeMap.get(baseSearchKey + "." + CONNECTOR_OPERATION.UPDATEOP);
            }

            if (httpOpType == null && checkForDeleteOp) {
                httpOpType = opTypeMap.get(baseSearchKey + "." + CONNECTOR_OPERATION.DELETEOP);
            }

            if (httpOpType == null) {
                httpOpType = getDefaultHTTPOp(operation);
            }

            listUrlOpType.add(relURI);
            listUrlOpType.add(httpOpType);
            log.info("UrlOpTypeList:{0}", listUrlOpType);
            return listUrlOpType;
        }
    }

    public static Map<String, RESTCommonConstants.HTTPOperationType> getOpTypeMap(GenericRESTConfiguration configuration) {
        log.info("getting OpType Map");
        Map<String, RESTCommonConstants.HTTPOperationType> opTypeMap = new HashMap();
        String[] opTypes = configuration.getOpTypes();
        log.info("opTypes = " + Arrays.toString(opTypes));
        if (opTypes != null && opTypes.length != 0) {
            String[] var4 = opTypes;
            int var5 = opTypes.length;

            for (int var6 = 0; var6 < var5; ++var6) {
                String entry = var4[var6];
                String[] keys = entry.split("=");
                opTypeMap.put(keys[0], getHTTPOperation(keys[1], configuration));
            }
        }

        return opTypeMap;
    }

    public static Map<String, Map<String, String>> getNamedAttributeMap(GenericRESTConfiguration config) {
        log.info("getting Named Attribute Map");
        Map<String, Map<String, String>> namedAttrs = new HashMap();
        String[] statusAttrArray;
        String[] var3;
        int var4;
        int var5;
        String statusAttr;
        String[] keys;
        if (config.getNameAttributes() != null && config.getNameAttributes().length > 0) {
            statusAttrArray = config.getNameAttributes();
            var3 = statusAttrArray;
            var4 = statusAttrArray.length;

            for (var5 = 0; var5 < var4; ++var5) {
                statusAttr = var3[var5];
                keys = statusAttr.split("[.]");
                if (keys[0].equals(GenericRESTConstants.USERS)) {
                    keys[0] = ObjectClass.ACCOUNT_NAME;
                } else if (keys[0].equals(GenericRESTConstants.GROUPS)) {
                    keys[0] = ObjectClass.GROUP_NAME;
                }

                Map<String, String> name = new HashMap();
                name.put(Name.NAME, keys[1]);
                namedAttrs.put(keys[0], name);
            }
        }

        if (config.getPasswordAttribute() != null) {
            String password = config.getPasswordAttribute();
            Map<String, String> passwordMap;
            if (namedAttrs.containsKey(ObjectClass.ACCOUNT_NAME)) {
                passwordMap = namedAttrs.get(ObjectClass.ACCOUNT_NAME);
            } else {
                passwordMap = new HashMap();
                namedAttrs.put(ObjectClass.ACCOUNT_NAME, passwordMap);
            }

            ((Map) passwordMap).put(OperationalAttributes.PASSWORD_NAME, password);
        }

        Map<String, String> status;
        if (config.getUidAttributes() != null && config.getUidAttributes().length > 0) {
            statusAttrArray = config.getUidAttributes();
            var3 = statusAttrArray;
            var4 = statusAttrArray.length;

            for (var5 = 0; var5 < var4; ++var5) {
                statusAttr = var3[var5];
                keys = statusAttr.split("[.]");
                if (keys[0].equals(GenericRESTConstants.USERS)) {
                    keys[0] = ObjectClass.ACCOUNT_NAME;
                } else if (keys[0].equals(GenericRESTConstants.GROUPS)) {
                    keys[0] = ObjectClass.GROUP_NAME;
                }

                if (namedAttrs.containsKey(keys[0])) {
                    status = namedAttrs.get(keys[0]);
                } else {
                    status = new HashMap();
                }

                ((Map) status).put(Uid.NAME, keys[1]);
                namedAttrs.put(keys[0], status);
            }
        }

        if (config.getStatusAttributes() != null && config.getStatusAttributes().length > 0) {
            statusAttrArray = config.getStatusAttributes();
            var3 = statusAttrArray;
            var4 = statusAttrArray.length;

            for (var5 = 0; var5 < var4; ++var5) {
                statusAttr = var3[var5];
                keys = statusAttr.split("[.]");
                if (keys[0].equals(GenericRESTConstants.USERS)) {
                    keys[0] = ObjectClass.ACCOUNT_NAME;
                } else if (keys[0].equals(GenericRESTConstants.GROUPS)) {
                    keys[0] = ObjectClass.GROUP_NAME;
                }

                new HashMap();
                if (namedAttrs.containsKey(keys[0])) {
                    status = namedAttrs.get(keys[0]);
                } else {
                    status = new HashMap();
                }

                ((Map) status).put("__ENABLE__", keys[1]);
                namedAttrs.put(keys[0], status);
            }
        }

        return namedAttrs;
    }

    public static Object getAttributeValue(
            String attr,
            Map<String, Object> jsonMap
    ) {
        log.info("BEGIN");
        log.info("getting attribute value for:{0}", attr);
        Object value = null;
        String parentAttribute = null;
        String childAttribute = null;
        if (isJsonObject(attr)) {
            log.info("complex attribute :{0}", attr);
            String[] attrName = attr.split("\\.");

            for (int i = 1; i < attrName.length; ++i) {
                parentAttribute = attrName[i - 1];
                childAttribute = attrName[i];
                if (isJsonObjectComplexSingleValued(jsonMap, parentAttribute)) {
                    jsonMap = (Map) jsonMap.get(parentAttribute);
                    value = parseJsonObject(childAttribute, jsonMap);
                } else if (isJsonObjectComplexMultiValued(jsonMap, parentAttribute)) {
                    value = parseJsonArray(parentAttribute, childAttribute, (List) jsonMap.get(parentAttribute));
                }
            }
        } else if (jsonMap.get(attr) instanceof String) {
            value = parseString(attr, jsonMap);
        } else if (jsonMap.get(attr) instanceof Boolean) {
            value = parseBoolean(attr, jsonMap);
        } else if (jsonMap.get(attr) instanceof Integer) {
            value = parseInteger(attr, jsonMap);
        } else if (jsonMap.get(attr) instanceof Long) {
            value = parseLong(attr, jsonMap);
        } else if (jsonMap.get(attr) instanceof Double) {
            value = parseDouble(attr, jsonMap);
        } else if (jsonMap.get(attr) instanceof List) {
            log.info("simple multivalued attribute:{0}", attr);
            log.info("simple multivalued map:{0}", jsonMap);
            value = parseList(attr, jsonMap);
        } else if (jsonMap.get(attr) instanceof Map) {
            value = parseMap(attr, jsonMap);
        }

        log.info("value of attribute: {0} is :{1}", attr, value);
        log.info("END");
        return value;
    }

    public static Map<String, String> formParserConfigParamsMap(GenericRESTConfiguration config) {
        log.ok("Method Entered");
        Map<String, String> parserConfigParamsMap = new HashMap();
        if (config.getCustomParserConfigParams() != null && config.getCustomParserConfigParams().length > 0) {
            parserConfigParamsMap.putAll(convertConfigArrayToMap(config.getCustomParserConfigParams()));
        }

        log.ok("Method Exiting");
        return parserConfigParamsMap;
    }

    public static Map<String, String> formObjectClasstoParserConfigMap(GenericRESTConfiguration config) {
        log.ok("Method Entered");
        Map<String, String> objectClasstoParserConfigMap = new HashMap();
        if (config.getJsonResourcesTag() != null && config.getJsonResourcesTag().length > 0) {
            String[] resourceTagWithObjectClassArray = config.getJsonResourcesTag();
            String[] var3 = resourceTagWithObjectClassArray;
            int var4 = resourceTagWithObjectClassArray.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                String resourceTagWithObjectClass = var3[var5];
                String[] resourceTagArray = resourceTagWithObjectClass.split("=", 2);
                String jsonConfigStr = objectClasstoParserConfigMap.get(resourceTagArray[0]);
                StringBuilder jsonConfig = new StringBuilder();
                jsonConfig.append(jsonConfigStr != null ? jsonConfigStr : "");
                if (jsonConfig.length() != 0) {
                    jsonConfig.append(";").append(resourceTagArray[1]);
                } else {
                    jsonConfig.append(resourceTagArray[1]);
                }

                objectClasstoParserConfigMap.put(resourceTagArray[0], jsonConfig.toString());
            }
        }

        log.ok("Method Exiting");
        return objectClasstoParserConfigMap;
    }

    public static void addJsonResourcesTagToConfigParamsMap(
            Map<String, String> parserConfigParamsMap,
            Map<String, String> objectClasstoParserConfigMap,
            String objectClass
    ) {
        log.ok("Method Entered");
        String responseListTagValue = objectClasstoParserConfigMap.get(objectClass);
        if (responseListTagValue != null) {
            parserConfigParamsMap.put("ResponsesListTag", responseListTagValue);
        } else {
            parserConfigParamsMap.remove("ResponsesListTag");
        }

        log.ok("Method Exiting");
    }

    public static void addPageAttrToConfigParamsMap(
            Map<String, String> parserConfigParamsMap,
            String pageattribute
    ) {
        log.ok("Method Entered");
        if (pageattribute != null && !pageattribute.equalsIgnoreCase("")) {
            parserConfigParamsMap.put("PageAttrTag", pageattribute);
        }

        log.ok("Method Exiting");
    }

    private static boolean isJsonObject(String attr) {
        log.info("BEGIN");
        return attr.contains(".");
    }

    private static boolean isJsonObjectComplexMultiValued(
            Map<String, Object> jsonMap,
            String parentAttribute
    ) {
        log.info("BEGIN");
        return jsonMap.get(parentAttribute) instanceof List;
    }

    private static boolean isJsonObjectComplexSingleValued(
            Map<String, Object> jsonMap,
            String parentAttribute
    ) {
        log.info("BEGIN");
        return jsonMap.get(parentAttribute) instanceof Map;
    }

    private static String parseString(
            String attributeName,
            Map<String, Object> jsonMap
    ) {
        log.info("BEGIN: getting string value");
        log.info("END : getting string value");
        return (String) jsonMap.get(attributeName);
    }

    private static Boolean parseBoolean(
            String attributeName,
            Map<String, Object> jsonMap
    ) {
        log.info("BEGIN:  getting boolean value");
        log.info("END : getting boolean value");
        return (Boolean) jsonMap.get(attributeName);
    }

    private static List<Object> parseList(
            String attributeName,
            Map<String, Object> jsonMap
    ) {
        log.info("BEGIN:  getting list value");
        log.info("END : getting list value");
        return (List) jsonMap.get(attributeName);
    }

    private static List<Object> parseMap(
            String attributeName,
            Map<String, Object> jsonMap
    ) {
        log.info("BEGIN:  getting map value");
        log.info("END : getting map value");
        Object obj = jsonMap.get(attributeName);
        List<Object> list = new ArrayList();
        list.add(obj);
        return list;
    }

    private static Object parseJsonArray(
            String parentAttribute,
            String attributeName,
            List<Object> jsonArray
    ) {
        log.info("BEGIN:jsonArray");
        if (jsonArray.size() > 0 && jsonArray.get(0) instanceof Map) {
            log.info("size of jsonArray is : {0}", jsonArray.size());
            List<Object> ObjectList = new ArrayList();
            Iterator jsonIterator = jsonArray.iterator();

            while (jsonIterator.hasNext()) {
                Map<String, Object> jsonMap = (Map) jsonIterator.next();
                log.info("json map :{0}", jsonMap);
                if (jsonMap.get(attributeName) instanceof String) {
                    ObjectList.add(parseString(attributeName, jsonMap));
                } else if (jsonMap.get(attributeName) instanceof Boolean) {
                    ObjectList.add(parseBoolean(attributeName, jsonMap));
                } else if (jsonMap.get(attributeName) instanceof Map) {
                    ObjectList.add(parseJsonObject(attributeName, (Map) jsonMap.get(parentAttribute)));
                } else if (jsonMap.get(attributeName) instanceof List) {
                    ObjectList.add(parseJsonArray(parentAttribute, attributeName, (List) jsonMap.get(parentAttribute)));
                } else if (jsonMap.get(attributeName) instanceof Integer) {
                    ObjectList.add(parseInteger(attributeName, jsonMap));
                } else if (jsonMap.get(attributeName) instanceof Double) {
                    ObjectList.add(parseDouble(attributeName, jsonMap));
                }
            }

            return ObjectList;
        } else if (jsonArray.size() > 0 && jsonArray.get(0) instanceof String) {
            log.info("size of jsonArray is : {0}", jsonArray.size());
            return jsonArray;
        } else {
            return null;
        }
    }

    private static Double parseDouble(
            String attributeName,
            Map<String, Object> jsonMap
    ) {
        log.info("BEGIN: getting double value");
        log.info("END : getting double value");
        return (Double) jsonMap.get(attributeName);
    }

    private static Integer parseInteger(
            String attributeName,
            Map<String, Object> jsonMap
    ) {
        log.info("BEGIN: getting integer value");
        log.info("END : getting integer value");
        return (Integer) jsonMap.get(attributeName);
    }

    private static Long parseLong(
            String attributeName,
            Map<String, Object> jsonMap
    ) {
        log.info("BEGIN: getting long value");
        log.info("END : getting long value");
        return (Long) jsonMap.get(attributeName);
    }

    private static Object parseJsonObject(
            String attributeName,
            Map<String, Object> jsonMap
    ) {
        log.info("BEGIN:jsonobject");
        if (jsonMap.get(attributeName) instanceof Map) {
            return jsonMap.get(attributeName);
        } else if (jsonMap.get(attributeName) instanceof List) {
            return jsonMap.get(attributeName);
        } else if (jsonMap.get(attributeName) instanceof String) {
            return parseString(attributeName, jsonMap);
        } else if (jsonMap.get(attributeName) instanceof Boolean) {
            return parseBoolean(attributeName, jsonMap);
        } else if (jsonMap.get(attributeName) instanceof Integer) {
            return parseInteger(attributeName, jsonMap);
        } else {
            return jsonMap.get(attributeName) instanceof Double ? parseDouble(attributeName, jsonMap) : null;
        }
    }

    public static Map<String, String> getSpecialAttributeMap(String[] configString) {
        log.info("BEGIN: getSpecialAttributeMap for String[]");
        Map<String, String> specialAttributeMap = new HashMap();
        String[] var2 = configString;
        int var3 = configString.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            String str = var2[var4];
            String[] key = str.split("=");
            specialAttributeMap.put(key[0], key[1]);
        }

        log.info("END: getSpecialAttributeMap for String[]");
        return specialAttributeMap;
    }

    public static void separateSpecialAttributeSet(
            ObjectClass oclass,
            Set<Attribute> attrSet,
            Map<String, String> relURIsMap,
            GenericRESTConstants.CONNECTOR_OPERATION operation,
            Set<Attribute> specialAttributeset,
            Set<Attribute> normalAttributeSet
    ) {
        log.info("BEGIN: getSpecialAttributeSet");
        String key = null;
        String baseSearchKey = null;
        String updateOp = null;
        String deleteOp = null;
        if (operation.toString().equalsIgnoreCase(CONNECTOR_OPERATION.ADDATTRIBUTE.toString())) {
            updateOp = CONNECTOR_OPERATION.UPDATEOP.toString();
        } else if (operation.toString().equalsIgnoreCase(CONNECTOR_OPERATION.REMOVEATTRIBUTE.toString())) {
            deleteOp = CONNECTOR_OPERATION.DELETEOP.toString();
        }

        Iterator var10 = attrSet.iterator();

        while (var10.hasNext()) {
            Attribute attr = (Attribute) var10.next();
            key = attr.getName();
            baseSearchKey = getSearchKey(oclass.getObjectClassValue(), key);
            boolean isSpecialAttribute = false;
            isSpecialAttribute = relURIsMap.containsKey(baseSearchKey) || relURIsMap.containsKey(getSearchKey(baseSearchKey, operation.toString())) || updateOp != null && relURIsMap.containsKey(getSearchKey(baseSearchKey, updateOp)) || deleteOp != null && relURIsMap.containsKey(getSearchKey(baseSearchKey, deleteOp));
            if (isSpecialAttribute) {
                specialAttributeset.add(attr);
            } else {
                normalAttributeSet.add(attr);
            }
        }

        log.info("END: getSpecialAttributeSet");
    }

    public static String getValueToBeRemoved(
            Object attrValue,
            Map<String, Map<String, String>> namedAttributeMap
    ) {
        if (attrValue != null && attrValue instanceof EmbeddedObject) {
            String embUidAttrName = (String) ((Map) namedAttributeMap.get(((EmbeddedObject) attrValue).getObjectClass().getObjectClassValue())).get("__UID__");
            String uidAttrValue = null;
            Attribute attribute = ((EmbeddedObject) attrValue).getAttributeByName(embUidAttrName);
            if (attribute != null) {
                uidAttrValue = attribute.getValue().get(0).toString();
            }

            return uidAttrValue;
        } else {
            return (String) attrValue;
        }
    }

    public static Set<String> getSpecialAttributeSet(
            ObjectClass oclass,
            String[] attributesToGet,
            Map<String, String> relURIsMap,
            GenericRESTConstants.CONNECTOR_OPERATION operation
    ) {
        log.info("BEGIN: getSpecialAttributeSet");
        Set<String> specialAttributeset = new HashSet();
        String searchKey = null;
        String[] var6 = attributesToGet;
        int var7 = attributesToGet.length;

        for (int var8 = 0; var8 < var7; ++var8) {
            String attr = var6[var8];
            searchKey = getSearchKey(oclass.getObjectClassValue(), attr, operation.toString());
            if (relURIsMap != null && (relURIsMap.containsKey(searchKey) || relURIsMap.containsKey(getSearchKey(oclass.getObjectClassValue(), attr)))) {
                specialAttributeset.add(attr);
            }
        }

        log.info("END: getSpecialAttributeSet");
        return specialAttributeset;
    }

    public static String decryptPassword(GuardedString password) {
        log.info("BEGIN: decryptPassword");
        GuardedStringAccessor accessor = new GuardedStringAccessor();
        password.access(accessor);
        String passwordString = accessor.getString();
        accessor.clear();
        log.info("END: decryptPassword");
        return passwordString;
    }

    public static String getSearchKey(String... obj) {
        if (obj == null) {
            return null;
        } else {
            StringBuilder searchKey = new StringBuilder();
            String[] var2 = obj;
            int var3 = obj.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                String str = var2[var4];
                searchKey.append(str).append(".");
            }

            searchKey.setLength(searchKey.length() - 1);
            return searchKey.toString();
        }
    }

    public static boolean isUidPlaceHolderPresent(String url) {
        return url.contains("$(__UID__)$") && url.contains("$(") && url.contains(")$");
    }

    public static boolean isAnyIncrementalReconPlaceHolderPresent(String url) {
        return url.contains("$(Incremental Recon Attribute)$") || url.contains("$(Latest Token)$");
    }

    public static boolean isPlaceHolderPresent(String url) {
        return url.contains("$(") && url.contains(")$");
    }

    public static boolean isNonUidPlaceHolderPresent(String url) {
        log.info("Begin : isNonUidPlaceHolderPresent check method");
        url = url.replace("$(__UID__)$", "");
        return url.contains("$(") && url.contains(")$");
    }

    public static String getAttributeIdFromURL(
            String url,
            Object attributeValue,
            Set<Attribute> attributeSet
    ) {
        log.info("Begin : getAttributeIdFromURL method");
        String attrId = null;
        if (attributeValue instanceof EmbeddedObject) {
            String objectClassWithAttr = url.substring(url.indexOf("$(") + "$(".length(), url.indexOf(")$"));
            String[] sliptArray = objectClassWithAttr.split("\\.");
            String objectClass = sliptArray[0];
            String attributeName = sliptArray[1];
            Iterator var8 = ((EmbeddedObject) attributeValue).getAttributes().iterator();

            while (true) {
                while (var8.hasNext()) {
                    Attribute attrFromEmbObject = (Attribute) var8.next();
                    if (((EmbeddedObject) attributeValue).getObjectClass().getObjectClassValue().equals(objectClass) && attrFromEmbObject.getName().equals(attributeName)) {
                        attrId = (String) attrFromEmbObject.getValue().get(0);
                    } else {
                        attributeSet.add(attrFromEmbObject);
                    }
                }

                return attrId;
            }
        } else {
            attrId = (String) attributeValue;
            return attrId;
        }
    }

    public static GenericRESTConstants.AuthenticationTypes getAuthType(
            String authType,
            GenericRESTConfiguration config
    ) {
        GenericRESTConstants.AuthenticationTypes[] authenticationTypes = AuthenticationTypes.values();
        GenericRESTConstants.AuthenticationTypes[] var3 = authenticationTypes;
        int var4 = authenticationTypes.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            GenericRESTConstants.AuthenticationTypes authenticationType = var3[var5];
            if (authenticationType.getAuthName().equalsIgnoreCase(authType)) {
                return authenticationType;
            }
        }

        log.error("Unsupported authentication type: {0}", authType);
        throw new ConnectorException(config.getMessage("ex.unsupportedAuthenticationType", "Unsupported authentication type: " + authType + ", Supported authentication types are " + "basic, oauth_jwt, oauth_client_credentials, oauth_resource_owner_password", authType));
    }

    public static RESTCommonConstants.HTTPOperationType getHTTPOperation(
            String operationTypeString,
            GenericRESTConfiguration configuration
    ) {
        RESTCommonConstants.HTTPOperationType[] supportedHTTPOperations = HTTPOperationType.values();
        log.info("operationTypeString: {0}", operationTypeString);
        RESTCommonConstants.HTTPOperationType[] var3 = supportedHTTPOperations;
        int var4 = supportedHTTPOperations.length;

        for (int var5 = 0; var5 < var4; ++var5) {
            RESTCommonConstants.HTTPOperationType supportedHTTPOperation = var3[var5];
            if (supportedHTTPOperation.getValue().equalsIgnoreCase(operationTypeString)) {
                return supportedHTTPOperation;
            }
        }

        log.error("Unsupported HTTP Operation: {0}", operationTypeString);
        throw new ConnectorException(configuration.getMessage("ex.unsupportedHTTPOperationType", "Unsupported HTTP operation type: " + operationTypeString + ", Supported HTTP operation types are " + "POST, PUT, PATCH, DELETE, GET", operationTypeString));
    }

    public static boolean getStatusValue(
            String status,
            GenericRESTConfiguration config
    ) {
        if (!StringUtil.isBlank(config.getStatusEnableValue()) && !StringUtil.isBlank(config.getStatusDisableValue())) {
            if (config.getStatusEnableValue().equalsIgnoreCase(status)) {
                return true;
            } else if (config.getStatusDisableValue().equalsIgnoreCase(status)) {
                return false;
            } else {
                throw new ConnectorException(config.getMessage("ex.invalidStatus", "Status " + status + " is not configured as one of the target supported status value", status));
            }
        } else {
            return status.equalsIgnoreCase("true") || status.equalsIgnoreCase("yes") || status.equalsIgnoreCase("1");
        }
    }

    public static boolean isValidObject(
            Map<String, Object> attrValueMap,
            String[] targetIdentifier
    ) {
        log.info("BEGIN: isValidObject");
        if (targetIdentifier == null) {
            return true;
        } else {
            return attrValueMap.containsKey(targetIdentifier[0]) && attrValueMap.get(targetIdentifier[0]).toString().equals(targetIdentifier[1]);
        }
    }

    public static Map<String, String> getCustomPayloadMap(GenericRESTConfiguration configuration) {
        log.info("getting Payload Format Map");
        Map<String, String> payloadFormatMap = new HashMap();
        if (configuration.getCustomPayload() != null && configuration.getCustomPayload().length != 0) {
            String[] individualPayload = configuration.getCustomPayload();
            String[] var3 = individualPayload;
            int var4 = individualPayload.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                String entry = var3[var5];
                String[] keys = entry.split("=", 2);
                payloadFormatMap.put(keys[0], keys[1]);
            }

            return payloadFormatMap;
        } else {
            return payloadFormatMap;
        }
    }

    public static String getMembershipSearchUrl(
            String oclass,
            String uid,
            String attrName,
            String attrId,
            Map<String, String> relURIsMap,
            GenericRESTConfiguration configuration
    ) {
        StringBuilder requestUrl = (new StringBuilder(configuration.isSslEnabled() ? "https://" : "http://")).append(configuration.getHost());
        if (configuration.getPort() > 0) {
            requestUrl.append(":").append(configuration.getPort());
        }

        requestUrl.append(relURIsMap.get(getSearchKey(oclass, "__MEMBERSHIP__", attrName, CONNECTOR_OPERATION.SEARCHOP.toString())));
        String requestUrlStr = requestUrl.toString();
        if (isUidPlaceHolderPresent(requestUrlStr)) {
            requestUrlStr = requestUrlStr.replace("$(__UID__)$", uid);
        }

        if (isPlaceHolderPresent(requestUrlStr)) {
            String objectClassWithAttrPlaceHolder = requestUrlStr.substring(requestUrlStr.indexOf("$("), requestUrlStr.indexOf(")$") + ")$".length());
            requestUrlStr = requestUrlStr.replace(objectClassWithAttrPlaceHolder, attrId);
        }

        return requestUrlStr;
    }

    public static Map<String, Map<String, Object>> getConditionMapForMembershipSearch(
            String oclass,
            String attrName,
            String attrId,
            String objectClassWithAttr,
            GenericRESTConfiguration configuration
    ) {
        Map<String, Map<String, Object>> conditionMap = new HashMap();
        Map<String, Object> conditionValue = new HashMap();
        Map<String, String> specialAttributeTargetFormatMap = getSpecialAttributeMap(configuration.getSpecialAttributeTargetFormat());
        String targetAttr = specialAttributeTargetFormatMap.get(getSearchKey(oclass, "__MEMBERSHIP__", attrName));
        if (targetAttr == null) {
            targetAttr = specialAttributeTargetFormatMap.get(getSearchKey(oclass, attrName));
        }

        targetAttr = targetAttr != null ? targetAttr : attrName;
        conditionValue.put(targetAttr, attrId);
        conditionMap.put(objectClassWithAttr, conditionValue);
        return conditionMap;
    }

    public static Set<Attribute> handleCurrentAttributes(
            Set<Attribute> attrSet,
            Set<Attribute> currentAttrsSet
    ) {
        log.info("Begin : Retrieving CURRENT_ATTRIBUTES");
        attrSet = new HashSet(attrSet);
        Iterator var2 = currentAttrsSet.iterator();

        while (var2.hasNext()) {
            Attribute currentattr = (Attribute) var2.next();
            Attribute attr = AttributeUtil.find(currentattr.getName(), attrSet);
            if (attr == null && !OperationalAttributes.PASSWORD_NAME.equals(currentattr.getName())) {
                attrSet.add(currentattr);
            }
        }

        attrSet.remove(OperationalAttributes.CURRENT_ATTRIBUTES);
        log.info("End : Retrieving CURERNT_ATTRIBUTES");
        return attrSet;
    }

    public static Set<Attribute> handleBlankValue(Set<Attribute> attrSet) {
        log.info("Begin : handle blank Value");
        Set<Attribute> updAttrSet = new HashSet();
        Iterator var2 = attrSet.iterator();

        while (var2.hasNext()) {
            Attribute attr = (Attribute) var2.next();
            if (StringUtil.isBlank(AttributeUtil.getAsStringValue(attr))) {
                Attribute updatedAttr = AttributeBuilder.build(attr.getName(), "");
                updAttrSet.add(updatedAttr);
            } else {
                updAttrSet.add(attr);
            }
        }

        log.info("End : handle blank Value");
        return updAttrSet;
    }

    public static Set<String> getCommonParentChildAttr(List<String> attribute) {
        log.info("Begin : identify common attr in parent & child ");
        Set<String> commonAttr = new HashSet();
        Iterator var2 = attribute.iterator();

        while (true) {
            String attr;
            do {
                if (!var2.hasNext()) {
                    log.info("End : identify common attr in parent & child ");
                    return commonAttr;
                }

                attr = (String) var2.next();
            } while (!attr.startsWith("PARENT.") && !attr.startsWith("CHILD."));

            commonAttr.add(attr);
        }
    }

    public static ConnectorObjectBuilder handleCommonParentChildAttr(
            ConnectorObjectBuilder builder,
            Object attributeVal,
            String attr
    ) {
        log.info("Begin : Handling common attr in parent & child ");
        if (attributeVal != null && attributeVal instanceof List && attr.startsWith("PARENT.")) {
            builder.addAttribute(AttributeBuilder.build(attr, ((List) attributeVal).get(0)));
        } else if (attributeVal != null && attributeVal instanceof List && attr.startsWith("CHILD.")) {
            List<Object> temp = new ArrayList();
            temp.addAll((List) attributeVal);
            temp.remove(0);
            builder.addAttribute(AttributeBuilder.build(attr, temp));
        } else {
            log.error("Failed to retrieve value of attribute {0} from the response", attr);
        }

        log.info("End : Handling common attr in parent & child ");
        return builder;
    }

    public static List<Object> handleAttrNamePlaceHoldersInRelURI(
            ObjectClass oclass,
            Set<Attribute> attrSet,
            List<Object> urlOpTypeList,
            Map<String, Map<String, String>> namedAttributeMap,
            Map<String, List<String>> simpleMultivaluedAttributesMap,
            GenericRESTConfiguration configuration
    ) {
        log.ok("Begin : handle attribute name placeholders in RelURI");
        String relURI = (String) urlOpTypeList.get(0);
        log.ok("relURI is, {0}", relURI);
        if (relURI != null && relURI.contains("$(") && relURI.contains(")$")) {
            List<String> multiVal = new ArrayList();
            if (simpleMultivaluedAttributesMap != null && simpleMultivaluedAttributesMap.containsKey(oclass.getObjectClassValue())) {
                multiVal = simpleMultivaluedAttributesMap.get(oclass.getObjectClassValue());
            }

            log.ok("simple Multi-valued Attributes are, {0}", multiVal);
            Map<String, String> namedAttributes = namedAttributeMap.get(oclass.getObjectClassValue());
            if (namedAttributes == null) {
                log.error("Unable to find UID and Name attributes for the given object class {0}.", oclass.getObjectClassValue());
                throw new ConnectorException(configuration.getMessage("ex.nullNamedAttributes", "Unable to find UID and Name attributes for the given object class " + oclass.getObjectClassValue() + ".", oclass.getObjectClassValue()));
            }

            log.ok("namedAttributes are, {0}", namedAttributes);
            Set<Attribute> updAttrSet = new HashSet(attrSet);
            Map<String, Object> attrNameValueMap = ClientHandler.convertAttrSetToMap(oclass, updAttrSet, null, multiVal, namedAttributes);
            log.ok("attrNameValueMap is, {0}", attrNameValueMap);
            List<String> attrListInRelURI = new ArrayList();
            Map<Integer, Integer> indexPlaceholder = RESTCommonUtils.searchPlaceHolders(relURI);
            Iterator var13 = indexPlaceholder.keySet().iterator();

            while (var13.hasNext()) {
                Integer startindex = (Integer) var13.next();
                attrListInRelURI.add(relURI.substring(startindex + "$(".length(), indexPlaceholder.get(startindex)));
            }

            log.ok("attrListInRelURI are, {0}", attrListInRelURI);
            var13 = attrListInRelURI.iterator();

            while (var13.hasNext()) {
                String attr = (String) var13.next();
                if (attrNameValueMap.containsKey(attr)) {
                    relURI = relURI.replace("$(" + attr + ")$", (String) attrNameValueMap.get(attr));
                }
            }

            log.ok("after replacing attribute name placeholders, relURI is, {0}", relURI);
            urlOpTypeList.set(0, relURI);
        }

        log.ok("returning urlOpTypeList is, {0}", urlOpTypeList);
        log.ok("End : handle attribute name placeholders in RelURI");
        return urlOpTypeList;
    }
}
