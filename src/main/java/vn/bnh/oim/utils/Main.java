package vn.bnh.oim.utils;

import oracle.iam.identity.usermgmt.api.UserManager;
import oracle.iam.identity.usermgmt.vo.User;
import oracle.iam.platform.OIMClient;
import oracle.iam.platform.entitymgr.vo.SearchCriteria;
import oracle.iam.provisioning.api.ProvisioningConstants;
import oracle.iam.provisioning.api.ProvisioningService;
import oracle.iam.provisioning.vo.Account;
import oracle.iam.provisioning.vo.AccountData;
import oracle.iam.provisioning.vo.ApplicationInstance;
import oracle.iam.provisioning.vo.ChildTableRecord;

import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Exception {
        HashMap<String, String> map = new HashMap<>();
        map.put("OIM_ADMIN_USERNAME", "xelsysadm");
        map.put("OIM_ADMIN_PASSWORD", "oracle_4U");
        map.put("OIM_HOSTNAME", "10.10.11.54");
        map.put("OIM_PORT", "14000");
        OIMUtils oim = new OIMUtils(map);
        OIMClient client = oim.getOimClient();
        UserManager usrService = client.getService(UserManager.class);
        ProvisioningService proSer = client.getService(ProvisioningService.class);
        User beneficiary = usrService.getDetails("USR2@ORACLE.COM", new HashSet<String>(), true);
        String usrKey = beneficiary.getId();
        SearchCriteria provisionedCriteria = new SearchCriteria(ProvisioningConstants.AccountSearchAttribute.ACCOUNT_STATUS.getId(), ProvisioningConstants.ObjectStatus.PROVISIONING.getId(), SearchCriteria.Operator.EQUAL);
        List<Account> provInstList = proSer.getAccountsProvisionedToUser(usrKey, provisionedCriteria, new HashMap<>(), true);
        Set<AccountData> accDataList = new HashSet<>();
        for (Account account : provInstList) {
            accDataList.add(account.getAccountData());
        }
        ApplicationInstance appInst = provInstList.get(0).getAppInstance();
        String udTblPk = String.valueOf(accDataList.stream().max(Comparator.comparing(AccountData::getUdTablePrimaryKey)));
        Map<String, Object> accountDataValue = new HashMap<>();
        accountDataValue.put("UD_TRM_USER_NAME", "java generated");
        AccountData accountData = new AccountData(String.valueOf(44), udTblPk, accountDataValue);
        ChildTableRecord ctr = new ChildTableRecord();
        Map<String, Object> childData = new HashMap<>();
        childData.put("UD_ROLES1_GROUP_ID", "123");
        childData.put("UD_ROLES1_ROLE_ID", "123");
        ctr.setChildData(childData);
        ctr.setRowKey("1");
        ctr.setAction(ChildTableRecord.ACTION.Add);
        Map<String, ArrayList<ChildTableRecord>> childRecords = new HashMap<>();
        ArrayList<ChildTableRecord> childRecArray = new ArrayList<>();
        childRecArray.add(ctr);
        childRecords.put("UD_ROLES1", childRecArray);
        accountData.addChildData(childRecords);

        Account accToProvision = new Account(appInst, accountData);
        proSer.provision(usrKey, accToProvision);
//        provInstList.stream().forEach(x -> {
//            System.out.println("form key: " + x.getAccountData().getFormKey());
//            System.out.println("tbl pk: " + x.getAccountData().getUdTablePrimaryKey());
//            System.out.println(x.getAccountData().getData());
//            System.out.println(x.getAccountID());
//            System.out.println(x.getProcessInstanceKey());
//
//        });
    }
}
