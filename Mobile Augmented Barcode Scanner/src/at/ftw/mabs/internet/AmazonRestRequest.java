package at.ftw.mabs.internet;

import java.util.Calendar;

import android.text.format.DateFormat;

public class AmazonRestRequest {
	static final String	ACCESS_KEY				= "AKIAJFGRIDCBAGNA2KNQ";
	static final String	ID_TYPE					= "EAN";
	static final String	OPERATION				= "ItemLookup";
	static final String	RESPONSE_GROUP			= "Reviews";
	static final String	SEARCH_INDEX			= "Books";
	static final String	SERVICE					= "AWSECommerceService";
	static final String	VERSION					= "2009-12-31";

	static final String	TIMESTAMP_DATE_FORMAT	= "yyyy-mm-tt";
	static final String	TIMESTAMP_TIME_FORMAT	= "hh-MM-ss";
	static final String	TIMESTAMP_FORMAT		= "<date>T<time>.000Z";

	// ISBN-13
	String				itemId;
	// 2009-01-01T12%3A00%3A00Z
	String				timeStamp;
	String				url						= "AWSAccessKeyId=<accesskey>&IdType=<idtype>&ItemId=<isbn>&Keywords=<isbn>&Operation=<operation>&ResponseGroup=<responsegroup>&SearchIndex=<searchindex>&Service=<service>&Timestamp=<timestamp>&Version=<version>";
	String				signatureHeader			= "GET\necs.amazonaws.com\n/onca/xml";

	// http://ecs.amazonaws.com/onca/xml?Service=AWSECommerceService&Version=2006-11-14&Operation=ItemLookup&SubscriptionId=0525E2PQ81DD7ZTWTK82&ItemId=9780060872984&IdType=EAN&SearchIndex=Books

	// Example request structure:
	// AWSAccessKeyId=00000000000000000000
	// ItemId=0679722769
	// Operation=ItemLookup
	// ResponseGroup=ItemAttributes%2COffers%2CImages%2CReviews
	// Service=AWSECommerceService
	// Timestamp=2009-01-01T12%3A00%3A00Z
	// Version=2009-01-06

	public byte getRanking(String isbn) {
		String tmpUrl = url;

		tmpUrl = tmpUrl.replace("<accesskey>", ACCESS_KEY);
		tmpUrl = tmpUrl.replace("<idtype>", ID_TYPE);
		tmpUrl = tmpUrl.replace("<isbn>", isbn);
		tmpUrl = tmpUrl.replace("<operation>", OPERATION);
		tmpUrl = tmpUrl.replace("<responsegroup>", RESPONSE_GROUP);
		tmpUrl = tmpUrl.replace("<searchindex>", SEARCH_INDEX);
		tmpUrl = tmpUrl.replace("<service>", SERVICE);
		tmpUrl = tmpUrl.replace("<timestamp>", ACCESS_KEY);
		tmpUrl = tmpUrl.replace("<version>", VERSION);

		String tmpSignature = signatureHeader + tmpUrl;

		return 0;
	}

	String getCurrentTimestamp() {
		String timeStamp = "";

		Calendar calendar = Calendar.getInstance();
		// int date = calendar.get(calendar.DATE);

		timeStamp = DateFormat.format(TIMESTAMP_DATE_FORMAT, calendar).toString();

		return timeStamp;
	}
}
