package com.samourai.sentinel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.PrefsUtil;

public class OneTimePopup extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new AlertDialog.Builder(OneTimePopup.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.new_version_message)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        PrefsUtil.getInstance(OneTimePopup.this).setValue("popup_" + getResources().getString(R.string.version_name), true);
                        AppUtil.getInstance(OneTimePopup.this).restartApp();
                    }
                }).show();

    }

}
