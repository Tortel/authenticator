package com.tortel.authenticator.service;

import android.net.Uri;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.tortel.authenticator.common.sync.SyncUtils;
import com.tortel.authenticator.common.utils.AccountDb;
import com.tortel.authenticator.common.utils.DependencyInjector;
import com.tortel.authenticator.common.utils.Log;

/**
 * Service which listens for incoming data
 */
public class DataListenerService extends WearableListenerService {

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("onDataChanged");
        for(DataEvent event : dataEvents){
            int id = getId(event.getDataItem().getUri());

            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d("DataItem deleted: " + event.getDataItem().getUri());
                deleteAccount(id);
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d("DataItem changed: " + event.getDataItem().getUri());
                DataMapItem mapItem = DataMapItem.fromDataItem(event.getDataItem());
                updateAccount(id, mapItem.getDataMap());
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("onMessageReceived "+messageEvent);
    }

    private void deleteAccount(int id){
        Log.d("Deleting account id "+id);
        AccountDb accountDb = DependencyInjector.getAccountDb();
        accountDb.delete(id);
    }

    private void updateAccount(int id, DataMap dataMap){
        Log.d("Updating account with id "+id);
        String email = dataMap.getString(SyncUtils.ACCOUNT_EMAIL);
        String secret = dataMap.getString(SyncUtils.ACCOUNT_SECRET);
        AccountDb.OtpType type = AccountDb.OtpType.getEnum(dataMap.getInt(SyncUtils.ACCOUNT_TYPE));
        int counter = dataMap.getInt(SyncUtils.ACCOUNT_COUNTER);
        int provider = dataMap.getInt(SyncUtils.ACCOUNT_PROVIDER);

        AccountDb accountDb = DependencyInjector.getAccountDb();
        accountDb.update(id, email, secret, type, counter, provider);
    }

    private int getId(Uri uri){
        return Integer.parseInt(uri.getLastPathSegment());
    }
}
