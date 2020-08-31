package com.samourai.sentinel.core.access;

import android.content.Context;
import android.util.Log;

import com.samourai.sentinel.util.Hash;

import org.jetbrains.annotations.NotNull;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//import android.util.Log;

public class AccessFactory {

    private static long TIMEOUT_DELAY = 1000 * 60 * 15;
    private static long lastPin = 0L;

    public static final int MIN_PIN_LENGTH = 5;
    public static final int MAX_PIN_LENGTH = 8;

    private static boolean isLoggedIn = false;
    private static String _pin = "";

    private static AccessFactory instance = null;

    private AccessFactory() {
        ;
    }

    public static AccessFactory getInstance(Context ctx) {
        if (instance == null) {
            instance = new AccessFactory();
        }
        return instance;
    }

    public void setIsLoggedIn(boolean logged) {
        isLoggedIn = logged;
    }


    public boolean validateHash(@NotNull String pin, String pinHash) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] b = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
        Hash hash = new Hash(b);
        return hash.toString().equals(pinHash);
    }

    public String getPin() {
        return _pin;
    }

    public void setPin(@NotNull String pin) {
        _pin = pin;
        updatePin();
    }


    public void updatePin() {
        lastPin = System.currentTimeMillis();
    }

    public boolean isTimedOut() {
        Log.i("isTimedOut", String.valueOf((System.currentTimeMillis() - lastPin) > TIMEOUT_DELAY));
        return (System.currentTimeMillis() - lastPin) > TIMEOUT_DELAY;
    }

    public void reset() {
        lastPin = 0L;
    }
}
