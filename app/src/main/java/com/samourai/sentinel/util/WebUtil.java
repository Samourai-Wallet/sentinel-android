package com.samourai.sentinel.util;

import android.content.Context;
import android.util.Log;

import com.samourai.sentinel.BuildConfig;
import com.samourai.sentinel.SamouraiSentinel;
import com.samourai.sentinel.tor.TorManager;

import org.json.JSONObject;

import java.sql.Time;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class WebUtil {


    private static WebUtil instance = null;
    private Context context = null;


    private WebUtil(Context context) {
        this.context = context;
    }

    public static WebUtil getInstance(Context ctx) {

        if (instance == null) {
            instance = new WebUtil(ctx);
        }

        return instance;
    }


    public static final String SAMOURAI_API = "https://api.samouraiwallet.com/";
    public static final String SAMOURAI_API_CHECK = "https://api.samourai.com/v1/status";
    public static final String SAMOURAI_API2 = "https://api.samouraiwallet.com/v2/";
    public static final String SAMOURAI_API2_TESTNET = "https://api.samouraiwallet.com/test/v2/";

    public static final String SAMOURAI_API2_TOR_DIST = "http://d2oagweysnavqgcfsfawqwql2rwxend7xxpriq676lzsmtfwbt75qbqd.onion/v2/";
    public static final String SAMOURAI_API2_TESTNET_TOR_DIST = "http://d2oagweysnavqgcfsfawqwql2rwxend7xxpriq676lzsmtfwbt75qbqd.onion/test/v2/";

    public static String SAMOURAI_API2_TOR = SAMOURAI_API2_TOR_DIST;
    public static String SAMOURAI_API2_TESTNET_TOR = SAMOURAI_API2_TESTNET_TOR_DIST;

    public static final String LBC_EXCHANGE_URL = "https://localbitcoins.com/bitcoinaverage/ticker-all-currencies/";
    public static final String BFX_EXCHANGE_URL = "https://api.bitfinex.com/v1/pubticker/btcusd";

    private static final int DefaultRequestRetry = 2;
    private static final int DefaultRequestTimeout = 60000;

    public String postURL(String URL, String urlParameters) throws Exception {

        if ( PrefsUtil.getInstance(context).getValue(PrefsUtil.ENABLE_TOR, false)) {
            return tor_postURL(URL,urlParameters);
        }


        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, urlParameters.toString());

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }

        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();

        try (Response response = builder.build().newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string();

        }
    }

    public String postURL(String URL, FormBody args) throws Exception {



        if ( PrefsUtil.getInstance(context).getValue(PrefsUtil.ENABLE_TOR, false)) {
            return tor_postURL(URL,args);
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }

        Request request = new Request.Builder()
                .url(URL)
                .post(args)
                .build();

        try (Response response = builder.build().newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string();

        }
    }


    public String getURL(String URL) throws Exception {

        if ( PrefsUtil.getInstance(context).getValue(PrefsUtil.ENABLE_TOR, false)) {
            return tor_getURL(URL);
        }

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(90, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .readTimeout(90, TimeUnit.SECONDS);

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }

        Request request = new Request.Builder()
                .url(URL)
                .build();

        try (Response response = builder.build().newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string();

        }

    }


    private String tor_getURL(String URL) throws Exception {


        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(TorManager.getInstance(this.context).getProxy())
                .connectTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS);

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }
        if (URL.contains("onion")) {
            getHostNameVerifier(builder);
        }

        Request request = new Request.Builder()
                .url(URL)
                .build();

        try (Response response = builder.build().newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string();

        }

    }

    public String tor_postURL(String URL, String args) throws Exception {


        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, args.toString());


        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(TorManager.getInstance(this.context).getProxy())
                .connectTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS);

        if (URL.contains("onion")) {
            getHostNameVerifier(builder);
            builder.connectTimeout(90, TimeUnit.SECONDS);
        }

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
        }

        Request request = new Request.Builder()
                .url(URL)
                .post(body)
                .build();

        try (Response response = builder.build().newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string();

        }

    }

    public String tor_postURL(String URL, FormBody args) throws Exception {


        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxy(TorManager.getInstance(this.context).getProxy());

        if (URL.contains("onion")) {
            getHostNameVerifier(builder);
            builder.connectTimeout(90, TimeUnit.SECONDS);
        }

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
            builder.connectTimeout(90, TimeUnit.SECONDS);
        }

        Request request = new Request.Builder()
                .url(URL)
                .post(args)
                .build();

        try (Response response = builder.build().newCall(request).execute()) {
            if (response.body() == null) {
                return "";
            }
            return response.body().string();

        }

    }


    private void getHostNameVerifier(OkHttpClient.Builder
                                             clientBuilder) throws
            Exception {

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();


        clientBuilder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        clientBuilder.hostnameVerifier((hostname, session) -> true);

    }

    public static String getAPIUrl(Context context) {
        if (TorManager.getInstance(context).isRequired()) {
            return SamouraiSentinel.getInstance().isTestNet() ? SAMOURAI_API2_TESTNET_TOR : SAMOURAI_API2_TOR;
        } else {
            return SamouraiSentinel.getInstance().isTestNet() ? SAMOURAI_API2_TESTNET : SAMOURAI_API2;
        }

    }

}
