package com.samourai.sentinel.network.dojo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.sentinel.R;
import com.samourai.sentinel.service.WebSocketService;
import com.samourai.sentinel.tor.TorManager;
import com.samourai.sentinel.tor.TorService;
import com.samourai.sentinel.util.AppUtil;
import com.samourai.sentinel.util.ConnectivityStatus;
import com.samourai.sentinel.util.PrefsUtil;

import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class Network extends AppCompatActivity {


    TextView torRenewBtn;
    TextView torConnectionStatus;
    Button torButton;
    Button dojoButton;
    ImageView torConnectionIcon;
    int activeColor, disabledColor, waiting;
    CompositeDisposable disposables = new CompositeDisposable();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);
//        Objects.requireNonNull(getActionBar()).setDisplayHomeAsUpEnabled(true);

        setSupportActionBar(findViewById(R.id.toolbar));
        if( getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        activeColor = ContextCompat.getColor(this, R.color.green_ui_2);
        disabledColor = ContextCompat.getColor(this, R.color.disabledRed);
        waiting = ContextCompat.getColor(this, R.color.warning_yellow);

        torButton = findViewById(R.id.networking_tor_btn);
        torRenewBtn = findViewById(R.id.networking_tor_renew);
        dojoButton = findViewById(R.id.networking_dojo_btn);

        torConnectionIcon = findViewById(R.id.network_tor_status_icon);
        torConnectionStatus = findViewById(R.id.network_tor_status);


        torRenewBtn.setOnClickListener(view -> {
            if (TorManager.getInstance(getApplicationContext()).isConnected()) {
                startService(new Intent(this, TorService.class).setAction(TorService.RENEW_IDENTITY));
            }
        });

        dojoButton.setOnClickListener(view -> {
            DojoConfigureBottomSheet dojoConfigureBottomSheet = new DojoConfigureBottomSheet();
            dojoConfigureBottomSheet.show(getSupportFragmentManager(), dojoConfigureBottomSheet.getTag());

        });

        listenToTorStatus();
//
        torButton.setOnClickListener(view -> {

            if (TorManager.getInstance(getApplicationContext()).isRequired()) {
//                if(DojoUtil.getInstance(NetworkDashboard.this).getDojoParams() !=null ){
//                    Toast.makeText(this,R.string.cannot_disable_tor_dojo,Toast.LENGTH_LONG).show();
//                    return;
//                }

                stopTor();
                PrefsUtil.getInstance(getApplicationContext()).setValue(PrefsUtil.ENABLE_TOR, false);
            } else {
                if (AppUtil.getInstance(this.getApplicationContext()).isServiceRunning(WebSocketService.class)) {
                    stopService(new Intent(this.getApplicationContext(), WebSocketService.class));
                }
                startTor();
                PrefsUtil.getInstance(getApplicationContext()).setValue(PrefsUtil.ENABLE_TOR, true);
            }
        });



    }

    private void stopTor() {
        Intent startIntent = new Intent(getApplicationContext(), TorService.class);
        startIntent.setAction(TorService.STOP_SERVICE);
        startService(startIntent);
    }

    private void listenToTorStatus() {
        Disposable disposable = TorManager.getInstance(getApplicationContext())
                .torStatus
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::setTorConnectionState);

        disposables.add(disposable);

        this.runOnUiThread(() -> setTorConnectionState(TorManager.getInstance(getApplicationContext()).isConnected() ? TorManager.CONNECTION_STATES.CONNECTED : TorManager.CONNECTION_STATES.DISCONNECTED));
    }

    private void setTorConnectionState(TorManager.CONNECTION_STATES enabled) {
        this.runOnUiThread(() -> {
            if (enabled == TorManager.CONNECTION_STATES.CONNECTED) {
                torButton.setText("Disable");
                torButton.setEnabled(true);
                torConnectionIcon.setColorFilter(activeColor);
                torConnectionStatus.setText("Enabled");
                torRenewBtn.setVisibility(View.VISIBLE);
//                if(waitingForPairing)    {
//                    waitingForPairing = false;
//
//                    if (strPairingParams != null) {
//                        DojoUtil.getInstance(NetworkDashboard.this).setDojoParams(strPairingParams);
//                        Toast.makeText(NetworkDashboard.this, "Tor enabled for Dojo pairing:" + DojoUtil.getInstance(this).getDojoParams(), Toast.LENGTH_SHORT).show();
//                        initDojo();
//                    }
//
//                }

            } else if (enabled == TorManager.CONNECTION_STATES.CONNECTING) {
                torRenewBtn.setVisibility(View.INVISIBLE);
                torButton.setText("loading...");
                torButton.setEnabled(false);
                torConnectionIcon.setColorFilter(waiting);
                torConnectionStatus.setText("Tor initializing");
            } else {
                torRenewBtn.setVisibility(View.INVISIBLE);
                torButton.setText("Enable");
                torButton.setEnabled(true);
                torConnectionIcon.setColorFilter(disabledColor);
                torConnectionStatus.setText("Disabled");

            }
        });

    }


    private void startTor() {
        if (ConnectivityStatus.hasConnectivity(getApplicationContext())) {

            Intent startIntent = new Intent(getApplicationContext(), TorService.class);
            startIntent.setAction(TorService.START_SERVICE);
            startService(startIntent);

        } else {
            Toast.makeText(getApplicationContext(), R.string.in_offline_mode, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        disposables.dispose();
        super.onDestroy();
    }

//    @Override
//    public boolean onMenuItemSelected(int featureId, MenuItem item) {
//        if (item.getItemId() == android.R.id.home) {
//            finish();
//        }
//        return super.onMenuItemSelected(featureId, item);
//    }
}
