package com.tortel.authenticator.common.data;

/**
 * A class which contains all the information about an account.
 */
public class AccountInfo {
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
}
