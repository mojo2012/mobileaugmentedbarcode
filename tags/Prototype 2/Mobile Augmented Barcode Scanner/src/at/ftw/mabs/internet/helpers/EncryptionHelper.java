//Author: Matthias Fuchs (meister.fuchs@gmail.com

package at.ftw.mabs.internet.helpers;

import java.io.UnsupportedEncodingException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionHelper {
	static EncryptionHelper	instance				= null;

	static final String		UTF8_CHARSET			= "UTF-8";
	static final String		HMAC_SHA256_ALGORITHM	= "HmacSHA256";

	SecretKeySpec			secretKeySpec			= null;
	Mac						mac						= null;

	// Mapping table from 6-bit nibbles to Base64 characters.
	static char[]			map1					= new char[64];
	static {
		int i = 0;
		for (char c = 'A'; c <= 'Z'; c++)
			map1[i++] = c;
		for (char c = 'a'; c <= 'z'; c++)
			map1[i++] = c;
		for (char c = '0'; c <= '9'; c++)
			map1[i++] = c;
		map1[i++] = '+';
		map1[i++] = '/';
	}

	// Mapping table from Base64 characters to 6-bit nibbles.
	static byte[]			map2					= new byte[128];
	static {
		for (int i = 0; i < map2.length; i++)
			map2[i] = -1;
		for (int i = 0; i < 64; i++)
			map2[map1[i]] = (byte) i;
	}

	EncryptionHelper() {
	}

	public static EncryptionHelper getInstance() {
		if (instance == null) {
			instance = new EncryptionHelper();
		}

		return instance;
	}

	/**
	 * Encodes a byte array into Base64 format. No blanks or line breaks are
	 * inserted. Home page: <a Author: Christian d'Heureuse, Inventec Informatik
	 * AG, Zurich, Switzerland http://www.source-code.biz">www.source-code.biz
	 * Multi-licensed: EPL/LGPL/AL/BSD.
	 * 
	 * @param in
	 *            an array containing the data bytes to be encoded.
	 * @return A character array with the Base64 encoded data.
	 */
	public char[] encodeBase64(byte[] in) {
		return encodeBase64(in, in.length);
	}

	/**
	 * Encodes a byte array into Base64 format. No blanks or line breaks are
	 * inserted. Author: Christian d'Heureuse, Inventec Informatik AG, Zurich,
	 * Switzerland http://www.source-code.biz">www.source-code.biz
	 * Multi-licensed: EPL/LGPL/AL/BSD.
	 * 
	 * @param in
	 *            an array containing the data bytes to be encoded.
	 * @param iLen
	 *            number of bytes to process in <code>in</code>.
	 * @return A character array with the Base64 encoded data.
	 */
	public char[] encodeBase64(byte[] in, int iLen) {
		int oDataLen = (iLen * 4 + 2) / 3; // output length without padding
		int oLen = ((iLen + 2) / 3) * 4; // output length including padding
		char[] out = new char[oLen];
		int ip = 0;
		int op = 0;
		while (ip < iLen) {
			int i0 = in[ip++] & 0xff;
			int i1 = ip < iLen ? in[ip++] & 0xff : 0;
			int i2 = ip < iLen ? in[ip++] & 0xff : 0;
			int o0 = i0 >>> 2;
			int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
			int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
			int o3 = i2 & 0x3F;
			out[op++] = map1[o0];
			out[op++] = map1[o1];
			out[op] = op < oDataLen ? map1[o2] : '=';
			op++;
			out[op] = op < oDataLen ? map1[o3] : '=';
			op++;
		}
		return out;
	}

	public String hmac(byte[] secretKeyBytes, String stringToSign) {
		try {
			secretKeySpec = new SecretKeySpec(secretKeyBytes, HMAC_SHA256_ALGORITHM);
			mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
			mac.init(secretKeySpec);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String signature = null;

		byte[] data;
		byte[] rawHmac;

		try {
			data = stringToSign.getBytes(UTF8_CHARSET);
			rawHmac = mac.doFinal(data);
			// Base64 encoder = new Base64();
			signature = new String(encodeBase64(rawHmac));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(UTF8_CHARSET + " is unsupported!", e);
		}

		return signature;
	}
}