package at.ftw.mabs.internet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

public class UrlHelper {
	static UrlHelper	instance;

	static final String	UTF8_CHARSET	= "UTF-8";

	UrlHelper() {
	}

	public static UrlHelper getInstance() {
		if (instance == null) {
			instance = new UrlHelper();
		}

		return instance;
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
