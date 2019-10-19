package com.samourai.sentinel;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.samourai.sentinel.api.APIFactory;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.WebUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;

public class InsertSegwitActivity extends Activity {

    private ProgressDialog progress = null;
    private Handler handler = null;
    private SegwitTask segwitTask = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String xpub = null;
        String label = null;
        String purpose = null;

        Bundle extras = getIntent().getExtras();
        if(extras != null && extras.containsKey("xpub"))	{
            xpub = extras.getString("xpub");
            label = extras.getString("label");
            purpose = extras.getString("purpose");
        }

        segwitTask = new SegwitTask();
        segwitTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, new String[] { xpub, purpose, label });

    }

    @Override
    public void onResume() {
        super.onResume();

        AppUtil.getInstance(InsertSegwitActivity.this).checkTimeOut();

    }

    private class SegwitTask extends AsyncTask<String, Void, String> {

        private Handler handler = null;

        @Override
        protected void onPreExecute() {
            handler = new Handler();

            progress = new ProgressDialog(InsertSegwitActivity.this);
            progress.setCancelable(false);
            progress.setTitle(R.string.app_name);
            progress.setMessage(getString(R.string.please_wait));
            progress.show();
        }

        @Override
        protected String doInBackground(String... params) {

            String response = null;
            try {

                FormBody body    = new FormBody.Builder()
                        .add("xpub", params[0])
                        .add("type", "restore")
                        .add("segwit", "bip".concat(params[1]))
                        .add("at", APIFactory.getInstance(getApplicationContext()).getAccessToken())
                        .build();


                Log.d("InsertSegwitActivity", "Segwit:" + body.toString());
                response = WebUtil.getInstance(getApplicationContext()).postURL(WebUtil.getAPIUrl(getApplicationContext()) + "xpub/", body);

                Log.d("InsertSegwitActivity", "Segwit:" + response);

                JSONObject obj = new JSONObject(response);
                if(obj != null && obj.has("status") && obj.getString("status").equals("ok"))    {
                    if(progress != null && progress.isShowing())    {
                        progress.dismiss();
                        progress = null;
                    }

                    if(params[1].equals("84"))    {
                        SamouraiSentinel.getInstance(InsertSegwitActivity.this).getBIP84().put(params[0], params[2]);
                    }
                    else    {
                        SamouraiSentinel.getInstance(InsertSegwitActivity.this).getBIP49().put(params[0], params[2]);
                    }

                    try {
                        SamouraiSentinel.getInstance(InsertSegwitActivity.this).serialize(SamouraiSentinel.getInstance(InsertSegwitActivity.this).toJSON(), null);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    } catch (JSONException je) {
                        je.printStackTrace();
                    }

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("xpub", params[0]);
                    resultIntent.putExtra("purpose", params[1]);
                    resultIntent.putExtra("label", params[2]);
                    setResult(Activity.RESULT_OK, resultIntent);
                    InsertSegwitActivity.this.finish();

                }
                else    {
                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_CANCELED, resultIntent);
                    InsertSegwitActivity.this.finish();
                }

            }
            catch(Exception e) {
                e.printStackTrace();
                Intent resultIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, resultIntent);
                InsertSegwitActivity.this.finish();
            }
            finally {
                ;
            }

            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            if(progress != null && progress.isShowing())    {
                progress.dismiss();
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            ;
        }

    }

}
