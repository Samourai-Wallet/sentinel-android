package com.samourai.sentinel.util;

public class BlockExplorerUtil {

    private static CharSequence[] blockExplorers = { "Smartbit", "UASF Explorer" };
    private static CharSequence[] blockExplorerUrls = { "https://www.smartbit.com.au/tx/", "https://uasf-explorer.satoshiportal.com/tx/" };

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
