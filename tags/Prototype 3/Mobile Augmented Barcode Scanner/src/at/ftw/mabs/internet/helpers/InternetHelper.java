//Author: Matthias Fuchs (meister.fuchs@gmail.com

package at.ftw.mabs.internet.helpers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class InternetHelper {
	private static InternetHelper	instance		= null;

	static final String				UTF8_CHARSET	= "UTF-8";

	private InternetHelper() {
	}

	public static InternetHelper getInstance() {
		if (instance == null) {
			instance = new InternetHelper();
		}

		return instance;
	}

	public String requestUrlContent(String requestUrl) {
		String response = "";

		Log.v("Test", requestUrl);

		HttpClient client = new DefaultHttpClient();

		try {
			HttpGet get = new HttpGet(requestUrl);

			HttpResponse rsp = client.execute(get);
			response = EntityUtils.toString(rsp.getEntity());

			return response;
		} catch (Exception ex) {
			Log.e("Test", ex.getMessage());
			return null;
		}
	}

	/**
	 * Encodes special characters for use in URLs.
	 * 
	 * @param s
	 * @return
	 */
	public String percentEncodeRfc3986(String s) {
		String out;
		try {
			out = URLEncoder.encode(s, UTF8_CHARSET)
					.replace("+", "%20")
					.replace("*", "%2A")
					.replace("%7E", "~");
		} catch (UnsupportedEncodingException e) {
			out = s;
		}
		return out;
	}

	/**
	 * Appends all given URL parameters to a correct URL.
	 * 
	 * @param sortedParamMap
	 * @return
	 */
	public String canonicalize(SortedMap<String, String> sortedParamMap) {
		if (sortedParamMap.isEmpty()) {
			return "";
		}

		StringBuffer buffer = new StringBuffer();
		Iterator<Map.Entry<String, String>> iter =
				sortedParamMap.entrySet().iterator();

		while (iter.hasNext()) {
			Map.Entry<String, String> kvpair = iter.next();
			buffer.append(percentEncodeRfc3986(kvpair.getKey()));
			buffer.append("=");
			buffer.append(percentEncodeRfc3986(kvpair.getValue()));
			if (iter.hasNext()) {
				buffer.append("&");
			}
		}

		String cannoical = buffer.toString();
		return cannoical;
	}
}
