package com.tortel.authenticator.common.sync;

import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.tortel.authenticator.common.utils.AccountDb;

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
    public static PutDataMapRequest createDataMap(Integer id, AccountDb accountDb){
        PutDataMapRequest data = PutDataMapRequest.create(ACCOUNT_PATH+id);
        DataMap map = data.getDataMap();
        map.putString(ACCOUNT_EMAIL, accountDb.getEmail(id));
        map.putString(ACCOUNT_SECRET, accountDb.getSecret(id));
        map.putInt(ACCOUNT_TYPE, accountDb.getType(id).value);
        map.putInt(ACCOUNT_COUNTER, accountDb.getCounter(id));
        map.putInt(ACCOUNT_PROVIDER, accountDb.getProvider(id));

        return data;
    }

}
