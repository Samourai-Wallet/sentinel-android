package com.samourai.sentinel.util;

import android.util.Patterns;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.WrongNetworkException;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.uri.BitcoinURI;
import org.bitcoinj.uri.BitcoinURIParseException;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;
//import android.util.Log;

public class FormatsUtil {

	private Pattern emailPattern = Patterns.EMAIL_ADDRESS;
	private Pattern phonePattern = Pattern.compile("(\\+[1-9]{1}[0-9]{1,2}+|00[1-9]{1}[0-9]{1,2}+)[\\(\\)\\.\\-\\s\\d]{6,16}");

	public static final int MAGIC_XPUB = 0x0488B21E;
	public static final int MAGIC_YPUB = 0x049D7CB2;

	private static FormatsUtil instance = null;
	
	private FormatsUtil() { ; }

	public static FormatsUtil getInstance() {

		if(instance == null) {
			instance = new FormatsUtil();
		}

		return instance;
	}

	public String validateBitcoinAddress(final String address) {
		
		if(isValidBitcoinAddress(address)) {
			return address;
		}
		else {
			String addr = uri2BitcoinAddress(address);
			if(addr != null) {
				return addr;
			}
			else {
				return null;
			}
		}
	}

	public boolean isValidBitcoinAddress(final String address) {

		boolean ret = false;
		Address addr = null;
		
		try {
			addr = new Address(MainNetParams.get(), address);
			if(addr != null) {
				ret = true;
			}
		}
		catch(WrongNetworkException wne) {
			ret = false;
		}
		catch(AddressFormatException afe) {
			ret = false;
		}

		return ret;
	}

	private String uri2BitcoinAddress(final String address) {
		
		String ret = null;
		BitcoinURI uri = null;
		
		try {
			uri = new BitcoinURI(address);
			ret = uri.getAddress().toString();
		}
		catch(BitcoinURIParseException bupe) {
			ret = null;
		}
		
		return ret;
	}

	public boolean isValidXpub(String xpub){

		try {
			byte[] xpubBytes = Base58.decodeChecked(xpub);

			ByteBuffer byteBuffer = ByteBuffer.wrap(xpubBytes);
			int magic = byteBuffer.getInt();
			if(magic != MAGIC_XPUB && magic != MAGIC_YPUB)   {
				throw new AddressFormatException("invalid version: " + xpub);
			}
			else	{

				byte[] chain = new byte[32];
				byte[] pub = new byte[33];
				// depth:
				byteBuffer.get();
				// parent fingerprint:
				byteBuffer.getInt();
				// child no.
				byteBuffer.getInt();
				byteBuffer.get(chain);
				byteBuffer.get(pub);

				ByteBuffer pubBytes = ByteBuffer.wrap(pub);
				int firstByte = pubBytes.get();
				if(firstByte == 0x02 || firstByte == 0x03){
					return true;
				}else{
					throw new AddressFormatException("invalid format: " + xpub);
				}
			}
		}
		catch(Exception e)	{
			return false;
		}
	}

}
