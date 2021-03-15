package com.samourai.sentinel.util;

import org.bitcoinj.core.Coin;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class MonetaryUtil {

    private static MonetaryUtil instance = null;
    private static NumberFormat btcFormat = null;
    private static NumberFormat fiatFormat = null;
    private static DecimalFormat decimalFormat = new DecimalFormat("0.########");

    private MonetaryUtil() {
        ;
    }

    public static MonetaryUtil getInstance() {

        if (instance == null) {
            fiatFormat = NumberFormat.getInstance(Locale.US);
            fiatFormat.setMaximumFractionDigits(2);
            fiatFormat.setMinimumFractionDigits(2);

            btcFormat = NumberFormat.getInstance(Locale.US);
            btcFormat.setMaximumFractionDigits(8);
            btcFormat.setMinimumFractionDigits(1);

            decimalFormat.setMinimumIntegerDigits(1);
            decimalFormat.setMaximumFractionDigits(8);
            decimalFormat.setMinimumFractionDigits(8);


            instance = new MonetaryUtil();
        }

        return instance;
    }

    public NumberFormat getBTCFormat() {
        return btcFormat;
    }

    public NumberFormat getFiatFormat(String fiat) {
        fiatFormat.setCurrency(Currency.getInstance(fiat));
        return fiatFormat;
    }

    public String getBTCUnits() {
        return "BTC";
    }

    public String formatToBtc(Long value) {
        return Coin.valueOf(value).toPlainString();
    }

    public  String getBTCDecimalFormat(Long sats) {
        return decimalFormat.format(sats / 1e8);
    }


}
