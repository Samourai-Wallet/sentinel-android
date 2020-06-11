package com.samourai.sentinel.send;

import org.bouncycastle.util.encoders.Hex;

public class PSBTEntry    {

    public PSBTEntry() { ; }

    private byte[] key = null;
    private byte[] keyType = null;
    private byte[] keyData = null;
    private byte[] data = null;

    private int state = -1;

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public byte[] getKeyType() {
        return keyType;
    }

    public void setKeyType(byte[] keyType) {
        this.keyType = keyType;
    }

    public byte[] getKeyData() {
        return keyData;
    }

    public void setKeyData(byte[] keyData) {
        this.keyData = keyData;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("key:");
        if(key == null) {
            sb.append("null key");
            return sb.toString();
        }
        else {
            sb.append(Hex.toHexString(key));
        }
        sb.append(",");
        sb.append("keyType:");
        sb.append(Hex.toHexString(keyType));
        sb.append(",");
        sb.append("keyData:");
        if(keyData != null) {
            sb.append(Hex.toHexString(keyData));
        }
        else {
            sb.append("null");
        }
        sb.append(",");
        sb.append("data:");
        sb.append(Hex.toHexString(data));

        return sb.toString();
    }

}
