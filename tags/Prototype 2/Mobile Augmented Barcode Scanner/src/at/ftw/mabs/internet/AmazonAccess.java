//Author: Matthias Fuchs (meister.fuchs@gmail.com

package at.ftw.mabs.internet;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import at.ftw.mabs.internet.helpers.EncryptionHelper;
import at.ftw.mabs.internet.helpers.InternetHelper;
import at.ftw.mabs.internet.helpers.TimestampHelper;

public class AmazonAccess {
	private static AmazonAccess	instance			= null;

	// must be lower case
	static final String			endpoint			= "ecs.amazonaws.com";
	static final String			REQUEST_URI			= "/onca/xml";
	static final String			REQUEST_METHOD		= "GET";
	static final String			UTF8_CHARSET		= "UTF-8";

	static final String			awsAccessKeyId		= "AKIAJFGRIDCBAGNA2KNQ";
	static final String			awsSecretKey		= "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";

	static final String			ACCESS_KEY_ID_KEY	= "AWSAccessKeyId";
	static final String			ACCESS_KEY_ID		= awsAccessKeyId;
	static final String			ID_TYPE_KEY			= "IdType";
	static final String			ID_TYPE				= "EAN";
	static final String			ITEM_ID_KEY			= "ItemId";
	static final String			KEYWORDS_KEY		= "Keywords";
	static final String			OPERATION_KEY		= "Operation";
	static final String			OPERATION			= "ItemLookup";
	static final String			RESPONSE_GROUP_KEY	= "ResponseGroup";
	static final String			RESPONSE_GROUP		= "Reviews";
	static final String			SEARCH_INDEX_KEY	= "SearchIndex";
	static final String			SEARCH_INDEX		= "Books";
	static final String			SERVICE_KEY			= "Service";
	static final String			SERVICE				= "AWSECommerceService";
	static final String			VERSION_KEY			= "Version";

	static final String			TIMESTAMP_FORMAT	= "yyyy-MM-dd'T'HH:mm:ss'Z'";

	EncryptionHelper			encryptionHelper;
	InternetHelper				internetHelper;

	Map<String, String>			urlParams			= new TreeMap<String, String>();
	byte[]						secretyKeyBytes;

	private AmazonAccess() {
		encryptionHelper = EncryptionHelper.getInstance();
		internetHelper = InternetHelper.getInstance();

		try {
			secretyKeyBytes = awsSecretKey.getBytes(UTF8_CHARSET);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		urlParams.put(ACCESS_KEY_ID_KEY, ACCESS_KEY_ID);
		urlParams.put(ID_TYPE_KEY, ID_TYPE);
		urlParams.put(OPERATION_KEY, OPERATION);
		urlParams.put(RESPONSE_GROUP_KEY, RESPONSE_GROUP);
		urlParams.put(SEARCH_INDEX_KEY, SEARCH_INDEX);
		urlParams.put(SERVICE_KEY, SERVICE);

	}

	public static AmazonAccess getInstance() {
		if (instance == null) {
			instance = new AmazonAccess();
		}

		return instance;
	}

	/**
	 * Returns the Amazon book title of a given ISBN. NOT YET IMPLEMENTED
	 * 
	 * @param isbn
	 * @return
	 */
	public String getBookTitle(String isbn) {
		String xmlResponse = internetHelper.requestUrlContent(createRequestUrl(isbn));
		String title = "";

		String tag = "";

		return title;
	}

	/**
	 * Returns the Amazon rating of a given ISBN.
	 * 
	 * @param isbn
	 * @return
	 */
	public double getRating(String isbn) {
		String xmlResponse = internetHelper.requestUrlContent(createRequestUrl(isbn));
		String rating;

		String tag = "AverageRating";

		rating = getTagContent(xmlResponse, tag);

		if (rating.equals(""))
			rating = "-1";

		return Double.parseDouble(rating);
	}

	String getTagContent(String xml, String tag) {
		String retVal = "";

		try {
			int indexOfStartTag = xml.indexOf("<" + tag + ">") + tag.length() + 2;
			int indexOfStopTag = xml.indexOf("</" + tag + ">");

			retVal = xml.substring(indexOfStartTag, indexOfStopTag);
		} catch (StringIndexOutOfBoundsException ex) {
		}

		return retVal;
	}

	/**
	 * Creates a an REST request URL and signs it correctly.
	 * 
	 * @param params
	 * @return
	 */
	String createRequestUrl(String isbn) {
		SortedMap<String, String> sortedParamMap = new TreeMap<String, String>(urlParams);

		sortedParamMap.put(ITEM_ID_KEY, isbn);
		sortedParamMap.put(KEYWORDS_KEY, isbn);
		urlParams.put(VERSION_KEY, TimestampHelper.getInstance().timestamp("yyyy-MM-dd"));
		sortedParamMap.put("Timestamp", TimestampHelper.getInstance().timestamp(TIMESTAMP_FORMAT));

		String canonicalQS = internetHelper.canonicalize(sortedParamMap);

		String toSign =
				REQUEST_METHOD + "\n"
				+ endpoint + "\n"
				+ REQUEST_URI + "\n"
				+ canonicalQS;

		String hmac = encryptionHelper.hmac(secretyKeyBytes, toSign);
		String sig = internetHelper.percentEncodeRfc3986(hmac);
		String url = "http://" + endpoint + REQUEST_URI + "?" +
				canonicalQS + "&Signature=" + sig;

		return url;
	}
}
