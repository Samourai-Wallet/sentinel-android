package com.samourai.sentinel.util;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

//import android.util.Log;

public class ExchangeRateFactory	{

    private static Context context = null;

    private static String strDataLBC = null;
    private static String strDataBFX = null;

    private static HashMap<String,Double> fxRatesLBC = null;
    private static HashMap<String,Double> fxRatesBFX = null;

    private static ExchangeRateFactory instance = null;

    private static String[] currencies = {
            "CNY", "EUR", "GBP", "RUB", "USD"
    };

    private static String[] currencyLabels = {
            "United States Dollar - USD",
            "Euro - EUR",
            "British Pound Sterling - GBP",
            "Chinese Yuan - CNY",
            "Russian Rouble - RUB"
    };

    private static String[] exchangeLabels = {
            "LocalBitcoins.com",
            "Bitfinex",
    };

    private ExchangeRateFactory()	 { ; }

    public static ExchangeRateFactory getInstance(Context ctx)	 {

        context = ctx;

        if(instance == null)	 {
            fxRatesLBC = new HashMap<String,Double>();
            fxRatesBFX = new HashMap<String,Double>();

            instance = new ExchangeRateFactory();
        }

        return instance;
    }

    public double getAvgPrice(String currency)	 {
        int fxSel = PrefsUtil.getInstance(context).getValue(PrefsUtil.CURRENT_EXCHANGE_SEL, 0);
        HashMap<String,Double> fxRates = null;

        if(fxSel == 1)	 {
            fxRates = fxRatesBFX;
        }
        else	 {
            fxRates = fxRatesLBC;
        }

        if(fxRates.get(currency) != null && fxRates.get(currency) > 0.0)	 {
            PrefsUtil.getInstance(context).setValue("CANNED_" + currency, Double.toString(fxRates.get(currency)));
            return fxRates.get(currency);
        }
        else	 {
            return Double.parseDouble(PrefsUtil.getInstance(context).getValue("CANNED_" + currency, "0.0"));
        }
    }

    public String[] getCurrencies()	 {
        return currencies;
    }

    public String[] getCurrencyLabels()	 {
        return currencyLabels;
    }

    public String[] getExchangeLabels()	 {
        return exchangeLabels;
    }

    public void setDataLBC(String data)	 {
        strDataLBC = data;
    }

    public void setDataBFX(String data)	 {
        strDataBFX = data;
    }

    public void parseLBC()	 {
        for(int i = 0; i < currencies.length; i++)	 {
            getLBC(currencies[i]);
        }
    }

    public void parseBFX()	 {
        for(int i = 0; i < currencies.length; i++)	 {
            if(currencies[i].equals("USD"))	 {
                getBFX("USD");
            }
            else	 {
                continue;
            }
        }
    }

    private void getLBC(String currency)	 {
        try {
            JSONObject jsonObject = new JSONObject(strDataLBC);
            if(jsonObject != null)	{
                JSONObject jsonCurr = jsonObject.getJSONObject(currency);
                if(jsonCurr != null)	{
                    double avg_price = 0.0;
                    if(jsonCurr.has("avg_12h"))	{
                        avg_price = jsonCurr.getDouble("avg_12h");
                    }
                    else if(jsonCurr.has("avg_24h"))	{
                        avg_price = jsonCurr.getDouble("avg_24h");
                    }
                    fxRatesLBC.put(currency, Double.valueOf(avg_price));
//                    Log.i("ExchangeRateFactory", "LBC:" + currency + " " + Double.valueOf(avg_price));
                }
            }
        } catch (JSONException je) {
            fxRatesLBC.put(currency, Double.valueOf(-1.0));
//            fxSymbols.put(currency, null);
        }
    }

    private void getBFX(String currency)	 {
        try {
            JSONObject jsonObject = new JSONObject(strDataBFX);
            if(jsonObject != null && jsonObject.has("last_price"))	{
                String strLastPrice = jsonObject.getString("last_price");
                double avg_price = Double.parseDouble(strLastPrice);
                fxRatesBFX.put(currency, Double.valueOf(avg_price));
//                    Log.i("ExchangeRateFactory", "BFX:" + currency + " " + Double.valueOf(avg_price));
            }
        }
        catch (JSONException je) {
            fxRatesBFX.put(currency, Double.valueOf(-1.0));
//            fxSymbols.put(currency, null);
        }
        catch (NumberFormatException nfe) {
            fxRatesBFX.put(currency, Double.valueOf(-1.0));
//            fxSymbols.put(currency, null);
        }
    }

}
