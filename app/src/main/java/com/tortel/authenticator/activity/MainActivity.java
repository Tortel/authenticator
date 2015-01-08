package com.tortel.authenticator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.tortel.authenticator.AccountDb;
import com.tortel.authenticator.R;
import com.tortel.authenticator.export.FileExportActivity;
import com.tortel.authenticator.export.FileImportActivity;
import com.tortel.authenticator.fragment.NoAccountsFragment;
import com.tortel.authenticator.utils.DependencyInjector;

import java.util.ArrayList;

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
            ArrayList<Integer> ids = new ArrayList<>();
            mAccountDb.getIds(ids);
            if(ids.size() > 0){

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

    public void showAddAccount(){
        Intent intent = new Intent(this, AddAccountActivity.class);
        startActivity(intent);
    }

    public void showHowItWorks(){
        Intent intent = new Intent(this, HowItWorksActivity.class);
        startActivity(intent);
    }
}
