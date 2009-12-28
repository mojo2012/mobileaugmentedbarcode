package at.ftw.mabs.internet;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class ContentDownloader {
	private static ContentDownloader	instance	= null;

	private ContentDownloader() {
	}

	public static ContentDownloader getInstance() {
		if (instance == null) {
			instance = new ContentDownloader();
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
