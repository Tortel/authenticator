package com.tortel.authenticator.activity;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.tortel.authenticator.AccountDb;
import com.tortel.authenticator.R;
import com.tortel.authenticator.dialog.DownloadScannerDialog;
import com.tortel.authenticator.export.FileExportActivity;
import com.tortel.authenticator.export.FileImportActivity;
import com.tortel.authenticator.fragment.CodeListFragment;
import com.tortel.authenticator.fragment.NoAccountsFragment;
import com.tortel.authenticator.utils.DependencyInjector;
import com.tortel.authenticator.utils.Log;
import com.tortel.authenticator.utils.Utilities;

import java.util.List;

/**
 * Main activity that shows the codes and stuff
 */
public class MainActivity extends ActionBarActivity {

    private AccountDb mAccountDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_activity);

        mAccountDb = DependencyInjector.getAccountDb();

        // Check if the fragment is null
        if(getSupportFragmentManager().findFragmentById(R.id.content_frame) == null){
            // Display the fragment

            if(mAccountDb.getCount() > 0){
                Fragment frag = new CodeListFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, frag).commit();
            } else {
                Fragment frag = new NoAccountsFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, frag).commit();
            }
        }
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
}
