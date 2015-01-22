package com.tortel.authenticator.common.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A class which contains all the information about an account.
 */
public class AccountInfo implements Parcelable {
    private final int id;
    private final String secret;
    private final AccountDb.OtpType type;
    private String name;
    private int counter;
    private String code;

    AccountInfo(int id, String name, String secret, AccountDb.OtpType type, int counter){
        this.id = id;
        this.secret = secret;
        this.type = type;
        this.name = name;
        this.counter = counter;
    }

    @Override
    public boolean equals(Object o){
        return o instanceof AccountInfo && id == ((AccountInfo) o).id;
    }

    public boolean isHtop(){
        return type == AccountDb.OtpType.HOTP;
    }

    public AccountDb.OtpType getType(){
        return type;
    }
    public int getId() {
        return id;
    }
    public String getSecret() {
        return secret;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getCounter() {
        return counter;
    }
    public void setCounter(Integer counter) {
        this.counter = counter;
    }
    public String getCode() {
        return code;
    }
    public void setCode(String code) {
        this.code = code;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.secret);
        dest.writeInt(this.type == null ? -1 : this.type.ordinal());
        dest.writeString(this.name);
        dest.writeInt(this.counter);
        dest.writeString(this.code);
    }

    private AccountInfo(Parcel in) {
        this.id = in.readInt();
        this.secret = in.readString();
        int tmpType = in.readInt();
        this.type = tmpType == -1 ? null : AccountDb.OtpType.values()[tmpType];
        this.name = in.readString();
        this.counter = in.readInt();
        this.code = in.readString();
    }

    public static final Parcelable.Creator<AccountInfo> CREATOR = new Parcelable.Creator<AccountInfo>() {
        public AccountInfo createFromParcel(Parcel source) {
            return new AccountInfo(source);
        }

        public AccountInfo[] newArray(int size) {
            return new AccountInfo[size];
        }
    };
}
