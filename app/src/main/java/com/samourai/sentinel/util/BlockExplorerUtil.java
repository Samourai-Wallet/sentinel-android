package com.samourai.sentinel.util;

public class BlockExplorerUtil {

    private static CharSequence[] blockExplorers = { "Smartbit", "Blockchain Reader (Yogh)", "OXT" };
    private static CharSequence[] blockExplorerUrls = { "https://www.smartbit.com.au/tx/", "http://srv1.yogh.io/#tx:id:", "https://m.oxt.me/transaction/" };

    private static BlockExplorerUtil instance = null;

    private BlockExplorerUtil() { ; }

    public static BlockExplorerUtil getInstance() {

        if(instance == null) {
            instance = new BlockExplorerUtil();
        }

        return instance;
    }

    public CharSequence[] getBlockExplorers() {
        return blockExplorers;
    }

    public CharSequence[] getBlockExplorerUrls() {
        return blockExplorerUrls;
    }

}
