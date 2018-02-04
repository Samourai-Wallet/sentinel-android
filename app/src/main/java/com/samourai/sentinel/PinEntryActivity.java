package com.samourai.sentinel;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.sentinel.access.AccessFactory;
import com.samourai.sentinel.access.ScrambledPin;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.Hash;
import com.samourai.sentinel.util.PrefsUtil;
import com.samourai.sentinel.util.TimeOutUtil;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PinEntryActivity extends Activity {

    private Button ta = null;
    private Button tb = null;
    private Button tc = null;
    private Button td = null;
    private Button te = null;
    private Button tf = null;
    private Button tg = null;
    private Button th = null;
    private Button ti = null;
    private Button tj = null;
    private ImageButton tsend = null;
    private ImageButton tback = null;

    private TextView tvPrompt = null;
    private TextView tvUserInput = null;

    private ScrambledPin keypad = null;
    
    private StringBuilder userInput = null;

    private boolean create = false;             // create PIN
    private boolean confirm = false;            // confirm PIN
    private String strConfirm = null;
    private String strSeed = null;
    private String strPassphrase = null;

    private ProgressDialog progress = null;
    private Vibrator vibrator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.grid);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        userInput = new StringBuilder();
        keypad = new ScrambledPin();

        tvUserInput = (TextView)findViewById(R.id.userInput);
        tvUserInput.setText("");

        tvPrompt = (TextView)findViewById(R.id.prompt2);

        boolean scramble = PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.SCRAMBLE_PIN, false);

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey("create") && extras.getBoolean("create") == true)	{
            tvPrompt.setText(R.string.create_pin);
            scramble = false;
            create = true;
            confirm = false;
            strSeed = extras.getString("seed");
            strPassphrase = extras.getString("passphrase");
            Toast.makeText(PinEntryActivity.this, R.string.pin_5_8, Toast.LENGTH_LONG).show();
        }
        else if(extras != null && extras.containsKey("confirm") && extras.getBoolean("confirm") == true)	{
            tvPrompt.setText(R.string.confirm_pin);
            scramble = false;
            create = false;
            confirm = true;
            strConfirm = extras.getString("first");
            strSeed = extras.getString("seed");
            strPassphrase = extras.getString("passphrase");
            Toast.makeText(PinEntryActivity.this, R.string.pin_5_8_confirm, Toast.LENGTH_LONG).show();
        }
        else	{
            ;
        }

        if(strSeed != null && strSeed.length() < 1)	{
            strSeed = null;
        }

        if(strPassphrase == null)	{
            strPassphrase = "";
        }

        ta = (Button)findViewById(R.id.ta);
        ta.setText(scramble ? Integer.toString(keypad.getMatrix().get(0).getValue()) : "1");
        tb = (Button)findViewById(R.id.tb);
        tb.setText(scramble ? Integer.toString(keypad.getMatrix().get(1).getValue()) : "2");
        tc = (Button)findViewById(R.id.tc);
        tc.setText(scramble ? Integer.toString(keypad.getMatrix().get(2).getValue()) : "3");
        td = (Button)findViewById(R.id.td);
        td.setText(scramble ? Integer.toString(keypad.getMatrix().get(3).getValue()) : "4");
        te = (Button)findViewById(R.id.te);
        te.setText(scramble ? Integer.toString(keypad.getMatrix().get(4).getValue()) : "5");
        tf = (Button)findViewById(R.id.tf);
        tf.setText(scramble ? Integer.toString(keypad.getMatrix().get(5).getValue()) : "6");
        tg = (Button)findViewById(R.id.tg);
        tg.setText(scramble ? Integer.toString(keypad.getMatrix().get(6).getValue()) : "7");
        th = (Button)findViewById(R.id.th);
        th.setText(scramble ? Integer.toString(keypad.getMatrix().get(7).getValue()) : "8");
        ti = (Button)findViewById(R.id.ti);
        ti.setText(scramble ? Integer.toString(keypad.getMatrix().get(8).getValue()) : "9");
        tj = (Button)findViewById(R.id.tj);
        tj.setText(scramble ? Integer.toString(keypad.getMatrix().get(9).getValue()) : "0");
        tsend = (ImageButton)findViewById(R.id.tsend);
        tback = (ImageButton)findViewById(R.id.tback);

        tsend.setVisibility(View.INVISIBLE);
        tsend.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if(create && userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
                    Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra("confirm", true);
                    intent.putExtra("create", false);
                    intent.putExtra("first", userInput.toString());
                    startActivity(intent);
                }
                else if(confirm && userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {

                    if(userInput.toString().equals(strConfirm)) {

                        progress = new ProgressDialog(PinEntryActivity.this);
                        progress.setCancelable(false);
                        progress.setTitle(R.string.app_name);
                        progress.setMessage(getString(R.string.entering_stealth));
                        progress.show();

                        init(userInput.toString());

                    }
                    else {
                        Intent intent = new Intent(PinEntryActivity.this, PinEntryActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("create", true);
                        startActivity(intent);
                    }

                }
                else if(userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {

                    validateThread(userInput.toString());

                }
                else {
                    ;
                }

            }
        });

        tback.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                
                if(userInput.toString().length() > 0) {
                    userInput.deleteCharAt(userInput.length() - 1);
                    if(PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.HAPTIC_PIN, false) == true)    {
                        vibrator.vibrate(55);
                    }
                }
                displayUserInput();

            }
        });

        tback.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(userInput.toString().length() > 0) {
                    userInput.setLength(0);
                    if(PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.HAPTIC_PIN, false) == true)    {
                        vibrator.vibrate(55);
                    }
                }
                displayUserInput();
                return false;
            }
        });

    }

    public void OnNumberPadClick(View view) {
        if(PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.HAPTIC_PIN, false) == true)    {
            vibrator.vibrate(55);
        }
        userInput.append(((Button) view).getText().toString());
        displayUserInput();
    }

    private void displayUserInput() {

        tvUserInput.setText("");

        for(int i = 0; i < userInput.toString().length(); i++) {
            tvUserInput.append("*");
        }

        if(userInput.toString().length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
            tsend.setVisibility(View.VISIBLE);
        }
        else {
            tsend.setVisibility(View.INVISIBLE);
        }

    }

    private void init(final String pin) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] b = digest.digest(pin.getBytes("UTF-8"));
            Hash hash = new Hash(b);
            PrefsUtil.getInstance(PinEntryActivity.this).setValue(PrefsUtil.PIN_HASH, hash.toString());
            validateThread(pin);
        }
        catch(NoSuchAlgorithmException | UnsupportedEncodingException e) {
            Toast.makeText(PinEntryActivity.this, R.string.error_activating_stealth, Toast.LENGTH_SHORT).show();
        }

    }

    private void validateThread(final String pin) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Looper.prepare();

                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] b = digest.digest(pin.getBytes("UTF-8"));
                    Hash hash = new Hash(b);

                    if(PrefsUtil.getInstance(PinEntryActivity.this).getValue(PrefsUtil.PIN_HASH, "").equals(hash.toString())) {

                        TimeOutUtil.getInstance().updatePin();

                        Intent i = new Intent(PinEntryActivity.this, MainActivity2.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        i.putExtra("verified", true);
                        PinEntryActivity.this.startActivity(i);

                    }
                    else    {
                        Toast.makeText(PinEntryActivity.this, R.string.pin_error, Toast.LENGTH_SHORT).show();
                        AppUtil.getInstance(PinEntryActivity.this).restartApp();
                    }
                }
                catch(NoSuchAlgorithmException | UnsupportedEncodingException e) {
                    Toast.makeText(PinEntryActivity.this, R.string.pin_error, Toast.LENGTH_SHORT).show();
                    AppUtil.getInstance(PinEntryActivity.this).restartApp();
                }

                Looper.loop();

            }
        }).start();

    }

}
