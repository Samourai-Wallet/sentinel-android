package com.samourai.sentinel.util;

public class BlockExplorerUtil {

    private static CharSequence[] blockExplorers = { "Blocktrail", "Blockchain", "Blockr.io", "BlockCypher", "Blockexplorer.com", "SoChain" };
    private static CharSequence[] blockExplorerUrls = { "https://www.blocktrail.com/BTC/tx/", "https://blockchain.info/tx/", "https://btc.blockr.io/tx/info/", "https://live.blockcypher.com/btc/tx/", "https://blockexplorer.com/tx/", "https://chain.so/tx/BTC/" };

    public static final int BLOCKTRAIL = 0;
    public static final int BLOCKCHAIN = 1;
    public static final int BLOCKCYPHER = 2;
    public static final int BLOCKR = 3;
    public static final int CLASSIC = 4;
    public static final int SOCHAIN = 5;

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
