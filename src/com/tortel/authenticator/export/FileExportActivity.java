package com.tortel.authenticator.export;

import java.util.LinkedList;
import java.util.List;

import com.tortel.authenticator.AccountDb;
import com.tortel.authenticator.R;
import com.tortel.authenticator.testability.DependencyInjector;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

/**
 * Class for handling exporting the accounts to a file
 */
public class FileExportActivity extends Activity {
    private EditText passPhraseInput;
    
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_export);
        
       passPhraseInput = (EditText) findViewById(R.id.file_export_pass);
    }
    
    private void exportData(){
        String passPhrase = passPhraseInput.getText().toString().trim();
        
    }
    
    public void onClick(View view){
        switch(view.getId()){
        case R.id.file_export_ok:
            exportData();
            return;
        }
    }
    
    private class ExportTask extends AsyncTask<Void, Integer, Boolean>{
        private AccountDb accountDb;
        private AccountContainer container;
        
        protected void onPreExecute(){
            accountDb = DependencyInjector.getAccountDb();
            container = new AccountContainer();
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            List<String> emails = new LinkedList<String>();
            accountDb.getNames(emails);
            
            for(String email : emails){
                AccountContainer.Account account = new AccountContainer.Account();
                account.setEmail(email);
                account.setCounter(accountDb.getCounter(email));
                account.setType(accountDb.getType(email));
                
                container.addAccount(account);
            }
            
            
            
            return null;
        }
    }
}
