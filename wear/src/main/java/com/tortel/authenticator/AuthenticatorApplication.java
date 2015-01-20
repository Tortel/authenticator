package com.tortel.authenticator;

import android.app.Application;

import com.tortel.authenticator.common.utils.DependencyInjector;

/**
 * Application wrapper that sets up/closes the dependency injector
 */
public class AuthenticatorApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DependencyInjector.configure(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        DependencyInjector.close();
        super.onTerminate();
    }
}
