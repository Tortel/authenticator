package com.tortel.authenticator.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.tortel.authenticator.R;
import com.tortel.authenticator.common.sync.SyncUtils;
import com.tortel.authenticator.common.data.AccountDb;
import com.tortel.authenticator.common.utils.DependencyInjector;
import com.tortel.authenticator.common.utils.Log;
import com.tortel.authenticator.export.FileExportActivity;
import com.tortel.authenticator.export.FileImportActivity;
import com.tortel.authenticator.fragment.CodeListFragment;
import com.tortel.authenticator.fragment.NoAccountsFragment;

import java.util.List;

/**
 * Main activity that shows the codes and stuff
 */
public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static final String ACCOUNT_CHANGED = "com.tortel.authenticator.ACCOUNT_CHANGED";
    public static final String ACCOUNT_DELETED = "com.tortel.authenticator.ACCOUNT_DELETED";
    public static final String ACCOUNT_CREATED = "com.tortel.authenticator.ACCOUNT_CREATED";
    public static final String ACCOUNT_ID = "id";

    private AccountDb mAccountDb;
    private GoogleApiClient mGoogleApiClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_activity);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        mAccountDb = DependencyInjector.getAccountDb();

        showFragment();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        // Register the receiver for all the events
        broadcastManager.registerReceiver(mAccountChangeReceiver, new IntentFilter(ACCOUNT_CHANGED));
        broadcastManager.registerReceiver(mAccountChangeReceiver, new IntentFilter(ACCOUNT_DELETED));
        broadcastManager.registerReceiver(mAccountChangeReceiver, new IntentFilter(ACCOUNT_CREATED));
    }

    private void showFragment(){
        Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        int count = mAccountDb.getCount();

        Log.d("Current fragment: "+currentFrag);
        Log.d("Count: "+count);

        if(count > 0){
            if(currentFrag instanceof CodeListFragment){
                // Already showing the correct fragment
                return;
            }
            Fragment frag = new CodeListFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, frag).commitAllowingStateLoss();
        } else {
            Fragment frag = new NoAccountsFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, frag).commitAllowingStateLoss();
        }
    }

    /**
     * Sync all accounts to wear
     */
    private void syncAllAccounts(){
        if(mGoogleApiClient.isConnected()){
            List<Integer> ids = mAccountDb.getAllIds();
            for(Integer id : ids){
                PutDataMapRequest req = SyncUtils.createDataMap(id, mAccountDb);
                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, req.asPutDataRequest());
                pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d("Sent "+dataItemResult.toString());
                    }
                });
            }
        }
    }

    /**
     * Sync a specific account to wear
     * @param id
     */
    private void syncAccount(int id){
        if(mGoogleApiClient.isConnected()) {
            PutDataMapRequest req = SyncUtils.createDataMap(id, mAccountDb);
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, req.asPutDataRequest());
            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    Log.d("Sent "+dataItemResult.toString());
                }
            });
        }
    }

    /**
     * Delete a specific account from wear
     * @param id
     */
    private void deleteAccount(int id){
        if(mGoogleApiClient.isConnected()) {
            PutDataMapRequest req = SyncUtils.createDataMap(id, mAccountDb);
            PendingResult<DataApi.DeleteDataItemsResult> pendingResult = Wearable.DataApi.deleteDataItems(mGoogleApiClient, req.getUri());
            pendingResult.setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
                @Override
                public void onResult(DataApi.DeleteDataItemsResult dataItemResult) {
                    Log.d("Deleted "+dataItemResult.toString());
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getBaseContext());
        broadcastManager.unregisterReceiver(mAccountChangeReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.removeItem(R.id.new_main);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        switch (item.getItemId()) {
            case R.id.add_account:
                showAddAccount();
                return true;
            case R.id.how_it_works:
                showHowItWorks();
                return true;
            case R.id.settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.export_file:
                intent = new Intent(this, FileExportActivity.class);
                startActivity(intent);
                return true;
            case R.id.import_file:
                intent = new Intent(this, FileImportActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Opens the add account activity
     */
    public void showAddAccount(){
        Intent intent = new Intent(this, AddAccountActivity.class);
        startActivity(intent);
    }

    /**
     * Opens the how it works activity
     */
    public void showHowItWorks(){
        Intent intent = new Intent(this, HowItWorksActivity.class);
        startActivity(intent);
    }

    private BroadcastReceiver mAccountChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int id;
            switch(intent.getAction()){
                case ACCOUNT_CREATED:
                    showFragment();
                    syncAllAccounts();
                    return;
                case ACCOUNT_DELETED:
                    // TODO - Propigate delete to wear
                    id = intent.getIntExtra(ACCOUNT_ID, -1);
                    if(id > 0){
                        syncAccount(id);
                    }
                    showFragment();
                    return;
                case ACCOUNT_CHANGED:
                    id = intent.getIntExtra(ACCOUNT_ID, -1);
                    if(id > 0){
                        syncAccount(id);
                    }
                    return;
            }
        }
    };

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d("Connection failed "+connectionResult);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("Google API Connected");
        syncAllAccounts();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("Connection suspended");
    }
}
