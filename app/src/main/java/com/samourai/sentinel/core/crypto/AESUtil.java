package com.samourai.sentinel.crypto;

import com.samourai.sentinel.util.CharSequenceX;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.BlockCipherPadding;
import org.bouncycastle.crypto.paddings.ISO10126d2Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.apache.commons.lang.ArrayUtils;


public class AESUtil	{

//    private static Logger mLogger = LoggerFactory.getLogger(AESUtil.class);

    public static final int DefaultPBKDF2Iterations = 5000;

    private static byte[] copyOfRange(byte[] source, int from, int to) {
        byte[] range = new byte[to - from];
        System.arraycopy(source, from, range, 0, range.length);
        return range;
    }

    // AES 256 PBKDF2 CBC iso10126 decryption
    // 16 byte IV must be prepended to ciphertext - Compatible with crypto-js
    public static String decrypt(String ciphertext, CharSequenceX password, int iterations)  {

        final int AESBlockSize = 4;

        byte[] cipherdata = Base64.decodeBase64(ciphertext.getBytes());

        //Seperate the IV and cipher data
        byte[] iv = copyOfRange(cipherdata, 0, AESBlockSize * 4);
        byte[] input = copyOfRange(cipherdata, AESBlockSize * 4, cipherdata.length);

        PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password.toString().toCharArray()), iv, iterations);
        KeyParameter keyParam = (KeyParameter)generator.generateDerivedParameters(256);

        CipherParameters params = new ParametersWithIV(keyParam, iv);

        // setup AES cipher in CBC mode with PKCS7 padding
        BlockCipherPadding padding = new ISO10126d2Padding();
        BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
        cipher.reset();
        cipher.init(false, params);

        // create a temporary buffer to decode into (includes padding)
        byte[] buf = new byte[cipher.getOutputSize(input.length)];
        int len = cipher.processBytes(input, 0, input.length, buf, 0);
        try    {
            len += cipher.doFinal(buf, len);
        }
        catch(InvalidCipherTextException icte)    {
            icte.printStackTrace();
            return null;
        }

        // remove padding
        byte[] out = new byte[len];
        System.arraycopy(buf, 0, out, 0, len);

        // return string representation of decoded bytes
        String ret = null;
        try    {
            ret = new String(out, "UTF-8");
        }
        catch(UnsupportedEncodingException uee)    {
            uee.printStackTrace();
            return null;
        }

        return ret;
    }

    public static String encrypt(String cleartext, CharSequenceX password, int iterations)    {

        final int AESBlockSize = 4;

        if(password == null)   {
            return null;
        }

        // Use secure random to generate a 16 byte iv
        SecureRandom random = new SecureRandom();
        byte iv[] = new byte[AESBlockSize * 4];
        random.nextBytes(iv);

        byte[] clearbytes = null;
        try    {
            clearbytes = cleartext.getBytes("UTF-8");
        }
        catch(UnsupportedEncodingException uee)    {
            uee.printStackTrace();
            return null;
        }

        PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
        generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password.toString().toCharArray()), iv, iterations);
        KeyParameter keyParam = (KeyParameter)generator.generateDerivedParameters(256);

        CipherParameters params = new ParametersWithIV(keyParam, iv);

        // setup AES cipher in CBC mode with PKCS7 padding
        BlockCipherPadding padding = new ISO10126d2Padding();
        BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding);
        cipher.reset();
        cipher.init(true, params);

        byte[] outBuf = cipherData(cipher, clearbytes);

        // Append to IV to the output
        int len1 = iv.length;
        int len2 = outBuf.length;
        byte[] ivAppended = new byte[len1 + len2];
        System.arraycopy(iv, 0, ivAppended, 0, len1);
        System.arraycopy(outBuf, 0, ivAppended, len1, len2);

        byte[] raw = Base64.encodeBase64(ivAppended);
        String ret = new String(raw);

        return ret;
    }

    private static byte[] cipherData(BufferedBlockCipher cipher, byte[] data)  {
        int minSize = cipher.getOutputSize(data.length);
        byte[] outBuf = new byte[minSize];
        int len1 = cipher.processBytes(data, 0, data.length, outBuf, 0);
        int len2 = -1;
        try    {
            len2 = cipher.doFinal(outBuf, len1);
        }
        catch(InvalidCipherTextException icte)    {
            icte.printStackTrace();
        }

        int actualLength = len1 + len2;
        byte[] result = new byte[actualLength];
        System.arraycopy(outBuf, 0, result, 0, result.length);
        return result;
    }

}
