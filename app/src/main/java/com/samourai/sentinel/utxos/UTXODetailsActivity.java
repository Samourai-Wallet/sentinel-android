package com.samourai.sentinel.utxos;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.samourai.sentinel.R;
import com.samourai.sentinel.SamouraiSentinel;
import com.samourai.sentinel.api.APIFactory;
import com.samourai.sentinel.sweep.MyTransactionOutPoint;
import com.samourai.sentinel.sweep.SendFactory;
import com.samourai.sentinel.sweep.UTXO;
import com.samourai.sentinel.util.BlockedUTXO;
import com.samourai.sentinel.util.FormatsUtil;
import com.samourai.sentinel.util.LogUtil;
import com.samourai.sentinel.util.PreSelectUtil;
import com.samourai.sentinel.util.UTXOUtil;
import com.samourai.sentinel.utxos.models.UTXOCoin;
import com.squareup.picasso.Picasso;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import io.reactivex.disposables.CompositeDisposable;

public class UTXODetailsActivity extends AppCompatActivity {
    final DecimalFormat df = new DecimalFormat("#");
    private String hash, addr, hashIdx;
    private TextView addressTextView, amountTextView, statusTextView, notesTextView, hashTextView;
    private EditText noteEditText;
    private LinearLayout paynymLayout;
    private ImageView deleteButton;
    private TextView addNote;
    private static final String TAG = "UTXODetailsActivity";
    private int idx;
    private long amount;
    private UTXOCoin utxoCoin;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utxodetails);

        setSupportActionBar(findViewById(R.id.toolbar_utxo_activity));

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        addressTextView = findViewById(R.id.utxo_details_address);
        amountTextView = findViewById(R.id.utxo_details_amount);
        statusTextView = findViewById(R.id.utxo_details_spendable_status);
        hashTextView = findViewById(R.id.utxo_details_hash);
        addNote = findViewById(R.id.add_note_button);
        notesTextView = findViewById(R.id.utxo_details_note);
        deleteButton = findViewById(R.id.delete_note);

        df.setMinimumIntegerDigits(1);
        df.setMinimumFractionDigits(8);
        df.setMaximumFractionDigits(8);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey("hashIdx")) {
            hashIdx = getIntent().getExtras().getString("hashIdx");
        } else {
            finish();
        }


        List<UTXO> utxos = new ArrayList<>();

        utxos.addAll(APIFactory.getInstance(getApplicationContext()).getUtxos());
        for (UTXO utxo : utxos) {
            for (MyTransactionOutPoint outpoint : utxo.getOutpoints()) {
                if (outpoint.getTxHash() != null) {
                    String hashWithIdx = outpoint.getTxHash().toString().concat("-").concat(String.valueOf(outpoint.getTxOutputN()));
                    if (hashWithIdx.equals(hashIdx)) {
                        idx = outpoint.getTxOutputN();
                        amount = outpoint.getValue().longValue();
                        hash = outpoint.getTxHash().toString();
                        addr = outpoint.getAddress();
                        utxoCoin = new UTXOCoin(outpoint, utxo);
                        if (BlockedUTXO.getInstance().contains(outpoint.getTxHash().toString(), outpoint.getTxOutputN())) {
                            utxoCoin.doNotSpend = true;
                        }
                        setUTXOState();
                    }
                }

            }

        }
        deleteButton.setOnClickListener(view -> {
            if (UTXOUtil.getInstance().getNote(hash) != null) {
                UTXOUtil.getInstance().removeNote(hash);
            }
            setNoteState();
            saveWalletState();
        });
        addNote.setOnClickListener(view -> {
            View dialogView = getLayoutInflater().inflate(R.layout.bottom_sheet_note, null);
            BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.bottom_sheet_note);
            dialog.setContentView(dialogView);
            dialog.show();
            Button submitButton = dialog.findViewById(R.id.submit_note);

            if (UTXOUtil.getInstance().getNote(hash) != null) {
                ((EditText) dialog.findViewById(R.id.utxo_details_note)).setText(UTXOUtil.getInstance().getNote(hash));
                submitButton.setText("Save");
            } else {
                submitButton.setText("Add");
            }

            dialog.findViewById(R.id.submit_note).setOnClickListener((View view1) -> {
                dialog.dismiss();
                addNote(((EditText) dialog.findViewById(R.id.utxo_details_note)).getText().toString());
            });
        });

        setNoteState();

        addressTextView.setOnClickListener((event) -> new AlertDialog.Builder(UTXODetailsActivity.this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) UTXODetailsActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = null;
                    clip = android.content.ClipData.newPlainText("address", addr);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(UTXODetailsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                }).setNegativeButton(R.string.no, (dialog, whichButton) -> {
                }).show());

        hashTextView.setOnClickListener(view -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.txid_to_clipboard)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) UTXODetailsActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip;
                        clip = android.content.ClipData.newPlainText("tx id", hash);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                        }
                        Toast.makeText(UTXODetailsActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }).setNegativeButton(R.string.no, (dialog, whichButton) -> {
            }).show();
        });

    }

    void setUTXOState() {
        if (isBlocked()) {
            statusTextView.setText("Blocked");
        } else {
            statusTextView.setText(getText(R.string.spendable));
        }
        addressTextView.setText(addr);
        hashTextView.setText(hash);
        amountTextView.setText(df.format(((double) (amount) / 1e8)) + " BTC");
    }

    boolean isBlocked() {
        return BlockedUTXO.getInstance().contains(hash, idx);
    }

    void setNoteState() {
        TransitionManager.beginDelayedTransition((ViewGroup) notesTextView.getRootView());
        if (UTXOUtil.getInstance().getNote(hash) == null) {
            notesTextView.setVisibility(View.GONE);
            addNote.setText("Add");
            deleteButton.setVisibility(View.GONE);
        } else {
            notesTextView.setVisibility(View.VISIBLE);
            notesTextView.setText(UTXOUtil.getInstance().getNote(hash));
            deleteButton.setVisibility(View.VISIBLE);
            addNote.setText("Edit");
        }
    }

    void setSpendStatus() {

        final String[] export_methods = new String[2];
        export_methods[0] = "Spendable";
        export_methods[1] = "Do not spend";


        int selected = 0;
        if (isBlocked()) {
            selected = 1;
        }

        new AlertDialog.Builder(this)
                .setTitle("Set status")
                .setSingleChoiceItems(export_methods, selected, (dialog, which) -> {

                            if (which == 0) {
                                if (amount < BlockedUTXO.BLOCKED_UTXO_THRESHOLD && BlockedUTXO.getInstance().contains(hash, idx)) {
                                    BlockedUTXO.getInstance().remove(hash, idx);
                                    BlockedUTXO.getInstance().addNotDusted(hash, idx);

                                } else if (BlockedUTXO.getInstance().contains(hash, idx)) {

                                    BlockedUTXO.getInstance().remove(hash, idx);

                                }
                                utxoCoin.doNotSpend = false;

                            } else {

                                BlockedUTXO.getInstance().add(hash, idx, amount);

                                utxoCoin.doNotSpend = true;
                                LogUtil.debug("UTXOActivity", "added:" + hash + "-" + idx);

                            }
                            setUTXOState();
                            dialog.dismiss();
                            saveWalletState();
                        }
                ).

                show();

    }

    void addNote(String text) {
        if (text != null && text.length() > 0) {
            UTXOUtil.getInstance().addNote(hash, text);
        } else {
            UTXOUtil.getInstance().removeNote(hash);
        }
        setNoteState();
        saveWalletState();
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
        setResult(RESULT_OK, new Intent());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.utxo_details_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.utxo_details_menu_action_more_options) {
            showMoreOptions();
        }
        if (item.getItemId() == R.id.utxo_details_view_in_explorer) {
            viewInExplorer();
        }

        return super.onOptionsItemSelected(item);
    }


    private void showMoreOptions() {

        View dialogView = getLayoutInflater().inflate(R.layout.utxo_details_options_bottomsheet, null);
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(dialogView);
        dialog.show();
        TextView spendOption = dialog.findViewById(R.id.utxo_details_spending_status);


        if (isBlocked()) {
            if (spendOption != null)
                spendOption.setText(R.string.this_utxo_is_marked_as_blocked);
        } else {
            if (spendOption != null)
                spendOption.setText(R.string.this_utxo_is_marked_as_spendable);
        }


        dialog.findViewById(R.id.utxo_details_option_status).setOnClickListener(view -> {
            setSpendStatus();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.utxo_details_option_spend)
                .setOnClickListener(view -> {
                    dialog.dismiss();
                    ArrayList<UTXOCoin> list = new ArrayList<>();
                    list.add(utxoCoin);
                    String id = UUID.randomUUID().toString();
                    PreSelectUtil.getInstance().clear();
                    PreSelectUtil.getInstance().add(id, list);
                    if (utxoCoin.doNotSpend) {
                        Snackbar.make(paynymLayout.getRootView(), R.string.this_utxo_is_marked_as_blocked, Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    if (id != null) {
//                        Intent intent = new Intent(getApplicationContext(), SendActivity.class);
//                        intent.putExtra("preselected", id);
//                        intent.putExtra("_account", account);
//                        startActivity(intent);
                    }
                });

    }


    private void saveWalletState() {
//        Disposable disposable = Completable.fromCallable(() -> {
//            try {
//                PayloadUtil.getInstance(getApplicationContext()).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(getApplicationContext()).getGUID() + AccessFactory.getInstance().getPIN()));
//            } catch (MnemonicException.MnemonicLengthException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (JSONException e) {
//                e.printStackTrace();
//            } catch (DecryptionException e) {
//                e.printStackTrace();
//            }
//            return true;
//        })
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe();
//        compositeDisposable.add(disposable);
    }

    private void viewInExplorer() {
        String blockExplorer = "https://m.oxt.me/transaction/";
        if (SamouraiSentinel.getInstance().isTestNet()) {
            blockExplorer = "https://blockstream.info/testnet/";
        }
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(blockExplorer + hash));
        startActivity(browserIntent);
    }

}
