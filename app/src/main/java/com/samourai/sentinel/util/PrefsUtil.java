package com.samourai.sentinel.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PrefsUtil {

    public static final String CURRENT_FIAT = "currentFiat";
    public static final String CURRENT_FIAT_SEL = "currentFiatSel";
	public static final String CURRENT_EXCHANGE = "currentExchange";
	public static final String CURRENT_EXCHANGE_SEL = "currentExchangeSel";
    public static final String BLOCK_EXPLORER = "blockExplorer";
    public static final String FIRST_RUN = "1stRun";
    public static final String SIM_IMSI = "IMSI";
    public static final String PIN_HASH = "pinHash";
    public static final String XPUB = "xpub";
    public static final String SCRAMBLE_PIN = "scramblePin";
	public static final String HAPTIC_PIN = "hapticPin";

	private static Context context = null;
	private static PrefsUtil instance = null;

	private PrefsUtil() { ; }

	public static PrefsUtil getInstance(Context ctx) {

		context = ctx;
		
		if(instance == null) {
			instance = new PrefsUtil();
		}
		
		return instance;
	}
	
	public String getValue(String name, String value) {
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    return prefs.getString(name, (value == null || value.length() < 1) ? "" : value);
	}

	public boolean setValue(String name, String value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putString(name, (value == null || value.length() < 1) ? "" : value);
		return editor.commit();
	}

	public int getValue(String name, int value) {
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    return prefs.getInt(name, 0);
	}

	public boolean setValue(String name, int value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putInt(name, (value < 0) ? 0 : value);
		return editor.commit();
	}

	public boolean getValue(String name, boolean value) {
	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    return prefs.getBoolean(name, value);
	}

	public boolean setValue(String name, boolean value) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.putBoolean(name, value);
		return editor.commit();
	}

	public boolean removeValue(String name) {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.remove(name);
		return editor.commit();
	}

	public boolean clear() {
		Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		editor.clear();
		return editor.commit();
	}

}
