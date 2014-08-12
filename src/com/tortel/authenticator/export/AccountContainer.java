package com.tortel.authenticator.export;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tortel.authenticator.AccountDb;
import com.tortel.authenticator.AccountDb.OtpType;

/**
 * A POJO container for account details
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountContainer {
    public static int CURRENT_VERSION = 1;
    
    private int version;
    private List<Account> accounts;
    
    public AccountContainer(){
        accounts = new LinkedList<Account>();
        this.version = CURRENT_VERSION;
    }
    
    public void addAccount(Account account){
        accounts.add(account);
    }
    
    public List<Account> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }
    
    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    } 
    
    /**
     * Returns a populated container object
     * @param accountDb
     * @return
     */
    public static AccountContainer prepareExport(AccountDb accountDb){
        AccountContainer container = new AccountContainer();
        List<Integer> ids = new LinkedList<Integer>();
        accountDb.getIds(ids);
        
        for(Integer id : ids){
            AccountContainer.Account account = new AccountContainer.Account();
            account.setId(id);
            account.setEmail(accountDb.getEmail(id));
            account.setSecret(accountDb.getSecret(id));
            account.setCounter(accountDb.getCounter(id));
            account.setType(accountDb.getType(id));
            
            container.addAccount(account);
        }
        
        return container;
    }

    /**
     * A POJO for containing the account details.
     * Used for JSON mapping
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Account{
        private int id;
        private String email;
        private String secret;
        private int counter;
        private OtpType type;
        private int provider;
        
        @JsonIgnore
        public OtpType getType() {
            return type;
        }
        @JsonIgnore
        public void setType(OtpType otpType) {
            this.type = otpType;
        }
        
        @JsonProperty("type")
        public int getIntType(){
            if(type == OtpType.HOTP){
                return 1;
            }
            return 0;
        }
        @JsonProperty("type")
        public void setIntType(int type){
            if(type == 1){
                this.type = OtpType.HOTP;
            } else {
                this.type = OtpType.TOTP;
            }
        }
        
        public String getEmail() {
            return email;
        }
        public void setEmail(String email) {
            this.email = email;
        }
        public String getSecret() {
            return secret;
        }
        public void setSecret(String secret) {
            this.secret = secret;
        }
        public int getCounter() {
            return counter;
        }
        public void setCounter(int counter) {
            this.counter = counter;
        }
        public int getProvider() {
            return provider;
        }
        public void setProvider(int provider) {
            this.provider = provider;
        }
        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }
    }
}
