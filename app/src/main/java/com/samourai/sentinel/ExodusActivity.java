package com.samourai.sentinel;

import android.app.Activity;
import android.os.Bundle;

import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.TimeOutUtil;

public class ExodusActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TimeOutUtil.getInstance().reset();

        finish();
    }

}
