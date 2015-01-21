package com.tortel.authenticator.export;

import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tortel.authenticator.R;
import com.tortel.authenticator.common.data.AccountDb;
import com.tortel.authenticator.common.utils.DependencyInjector;
import com.tortel.authenticator.export.AccountContainer.Account;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Class for handling importing the accounts from a file
 */
public class FileImportActivity extends ActionBarActivity {
    private static final String DIALOG_TAG = "dialog";

    private EditText passPhraseInput;
    private AccountContainer container;

    private List<Account> accounts;
    private List<Account> selectedItems;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_import);

        passPhraseInput = (EditText) findViewById(R.id.file_export_pass);
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

    private void showImportSelection() {
        if (container == null) {
            return;
        }
        accounts = container.getAccounts();
        selectedItems = new ArrayList<Account>(accounts);

        ImportDialogFragment dialog = new ImportDialogFragment();
        dialog.show(getFragmentManager(), DIALOG_TAG);
    }

    private void doImport() {
        AccountDb db = DependencyInjector.getAccountDb();
        if (selectedItems.size() > 0) {
            for (Account account : selectedItems) {
                //TODO: Check for possible duplicates
                Log.v("Tortel", "Importing " + account.getEmail());
                db.update(null, account.getEmail(), account.getSecret(), account.getType(), account.getCounter());
            }

            Toast.makeText(this, R.string.import_successful, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.import_no_accounts, Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @SuppressLint("ValidFragment")
    public class ImportDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String[] accountNames = new String[accounts.size()];
            boolean[] checked = new boolean[accounts.size()];

            for (int i = 0; i < accounts.size(); i++) {
                Account account = accounts.get(i);
                accountNames[i] = account.getEmail();
                checked[i] = true;
            }


            AlertDialog.Builder builder = new AlertDialog.Builder(FileImportActivity.this);
            builder.setTitle(R.string.import_accounts)
                    .setMultiChoiceItems(accountNames, checked,
                            new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                    if (isChecked) {
                                        selectedItems.add(accounts.get(which));
                                    } else {
                                        selectedItems.remove(accounts.get(which));
                                    }
                                }
                            })
                    .setPositiveButton(R.string.import_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doImport();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing?
                        }
                    });
            return builder.create();

        }
    }

    private class ExportTask extends AsyncTask<Void, Integer, Integer> {
        private String key;

        public ExportTask(String key) {
            this.key = key;
        }

        protected void onPreExecute() {
            // Might not need
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                ObjectMapper mapper = new ObjectMapper();

                String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/authenticator.bk";
                String json = EncryptionUtils.readFile(fileName, key);

                container = mapper.readValue(json, AccountContainer.class);

            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return R.string.error_json_parsing;
            } catch (InvalidKeyException e) {
                e.printStackTrace();
                return R.string.error_wrong_pass;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return R.string.error_unknown;
            }

            return null;
        }

        protected void onPostExecute(Integer result) {
            if (result != null) {
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getBaseContext(), R.string.import_loaded, Toast.LENGTH_SHORT).show();
                showImportSelection();
            }
        }
    }
}
