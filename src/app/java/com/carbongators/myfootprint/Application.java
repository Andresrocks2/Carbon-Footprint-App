package com.carbongators.myfootprint;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Application object; ensures that the support library is correctly configured for use of
 * vector drawables.
 */
public final class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
}
