package com.tortel.authenticator.export;

import java.security.InvalidKeyException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tortel.authenticator.AccountDb;
import com.tortel.authenticator.R;
import com.tortel.authenticator.testability.DependencyInjector;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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
        new ExportTask(passPhrase).execute();
    }
    
    public void onClick(View view){
        switch(view.getId()){
        case R.id.file_export_ok:
            exportData();
            return;
        }
    }
    
    private class ExportTask extends AsyncTask<Void, Integer, String>{
        private AccountDb accountDb;
        private AccountContainer container;
        private String key;
        
        public ExportTask(String key){
            this.key = key;
        }
        
        protected void onPreExecute(){
            accountDb = DependencyInjector.getAccountDb();
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected String doInBackground(Void... params) {
            container = AccountContainer.prepareExport(accountDb);
            
            try {
            	ObjectMapper mapper = new ObjectMapper();
            	String json = mapper.writeValueAsString(container);
            	
            	String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/authenticator.bk";
            	EncryptionUtils.writeFile(fileName, key, json);
            	
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return e.getMessage();
			} catch (InvalidKeyException e) {
				// TODO: Umm, its writing the data, why would the key be wrong?
				e.printStackTrace();
				return e.getMessage();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return e.getMessage();
			}
            
            return null;
        }
        
        protected void onPostExecute(String result){
        	if(result != null){
        		Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
        	} else {
        		Toast.makeText(getBaseContext(), "Done!", Toast.LENGTH_SHORT).show();
        		FileExportActivity.this.finish();
        	}
        }
    }
}
