package com.samourai.sentinel.util;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.samourai.sentinel.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import com.samourai.sentinel.MainActivity2;

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
//        Intent intent = new Intent(context, MainActivity2.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent);
    }

    public void restartApp(boolean verified) {
//        Intent intent = new Intent(context, MainActivity2.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//        intent.putExtra("verified", verified);
//        context.startActivity(intent);
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

//                                                                        String encrypted = null;
//                                                                        try {
//                                                                            encrypted = AESUtil.encrypt(SamouraiSentinel.getInstance(context).toJSON().toString(), new CharSequenceX(pw), AESUtil.DefaultPBKDF2Iterations);
//                                                                        } catch (Exception e) {
//                                                                            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
//                                                                        } finally {
//                                                                            if (encrypted == null) {
//                                                                                Toast.makeText(context, R.string.encryption_error, Toast.LENGTH_SHORT).show();
//                                                                                return;
//                                                                            }
//                                                                            else    {
//
//                                                                                try {
//                                                                                    JSONObject jsonObj = new JSONObject();
//                                                                                    jsonObj.put("version", 1);
//                                                                                    jsonObj.put("payload", encrypted);
//                                                                                    encrypted = jsonObj.toString();
//                                                                                }
//                                                                                catch(JSONException je) {
//                                                                                    ;
//                                                                                }
//
//                                                                            }
//
//                                                                        }
//
//                                                                        if (which == 0) {
//                                                                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
//                                                                            android.content.ClipData clip = null;
//                                                                            clip = android.content.ClipData.newPlainText("Wallet backup", encrypted);
//                                                                            clipboard.setPrimaryClip(clip);
//                                                                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
//                                                                        } else {
//                                                                            Intent email = new Intent(Intent.ACTION_SEND);
//                                                                            email.putExtra(Intent.EXTRA_SUBJECT, "Sentinel backup");
//                                                                            email.putExtra(Intent.EXTRA_TEXT, encrypted);
//                                                                            email.setType("message/rfc822");
//                                                                            context.startActivity(Intent.createChooser(email, context.getText(R.string.choose_email_client)));
//                                                                        }

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


    public boolean isSideLoaded() {
        List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));
        final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        return installer == null || !validInstallers.contains(installer);
    }

}
