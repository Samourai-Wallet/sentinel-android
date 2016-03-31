package com.samourai.sentinel.access;

import android.content.Context;

//import android.util.Log;

public class AccessFactory	{

    public static final int MIN_PIN_LENGTH = 5;
    public static final int MAX_PIN_LENGTH = 8;

    private static boolean isLoggedIn = false;

    private static Context context = null;
    private static AccessFactory instance = null;

    private AccessFactory()	{ ; }

    public static AccessFactory getInstance(Context ctx) {
    	
    	context = ctx;

        if (instance == null) {
            instance = new AccessFactory();
        }

        return instance;
    }

    public void setIsLoggedIn(boolean logged) {
        isLoggedIn = logged;
    }

}
