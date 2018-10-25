package com.tortel.authenticator.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.tortel.authenticator.R;
import com.tortel.authenticator.common.sync.SyncUtils;
import com.tortel.authenticator.common.data.AccountDb;
import com.tortel.authenticator.common.utils.DependencyInjector;
import com.tortel.authenticator.common.utils.Log;
import com.tortel.authenticator.export.FileExportActivity;
import com.tortel.authenticator.export.FileImportActivity;
import com.tortel.authenticator.fragment.AddAccountListDialogFragment;
import com.tortel.authenticator.fragment.CodeListFragment;
import com.tortel.authenticator.fragment.NoAccountsFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Main activity that shows the codes and stuff
 */
public class MainActivity extends AppCompatActivity {
    public static final String ACCOUNT_CHANGED = "com.tortel.authenticator.ACCOUNT_CHANGED";
    public static final String ACCOUNT_DELETED = "com.tortel.authenticator.ACCOUNT_DELETED";
    public static final String ACCOUNT_CREATED = "com.tortel.authenticator.ACCOUNT_CREATED";
    public static final String ACCOUNT_ID = "id";

    private AccountDb mAccountDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAccountDb = DependencyInjector.getAccountDb();

        showFragment();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        // Register the receiver for all the events
        broadcastManager.registerReceiver(mAccountChangeReceiver, new IntentFilter(ACCOUNT_CHANGED));
        broadcastManager.registerReceiver(mAccountChangeReceiver, new IntentFilter(ACCOUNT_DELETED));
        broadcastManager.registerReceiver(mAccountChangeReceiver, new IntentFilter(ACCOUNT_CREATED));

        FloatingActionButton fab = findViewById(R.id.add_account_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddAccount();
            }
        });
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
    @SuppressLint("StaticFieldLeak")
    private void syncAllAccounts(){
        final DataClient dataClient = Wearable.getDataClient(this);
        final List<PutDataMapRequest> requests = new ArrayList<>();
        for(Integer id : mAccountDb.getAllIds()) {
            requests.add(SyncUtils.createDataMap(id, mAccountDb));
        }

        (new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    for (PutDataMapRequest req : requests) {
                        DataItem item = Tasks.await(dataClient.putDataItem(req.asPutDataRequest()));
                        Log.d("Sent " + item.toString());
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Log.e("Exception deleting wear data", e);
                }
                return null;
            }
        }).execute();
    }

    /**
     * Sync a specific account to wear
     * @param id
     */
    @SuppressLint("StaticFieldLeak")
    private void syncAccount(final int id){
        final DataClient dataClient = Wearable.getDataClient(this);
        final PutDataMapRequest req = SyncUtils.createDataMap(id, mAccountDb);

        (new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    DataItem item = Tasks.await(dataClient.putDataItem(req.asPutDataRequest()));
                    Log.d("Sent " + item.toString());
                } catch (InterruptedException | ExecutionException e) {
                    Log.e("Exception deleting wear data", e);
                }
                return null;
            }
        }).execute();
    }

    /**
     * Delete a specific account from wear
     * @param id
     */
    @SuppressLint("StaticFieldLeak")
    private void deleteAccount(final int id){
        final DataClient dataClient = Wearable.getDataClient(this);
        final PutDataMapRequest req = SyncUtils.createDataMap(id, mAccountDb);

        (new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    int result = Tasks.await(dataClient.deleteDataItems(req.getUri()));
                    Log.d("Deleted " + req.getUri() + " with result " + result);
                } catch (InterruptedException | ExecutionException e) {
                    Log.e("Exception deleting wear data", e);
                }
                return null;
            }
        }).execute();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
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
        AddAccountListDialogFragment frag = new AddAccountListDialogFragment();

        frag.show(getSupportFragmentManager(), "add");
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
        public void onReceive(Context context, @NonNull Intent intent) {
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

}
