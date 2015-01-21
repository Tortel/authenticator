package com.tortel.authenticator.common.sync;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.tortel.authenticator.common.data.AccountDb;
import com.tortel.authenticator.common.data.AccountInfo;

/**
 * Contains constants for syncing data between the phone and wear apps
 */
public class SyncUtils {
    public static final String ACCOUNT_PATH = "/account/";

    public static final String ACCOUNT_EMAIL = "email";
    public static final String ACCOUNT_SECRET = "secret";
    public static final String ACCOUNT_TYPE = "type";
    public static final String ACCOUNT_COUNTER = "counter";
    public static final String ACCOUNT_PROVIDER = "provider";

    /**
     * Creates a PutDataMapRequest with all the account data in it
     * @param id
     * @param accountDb
     * @return
     */
    public static PutDataMapRequest createDataMap(int id, AccountDb accountDb){
        PutDataMapRequest data = PutDataMapRequest.create(ACCOUNT_PATH+id);
        DataMap map = data.getDataMap();
        AccountInfo info = accountDb.getAccountInfo(id);

        map.putString(ACCOUNT_EMAIL, info.getName());
        map.putString(ACCOUNT_SECRET, info.getSecret());
        map.putInt(ACCOUNT_TYPE, info.getType().value);
        map.putInt(ACCOUNT_COUNTER, info.getCounter());
        // TODO - provider
        map.putInt(ACCOUNT_PROVIDER, 0);

        return data;
    }

}
