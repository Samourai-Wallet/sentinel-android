package com.samourai.sentinel.util;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.Toast;

import com.samourai.sentinel.MainActivity2;
import com.samourai.sentinel.R;
import com.samourai.sentinel.SamouraiSentinel;
import com.samourai.sentinel.crypto.AESUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AppUtil {

    private ProgressDialog progress = null;

    public static final int MIN_BACKUP_PW_LENGTH = 6;
    public static final int MAX_BACKUP_PW_LENGTH = 255;

    private boolean isInForeground = false;

    private static AppUtil instance = null;
    private static Context context = null;

    private static String strReceiveQRFilename = null;

    private static boolean isOfflineMode = false;


    private AppUtil() {
        ;
    }

    public static AppUtil getInstance(Context ctx) {

        context = ctx;

        if (instance == null) {
            strReceiveQRFilename = context.getExternalCacheDir() + File.separator + "qr.png";
            instance = new AppUtil();
        }

        return instance;
    }

    public boolean isOfflineMode() {

        isOfflineMode = !ConnectivityStatus.hasConnectivity(context);

        return isOfflineMode;
    }


    public void restartApp() {
        Intent intent = new Intent(context, MainActivity2.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void restartApp(boolean verified) {
        Intent intent = new Intent(context, MainActivity2.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("verified", verified);
        context.startActivity(intent);
    }

    public void setIsInForeground(boolean foreground) {
        isInForeground = foreground;
    }

    public boolean isServiceRunning(Class<?> serviceClass) {

        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d("AppUtil", "service class name:" + serviceClass.getName() + " is running");
                return true;
            }
        }

        Log.d("AppUtil", "service class name:" + serviceClass.getName() + " is not running");
        return false;
    }

    public String getReceiveQRFilename(){
        return strReceiveQRFilename;
    }

    public void deleteQR(){
        String strFileName = strReceiveQRFilename;
        File file = new File(strFileName);
        if(file.exists()) {
            file.delete();
        }
    }

    public void doBackup() {

        final EditText password = new EditText(context);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setHint(R.string.password);

        new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(R.string.options_export2)
                .setView(password)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String pw = password.getText().toString();
                        if (pw != null && pw.length() >= AppUtil.MIN_BACKUP_PW_LENGTH && pw.length() <= AppUtil.MAX_BACKUP_PW_LENGTH) {

                            final EditText password2 = new EditText(context);
                            password2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            password2.setHint(R.string.confirm_password);

                            new AlertDialog.Builder(context)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.options_export2)
                                    .setView(password2)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            final String pw2 = password2.getText().toString();

                                            if (pw2 != null && pw2.equals(pw)) {

                                                final String[] export_methods = new String[2];
                                                export_methods[0] = context.getString(R.string.export_to_clipboard);
                                                export_methods[1] = context.getString(R.string.export_to_email);

                                                new AlertDialog.Builder(context)
                                                        .setTitle(R.string.options_export)
                                                        .setSingleChoiceItems(export_methods, 0, new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, int which) {

                                                                        String encrypted = null;
                                                                        try {
                                                                            encrypted = AESUtil.encrypt(SamouraiSentinel.getInstance(context).toJSON().toString(), new CharSequenceX(pw), AESUtil.DefaultPBKDF2Iterations);
                                                                        } catch (Exception e) {
                                                                            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                        } finally {
                                                                            if (encrypted == null) {
                                                                                Toast.makeText(context, R.string.encryption_error, Toast.LENGTH_SHORT).show();
                                                                                return;
                                                                            }
                                                                            else    {

                                                                                try {
                                                                                    JSONObject jsonObj = new JSONObject();
                                                                                    jsonObj.put("version", 1);
                                                                                    jsonObj.put("payload", encrypted);
                                                                                    encrypted = jsonObj.toString();
                                                                                }
                                                                                catch(JSONException je) {
                                                                                    ;
                                                                                }

                                                                            }

                                                                        }

                                                                        if (which == 0) {
                                                                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                                                            android.content.ClipData clip = null;
                                                                            clip = android.content.ClipData.newPlainText("Wallet backup", encrypted);
                                                                            clipboard.setPrimaryClip(clip);
                                                                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                                                        } else {
                                                                            Intent email = new Intent(Intent.ACTION_SEND);
                                                                            email.putExtra(Intent.EXTRA_SUBJECT, "Sentinel backup");
                                                                            email.putExtra(Intent.EXTRA_TEXT, encrypted);
                                                                            email.setType("message/rfc822");
                                                                            context.startActivity(Intent.createChooser(email, context.getText(R.string.choose_email_client)));
                                                                        }

                                                                        dialog.dismiss();
                                                                    }
                                                                }
                                                        ).show();

                                            } else {
                                                Toast.makeText(context, R.string.error_password, Toast.LENGTH_SHORT).show();
                                            }

                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            ;
                                        }
                                    }).show();

                        } else {
                            Toast.makeText(context, R.string.invalid_password, Toast.LENGTH_SHORT).show();
                        }

                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();

    }

    public void doRestore() {

        final EditText password = new EditText(context);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setHint(R.string.password);

        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setView(password)
                .setMessage(R.string.restore_wallet_from_backup)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        final String pw = password.getText().toString();
                        if (pw == null || pw.length() < 1) {
                            Toast.makeText(context, R.string.invalid_password, Toast.LENGTH_SHORT).show();
                            AppUtil.getInstance(context).restartApp();
                        }

                        final EditText edBackup = new EditText(context);
                        edBackup.setSingleLine(false);
                        edBackup.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                        edBackup.setLines(5);
                        edBackup.setHint(R.string.encrypted_backup);
                        edBackup.setGravity(Gravity.START);

                        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                                .setTitle(R.string.app_name)
                                .setView(edBackup)
                                .setMessage(R.string.restore_wallet_from_backup)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        String encrypted = edBackup.getText().toString();
                                        if (encrypted == null || encrypted.length() < 1) {
                                            Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                            AppUtil.getInstance(context).restartApp();
                                        }

                                        try {
                                            JSONObject jsonObject = new JSONObject(encrypted);
                                            if(jsonObject != null && jsonObject.has("payload"))    {
                                                encrypted = jsonObject.getString("payload");
                                            }
                                        }
                                        catch(JSONException je) {
                                            ;
                                        }

                                        String decrypted = null;
                                        try {
                                            decrypted = AESUtil.decrypt(encrypted, new CharSequenceX(pw), AESUtil.DefaultPBKDF2Iterations);
                                        } catch (Exception e) {
                                            Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                        } finally {
                                            if (decrypted == null || decrypted.length() < 1) {
                                                Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                AppUtil.getInstance(context).restartApp();
                                            }
                                        }

                                        final String decryptedPayload = decrypted;
                                        if (progress != null && progress.isShowing()) {
                                            progress.dismiss();
                                            progress = null;
                                        }

                                        progress = new ProgressDialog(context);
                                        progress.setCancelable(false);
                                        progress.setTitle(R.string.app_name);
                                        progress.setMessage(context.getString(R.string.please_wait));
                                        progress.show();

                                        new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Looper.prepare();

                                                try {

                                                    JSONObject json = new JSONObject(decryptedPayload);
                                                    if(json != null && (json.has("xpubs") || json.has("legacy")))    {
                                                        SamouraiSentinel.getInstance(context).parseJSON(json);

                                                        try {
                                                            SamouraiSentinel.getInstance(context).serialize(SamouraiSentinel.getInstance(context).toJSON(), null);
                                                        } catch (IOException ioe) {
                                                            ;
                                                        } catch (JSONException je) {
                                                            ;
                                                        }

                                                        AppUtil.getInstance(context).restartApp();
                                                    }

                                                }
                                                catch(JSONException je) {
                                                    je.printStackTrace();
                                                    Toast.makeText(context, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                }
                                                finally {
                                                    if (progress != null && progress.isShowing()) {
                                                        progress.dismiss();
                                                        progress = null;
                                                    }
                                                    AppUtil.getInstance(context).restartApp();
                                                }

                                                Looper.loop();

                                            }
                                        }).start();

                                    }
                                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        AppUtil.getInstance(context).restartApp();

                                    }
                                });

                        dlg.show();

                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        AppUtil.getInstance(context).restartApp();

                    }
                });

        dlg.show();

    }

    public void checkTimeOut()   {
        if(TimeOutUtil.getInstance().isTimedOut())    {
            AppUtil.getInstance(context).restartApp();
        }
        else    {
            TimeOutUtil.getInstance().updatePin();
        }
    }

    public boolean isSideLoaded() {
        List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));
        final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        return installer == null || !validInstallers.contains(installer);
    }

}
