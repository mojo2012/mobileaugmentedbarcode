package at.ftw.mabs.internet;

import java.util.Map;
import java.util.TreeMap;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.util.Log;
import at.ftw.mabs.internet.encryption.SignedRequestsHelper;

public class AmazonRestRequest {
	static final String		ACCESS_KEY			= "AKIAJFGRIDCBAGNA2KNQ";
	static final String		ID_TYPE_KEY			= "IdType";
	static final String		ID_TYPE				= "EAN";
	static final String		ITEM_ID_KEY			= "ItemId";
	static final String		KEYWORDS_KEY		= "Keywords";
	static final String		OPERATION_KEY		= "Operation";
	static final String		OPERATION			= "ItemLookup";
	static final String		RESPONSE_GROUP_KEY	= "ResponseGroup";
	static final String		RESPONSE_GROUP		= "Reviews";
	static final String		SEARCH_INDEX_KEY	= "SearchIndex";
	static final String		SEARCH_INDEX		= "Books";
	static final String		SERVICE_KEY			= "Service";
	static final String		SERVICE				= "AWSECommerceService";
	static final String		VERSION_KEY			= "Version";
	static final String		VERSION				= "2009-12-31";

	Map<String, String>		urlParams			= new TreeMap<String, String>();

	SignedRequestsHelper	helper				= new SignedRequestsHelper();

	public AmazonRestRequest() {
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

		String tmpUrl = helper.sign(params);

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
}
