package at.ftw.mabs.internet.helpers;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class InternetHelper {
	private static InternetHelper	instance	= null;

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
}
