package com.samourai.sentinel.service;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.samourai.sentinel.SamouraiSentinel;
import com.samourai.sentinel.api.APIFactory;
import com.samourai.sentinel.hd.HD_Wallet;
import com.samourai.sentinel.hd.HD_WalletFactory;
import com.samourai.sentinel.util.AddressFactory;
import com.samourai.sentinel.util.LogUtil;

import org.bitcoinj.crypto.MnemonicException;

import java.io.IOException;
import java.util.List;

import static com.samourai.sentinel.balance.BalanceActivity.ACTION_INTENT_COMPLETE;

public class JobRefreshService extends JobIntentService {

    static final int JOB_ID = 111;
    private static final String TAG = "JobRefreshService";

    public static void enqueueWork(Context context, Intent intent) {
        JobRefreshService.enqueueWork(context, JobRefreshService.class, JOB_ID, intent);
    }


    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        APIFactory.getInstance(getApplicationContext()).stayingAlive();


        int idx = SamouraiSentinel.getInstance(getApplication()).getCurrentSelectedAccount();

        List<String> _xpubs = SamouraiSentinel.getInstance(getApplication()).getAllAddrsSorted();
        if (idx == 0) {
            APIFactory.getInstance(getApplication()).getXPUB(_xpubs.toArray(new String[_xpubs.size()]));
        } else {
            APIFactory.getInstance(getApplication()).getXPUB(new String[]{_xpubs.get(idx - 1)});
        }


        APIFactory.getInstance(getApplication()).getUnspentOutputs(_xpubs);

        try {
            if (HD_WalletFactory.getInstance(getApplication()).get() != null) {

                HD_Wallet hdw = HD_WalletFactory.getInstance(getApplication()).get();

                for (int i = 0; i < hdw.getAccounts().size(); i++) {
                    HD_WalletFactory.getInstance(getApplication()).get().getAccount(i).getReceive().setAddrIdx(AddressFactory.getInstance().getHighestTxReceiveIdx(i));
                }

            }
        } catch (IOException ioe) {
            ;
        } catch (MnemonicException.MnemonicLengthException mle) {
            ;
        }

        Intent _intent = new Intent(ACTION_INTENT_COMPLETE);
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(_intent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.debug(TAG, "JobRefreshService Destroy");
    }


}
