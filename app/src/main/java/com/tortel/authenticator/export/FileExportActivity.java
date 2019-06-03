package com.tortel.authenticator.export;

import java.security.InvalidKeyException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tortel.authenticator.R;
import com.tortel.authenticator.common.data.AccountDb;
import com.tortel.authenticator.common.utils.DependencyInjector;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Class for handling exporting the accounts to a file
 */
public class FileExportActivity extends AppCompatActivity {
    private EditText passPhraseInput;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_export);

        passPhraseInput = findViewById(R.id.file_export_pass);
    }

    private void exportData() {
        String passPhrase = passPhraseInput.getText().toString().trim();
        new ExportTask(passPhrase).execute();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.file_export_ok:
                exportData();
                return;
        }
    }

    private class ExportTask extends AsyncTask<Void, Integer, Integer> {
        private AccountDb accountDb;
        private AccountContainer container;
        private String key;

        public ExportTask(String key) {
            this.key = key;
        }

        @Override
        protected void onPreExecute() {
            accountDb = DependencyInjector.getAccountDb();
        }

        @Override
        protected Integer doInBackground(Void... params) {
            container = AccountContainer.prepareExport(accountDb);

            try {
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(container);

                String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/authenticator.bk";
                EncryptionUtils.writeFile(fileName, key, json);

            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return R.string.error_processing;
            } catch (InvalidKeyException e) {
                e.printStackTrace();
                return R.string.error_unknown;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return R.string.error_unknown;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != null) {
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getBaseContext(), R.string.file_export_done, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
