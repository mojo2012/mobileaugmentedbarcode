package at.ftw.mabs.internet;

import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import android.text.format.DateFormat;
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

	static final String		TIMESTAMP_FORMAT	= "yyyy-MM-ddTk-mm-ss.000Z";

	Map<String, String>		urlParams			= new TreeMap<String, String>();

	// ISBN-13
	String					itemId;
	// 2009-01-01T12%3A00%3A00Z
	String					timeStamp;
	String					url					= "AWSAccessKeyId=<accesskey>&IdType=<idtype>&ItemId=<isbn>&Keywords=<isbn>&Operation=<operation>&ResponseGroup=<responsegroup>&SearchIndex=<searchindex>&Service=<service>&Timestamp=<timestamp>&Version=<version>";
	String					signatureHeader		= "GET\necs.amazonaws.com\n/onca/xml";

	SignedRequestsHelper	helper				= new SignedRequestsHelper();

	// http://ecs.amazonaws.com/onca/xml?Service=AWSECommerceService&Version=2006-11-14&Operation=ItemLookup&SubscriptionId=0525E2PQ81DD7ZTWTK82&ItemId=9780060872984&IdType=EAN&SearchIndex=Books

	// Example request structure:
	// AWSAccessKeyId=00000000000000000000
	// ItemId=0679722769
	// Operation=ItemLookup
	// ResponseGroup=ItemAttributes%2COffers%2CImages%2CReviews
	// Service=AWSECommerceService
	// Timestamp=2009-01-01T12%3A00%3A00Z
	// Version=2009-01-06

	public AmazonRestRequest() {
		urlParams.put(ID_TYPE_KEY, ID_TYPE);
		urlParams.put(OPERATION_KEY, OPERATION);
		urlParams.put(RESPONSE_GROUP_KEY, RESPONSE_GROUP);
		urlParams.put(SEARCH_INDEX_KEY, SEARCH_INDEX);
		urlParams.put(SERVICE_KEY, SERVICE);
		urlParams.put(VERSION_KEY, VERSION);
	}

	public String requestReviews(String isbn) {
		Map<String, String> params = new TreeMap<String, String>();

		params.put(ITEM_ID_KEY, isbn);
		params.put(KEYWORDS_KEY, isbn);

		params.putAll(urlParams);

		String tmpUrl = helper.sign(params);

		return tmpUrl;
	}

	// public byte getRanking(String isbn) {
	// String tmpUrl = url;
	//
	// tmpUrl = tmpUrl.replace("<accesskey>", ACCESS_KEY);
	// tmpUrl = tmpUrl.replace("<idtype>", ID_TYPE);
	// tmpUrl = tmpUrl.replace("<isbn>", isbn);
	// tmpUrl = tmpUrl.replace("<operation>", OPERATION);
	// tmpUrl = tmpUrl.replace("<responsegroup>", RESPONSE_GROUP);
	// tmpUrl = tmpUrl.replace("<searchindex>", SEARCH_INDEX);
	// tmpUrl = tmpUrl.replace("<service>", SERVICE);
	// tmpUrl = tmpUrl.replace("<timestamp>", getCurrentTimestamp());
	// tmpUrl = tmpUrl.replace("<version>", VERSION);
	//
	// String signature = signatureHeader + tmpUrl;
	//
	// tmpUrl += "&Signature=" + signature;
	//
	// return 0;
	// }

	public String getCurrentTimestamp() {
		String timeStamp = "";

		Calendar calendar = Calendar.getInstance();
		// int date = calendar.get(calendar.DATE);

		timeStamp = DateFormat.format(TIMESTAMP_FORMAT, calendar).toString();

		return timeStamp;
	}
}
