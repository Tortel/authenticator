/*
 * Copyright 2010 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tortel.authenticator.common.data;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Process;
import android.util.Log;

import com.tortel.authenticator.common.otp.PasscodeGenerator;
import com.tortel.authenticator.common.utils.Base32String;
import com.tortel.authenticator.common.utils.FileUtilities;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * A database of email addresses and secret values
 *
 * @author sweis@google.com (Steve Weis)
 */
public class AccountDb {
    public static final int DEFAULT_HOTP_COUNTER = 0;

    private static final String ID_COLUMN = "_id";
    private static final String EMAIL_COLUMN = "email";
    private static final String SECRET_COLUMN = "secret";
    private static final String COUNTER_COLUMN = "counter";
    private static final String TYPE_COLUMN = "type";
    static final String PROVIDER_COLUMN = "provider";
    static final String TABLE_NAME = "accounts";
    static final String PATH = "databases";

    private static final String TABLE_INFO_COLUMN_NAME_COLUMN = "name";

    private static final int PROVIDER_UNKNOWN = 0;
    private static final int PROVIDER_GOOGLE = 1;

    SQLiteDatabase mDatabase;

    private static final String LOCAL_TAG = "GoogleAuthenticator.AccountDb";

    /**
     * Types of secret keys.
     */
    public enum OtpType { // must be the same as in res/values/strings.xml:type
        TOTP(0), // time based
        HOTP(1); // counter based

        public final Integer value; // value as stored in SQLite database

        OtpType(Integer value) {
            this.value = value;
        }

        public static OtpType getEnum(Integer i) {
            for (OtpType type : OtpType.values()) {
                if (type.value.equals(i)) {
                    return type;
                }
            }

            return null;
        }

    }

    public AccountDb(Context context) {
        mDatabase = openDatabase(context);

        // Create the table if it doesn't exist
        mDatabase
                .execSQL(String
                        .format("CREATE TABLE IF NOT EXISTS %s"
                                        + " (%s INTEGER PRIMARY KEY, %s TEXT NOT NULL, %s TEXT NOT NULL, "
                                        + " %s INTEGER DEFAULT %s, %s INTEGER, %s INTEGER DEFAULT %s)",
                                TABLE_NAME, ID_COLUMN, EMAIL_COLUMN,
                                SECRET_COLUMN, COUNTER_COLUMN,
                                DEFAULT_HOTP_COUNTER, TYPE_COLUMN,
                                PROVIDER_COLUMN, PROVIDER_UNKNOWN));

        Collection<String> tableColumnNames = listTableColumnNamesLowerCase();
        if (!tableColumnNames.contains(PROVIDER_COLUMN.toLowerCase(Locale.US))) {
            // Migrate from old schema where the PROVIDER_COLUMN wasn't there
            mDatabase.execSQL(String.format(
                    "ALTER TABLE %s ADD COLUMN %s INTEGER DEFAULT %s",
                    TABLE_NAME, PROVIDER_COLUMN, PROVIDER_UNKNOWN));
        }
    }

    /*
     * Tries three times to open database before throwing
     * AccountDbOpenException.
     */
    private SQLiteDatabase openDatabase(Context context) {
        for (int count = 0; true; count++) {
            try {
                return context.openOrCreateDatabase(PATH, Context.MODE_PRIVATE,
                        null);
            } catch (SQLiteException e) {
                if (count < 2) {
                    continue;
                } else {
                    throw new AccountDbOpenException(
                            "Failed to open AccountDb database in three tries.\n"
                                    + getAccountDbOpenFailedErrorString(context),
                            e);
                }
            }
        }
    }

    private String getAccountDbOpenFailedErrorString(Context context) {
        String dataPackageDir = context.getApplicationInfo().dataDir;
        String databaseDirPathname = context.getDatabasePath(PATH).getParent();
        String databasePathname = context.getDatabasePath(PATH)
                .getAbsolutePath();
        String[] dirsToStat = new String[]{dataPackageDir,
                databaseDirPathname, databasePathname};
        StringBuilder error = new StringBuilder();
        int myUid = Process.myUid();
        for (String directory : dirsToStat) {
            try {
                FileUtilities.StatStruct stat = FileUtilities
                        .getStat(directory);
                String ownerUidName = null;
                try {
                    if (stat.uid == 0) {
                        ownerUidName = "root";
                    } else {
                        PackageManager packageManager = context
                                .getPackageManager();
                        ownerUidName = (packageManager != null) ? packageManager
                                .getNameForUid(stat.uid) : null;
                    }
                } catch (Exception e) {
                    ownerUidName = e.toString();
                }
                error.append(directory + " directory stat (my UID: " + myUid);
                if (ownerUidName == null) {
                    error.append("): ");
                } else {
                    error.append(", dir owner UID name: " + ownerUidName
                            + "): ");
                }
                error.append(stat.toString() + "\n");
            } catch (IOException e) {
                error.append(directory + " directory stat threw an exception: "
                        + e + "\n");
            }
        }
        return error.toString();
    }

    /**
     * Closes this database and releases any system resources held.
     */
    public void close() {
        mDatabase.close();
    }

    /**
     * Lists the names of all the columns in the specified table.
     */
    public static Collection<String> listTableColumnNamesLowerCase(
            SQLiteDatabase database, String tableName) {
        Cursor cursor = database.rawQuery(
                String.format("PRAGMA table_info(%s)", tableName),
                new String[0]);
        Collection<String> result = new ArrayList<String>();
        try {
            if (cursor != null) {
                int nameColumnIndex = cursor
                        .getColumnIndexOrThrow(TABLE_INFO_COLUMN_NAME_COLUMN);
                while (cursor.moveToNext()) {
                    result.add(cursor.getString(nameColumnIndex).toLowerCase(
                            Locale.US));
                }
            }
            return result;
        } finally {
            tryCloseCursor(cursor);
        }
    }

    /**
     * Lists the names of all the columns in the accounts table.
     */
    private Collection<String> listTableColumnNamesLowerCase() {
        return listTableColumnNamesLowerCase(mDatabase, TABLE_NAME);
    }

    /*
     * deleteAllData() will remove all rows. Useful for testing.
     */
    public boolean deleteAllData() {
        mDatabase.delete(AccountDb.TABLE_NAME, null, null);
        return true;
    }

    public String getSecret(int id) {
        Cursor cursor = getAccount(id);
        try {
            if (!cursorIsEmpty(cursor)) {
                cursor.moveToFirst();
                return cursor.getString(cursor.getColumnIndex(SECRET_COLUMN));
            }
        } finally {
            tryCloseCursor(cursor);
        }
        return null;
    }

    /**
     * @param id
     * @return
     */
    public String getEmail(int id) {
        Cursor cursor = getAccount(id);
        try {
            if (!cursorIsEmpty(cursor)) {
                cursor.moveToFirst();
                return cursor.getString(cursor.getColumnIndex(EMAIL_COLUMN));
            }
        } finally {
            tryCloseCursor(cursor);
        }
        return null;
    }

    public static PasscodeGenerator.Signer getSigningOracle(String secret) {
        try {
            byte[] keyBytes = decodeKey(secret);
            final Mac mac = Mac.getInstance("HMACSHA1");
            mac.init(new SecretKeySpec(keyBytes, ""));

            // Create a signer object out of the standard Java MAC
            // implementation.
            return new PasscodeGenerator.Signer() {
                @Override
                public byte[] sign(byte[] data) {
                    return mac.doFinal(data);
                }
            };
        } catch (Base32String.DecodingException | NoSuchAlgorithmException | InvalidKeyException error) {
            Log.e(LOCAL_TAG, error.getMessage());
        }

        return null;
    }

    private static byte[] decodeKey(String secret) throws Base32String.DecodingException, Base32String.DecodingException {
        return Base32String.decode(secret);
    }

    public Integer getCounter(int id) {
        Cursor cursor = getAccount(id);
        try {
            if (!cursorIsEmpty(cursor)) {
                cursor.moveToFirst();
                return cursor.getInt(cursor.getColumnIndex(COUNTER_COLUMN));
            }
        } finally {
            tryCloseCursor(cursor);
        }
        return null;
    }

    public void incrementCounter(int id) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, id);
        Integer counter = getCounter(id);
        values.put(COUNTER_COLUMN, counter + 1);
        mDatabase.update(TABLE_NAME, values, whereClause(id), null);
    }

    public OtpType getType(int id) {
        Cursor cursor = getAccount(id);
        try {
            if (!cursorIsEmpty(cursor)) {
                cursor.moveToFirst();
                Integer value = cursor.getInt(cursor
                        .getColumnIndex(TYPE_COLUMN));
                return OtpType.getEnum(value);
            }
        } finally {
            tryCloseCursor(cursor);
        }
        return null;
    }

    public void setEmail(int id, String email) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, id);
        values.put(EMAIL_COLUMN, email);
        mDatabase.update(TABLE_NAME, values, whereClause(id), null);
    }

    @Deprecated
    public boolean isGoogleAccount(Integer id) {
        return false;
    }

    /**
     * Get the provider of an account
     * @param id
     * @return
     */
    public int getProvider(int id){
        Cursor cursor = getAccount(id);
        try {
            if (!cursorIsEmpty(cursor)) {
                cursor.moveToFirst();
                return cursor.getInt(cursor.getColumnIndex(PROVIDER_COLUMN));
            }
        } finally {
            tryCloseCursor(cursor);
        }
        return 0;
    }

    private static String whereClause(int id) {
        return ID_COLUMN + " = " + id;
    }

    public void delete(int id) {
        mDatabase.delete(TABLE_NAME, whereClause(id), null);
    }

    /**
     * Save key to database, creating a new user entry if necessary.
     *
     * @param email   the user email address. When editing, the new user email.
     * @param secret  the secret key.
     * @param type    hotp vs totp
     * @param counter only important for the hotp type
     */
    public void update(Integer id, String email, String secret,
                       OtpType type, Integer counter) {
        update(id, email, secret, type, counter, null);
    }

    /**
     * Save key to database, creating a new user entry if necessary.
     *
     * @param email         the user email address. When editing, the new user email.
     * @param secret        the secret key.
     * @param type          hotp vs totp
     * @param counter       only important for the hotp type
     * @param provider        the id of the provider
     */
    public void update(Integer id, String email, String secret,
                       OtpType type, Integer counter, Integer provider) {
        ContentValues values = new ContentValues();
        values.put(EMAIL_COLUMN, email);
        values.put(SECRET_COLUMN, secret);
        values.put(TYPE_COLUMN, type.ordinal());
        values.put(COUNTER_COLUMN, counter);
        values.put(PROVIDER_COLUMN, provider != null ? provider : PROVIDER_UNKNOWN);

        int updated = 0;
        // Don't bother trying it if no ID was specified
        if (id != null) {
            updated = mDatabase.update(TABLE_NAME, values,
                    whereClause(id), null);
        }

        if (updated == 0) {
            mDatabase.insert(TABLE_NAME, null, values);
        }
    }

    private Cursor getIds() {
        return mDatabase.query(TABLE_NAME, null, null, null, null, null, null,
                null);
    }

    private Cursor getAccount(int id) {
        return mDatabase.query(TABLE_NAME, null, ID_COLUMN + "= ?",
                new String[]{"" + id}, null, null, null);
    }

    /**
     * Returns true if the cursor is null, or contains no rows.
     */
    private static boolean cursorIsEmpty(Cursor c) {
        return c == null || c.getCount() == 0;
    }

    /**
     * Closes the cursor if it is not null and not closed.
     */
    private static void tryCloseCursor(Cursor c) {
        if (c != null && !c.isClosed()) {
            c.close();
        }
    }

    /**
     * Get list of all account names.
     *
     * @param result Collection of strings-- account names are appended, without
     *               clearing this collection on entry.
     * @return Number of accounts added to the output parameter.
     */
    public int getIds(Collection<Integer> result) {
        Cursor cursor = getIds();

        try {
            if (cursorIsEmpty(cursor))
                return 0;

            int nameCount = cursor.getCount();
            int index = cursor.getColumnIndex(AccountDb.ID_COLUMN);

            for (int i = 0; i < nameCount; ++i) {
                cursor.moveToPosition(i);
                Integer id = cursor.getInt(index);
                result.add(id);
            }

            return nameCount;
        } finally {
            tryCloseCursor(cursor);
        }
    }

    /**
     * Gets a list of all the account IDs
     * @return
     */
    public List<Integer> getAllIds(){
        ArrayList<Integer> ids = new ArrayList<>();
        getIds(ids);
        return ids;
    }

    public AccountInfo getAccountInfo(int id){
        Cursor c = getAccount(id);
        try{
            if(!c.moveToFirst()){
                return null;
            }
            int emailIndex = c.getColumnIndex(EMAIL_COLUMN);
            int typeIndex = c.getColumnIndex(TYPE_COLUMN);
            int secretIndex = c.getColumnIndex(SECRET_COLUMN);
            int counterIndex = c.getColumnIndex(COUNTER_COLUMN);

            String email = c.getString(emailIndex);
            String secret = c.getString(secretIndex);
            OtpType type = c.getInt(typeIndex) == 0 ? OtpType.TOTP : OtpType.HOTP;
            int counter = c.getInt(counterIndex);

            return new AccountInfo(id, email, secret, type, counter);
        } finally {
            tryCloseCursor(c);
        }
    }

    /**
     * Get a list of all the account info
     * @return
     */
    public List<AccountInfo> getAllAccounts(){
        Cursor c = getIds();
        int count = c.getCount();
        List<AccountInfo> accounts = new ArrayList<>(count);
        try {
            int idIndex = c.getColumnIndex(ID_COLUMN);
            int emailIndex = c.getColumnIndex(EMAIL_COLUMN);
            int typeIndex = c.getColumnIndex(TYPE_COLUMN);
            int secretIndex = c.getColumnIndex(SECRET_COLUMN);
            int counterIndex = c.getColumnIndex(COUNTER_COLUMN);

            for(int i =0; i < count; i++){
                c.moveToPosition(i);
                int id = c.getInt(idIndex);
                String email = c.getString(emailIndex);
                String secret = c.getString(secretIndex);
                OtpType type = c.getInt(typeIndex) == 0 ? OtpType.TOTP : OtpType.HOTP;
                int counter = c.getInt(counterIndex);

                accounts.add(new AccountInfo(id, email, secret, type, counter));
            }

        } finally {
            tryCloseCursor(c);
        }
        return accounts;
    }

    /**
     * Get the count of items in the DB
     * @return the count
     */
    public int getCount(){
        return getIds().getCount();
    }

    private static class AccountDbOpenException extends RuntimeException {
        private static final long serialVersionUID = -4561104600253698948L;

        public AccountDbOpenException(String message, Exception e) {
            super(message, e);
        }
    }

}
