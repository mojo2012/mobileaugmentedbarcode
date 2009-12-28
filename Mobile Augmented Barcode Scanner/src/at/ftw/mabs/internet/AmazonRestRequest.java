package at.ftw.mabs.internet;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.util.Log;
import at.ftw.mabs.internet.helpers.EncryptionHelper;
import at.ftw.mabs.internet.helpers.UrlHelper;

public class AmazonRestRequest {
	static final String	UTF8_CHARSET		= "UTF-8";
	static final String	REQUEST_URI			= "/onca/xml";
	static final String	REQUEST_METHOD		= "GET";

	// must be lower case
	final String		endpoint			= "ecs.amazonaws.com";

	final String		awsAccessKeyId		= "AKIAJFGRIDCBAGNA2KNQ";
	final String		awsSecretKey		= "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";
	byte[]				secretyKeyBytes;

	EncryptionHelper	encryptionHelper;
	UrlHelper			urlHelper;

	static final String	ACCESS_KEY			= "AKIAJFGRIDCBAGNA2KNQ";
	static final String	ID_TYPE_KEY			= "IdType";
	static final String	ID_TYPE				= "EAN";
	static final String	ITEM_ID_KEY			= "ItemId";
	static final String	KEYWORDS_KEY		= "Keywords";
	static final String	OPERATION_KEY		= "Operation";
	static final String	OPERATION			= "ItemLookup";
	static final String	RESPONSE_GROUP_KEY	= "ResponseGroup";
	static final String	RESPONSE_GROUP		= "Reviews";
	static final String	SEARCH_INDEX_KEY	= "SearchIndex";
	static final String	SEARCH_INDEX		= "Books";
	static final String	SERVICE_KEY			= "Service";
	static final String	SERVICE				= "AWSECommerceService";
	static final String	VERSION_KEY			= "Version";
	static final String	VERSION				= "2009-12-31";

	Map<String, String>	urlParams			= new TreeMap<String, String>();

	public AmazonRestRequest() {
		encryptionHelper = EncryptionHelper.getInstance();
		urlHelper = UrlHelper.getInstance();

		try {
			secretyKeyBytes = awsSecretKey.getBytes(UTF8_CHARSET);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		urlParams.put(ID_TYPE_KEY, ID_TYPE);
		urlParams.put(OPERATION_KEY, OPERATION);
		urlParams.put(RESPONSE_GROUP_KEY, RESPONSE_GROUP);
		urlParams.put(SEARCH_INDEX_KEY, SEARCH_INDEX);
		urlParams.put(SERVICE_KEY, SERVICE);
		urlParams.put(VERSION_KEY, VERSION);
	}

	String request(String requestUrl) {
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

	String createRequestUrl(String isbn) {
		Map<String, String> params = new TreeMap<String, String>();

		params.putAll(urlParams);

		params.put(ITEM_ID_KEY, isbn);
		params.put(KEYWORDS_KEY, isbn);

		String tmpUrl = getSignedUrl(params);

		return tmpUrl;
	}

	public float getRating(String isbn) {
		String xmlResponse = request(createRequestUrl(isbn));
		String rating;

		String tag = "AverageRating";

		try {
			int indexOfStartTag = xmlResponse.indexOf("<" + tag + ">") + tag.length() + 2;
			int indexOfStopTag = xmlResponse.indexOf("</" + tag + ">");

			rating = xmlResponse.substring(indexOfStartTag, indexOfStopTag);

		} catch (StringIndexOutOfBoundsException ex) {
			rating = "-1.0";
		}

		return Float.parseFloat(rating);
	}

	/**
	 * Creates a an REST request URL and signs it correctly.
	 * 
	 * @param params
	 * @return
	 */
	public String getSignedUrl(Map<String, String> params) {
		params.put("AWSAccessKeyId", awsAccessKeyId);
		params.put("Timestamp", timestamp());

		SortedMap<String, String> sortedParamMap =
				new TreeMap<String, String>(params);
		String canonicalQS = urlHelper.canonicalize(sortedParamMap);
		String toSign =
				REQUEST_METHOD + "\n"
				+ endpoint + "\n"
				+ REQUEST_URI + "\n"
				+ canonicalQS;

		String hmac = encryptionHelper.hmac(secretyKeyBytes, toSign);
		String sig = urlHelper.percentEncodeRfc3986(hmac);
		String url = "http://" + endpoint + REQUEST_URI + "?" +
				canonicalQS + "&Signature=" + sig;

		return url;
	}

	/**
	 * Creates a timestamp needed for the signing process of the REST request.
	 * 
	 * @return
	 */
	String timestamp() {
		String timestamp = null;
		Calendar cal = Calendar.getInstance();
		DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
		timestamp = dfm.format(cal.getTime());
		return timestamp;
	}
}
