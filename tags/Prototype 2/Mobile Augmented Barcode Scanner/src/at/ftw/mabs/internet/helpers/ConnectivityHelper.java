//Author: Matthias Fuchs (meister.fuchs@gmail.com

package at.ftw.mabs.internet.helpers;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.net.ConnectivityManager;

public class ConnectivityHelper {
	static ConnectivityHelper	instance	= null;

	Context						context		= null;
	ConnectivityManager			connectivityManager;

	private ConnectivityHelper(Context context) {
		this.context = context;

		connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	public static ConnectivityHelper getInstance(Context context) {
		if (instance == null) {
			instance = new ConnectivityHelper(context);
		}

		return instance;
	}

	public boolean isInternetAvailable() {
		boolean retVal = false;

		retVal = connectivityManager.getNetworkInfo(1).isConnectedOrConnecting();

		if (!retVal) {
			retVal = connectivityManager.getNetworkInfo(0).isConnectedOrConnecting();
		}

		return retVal;
	}

	boolean isInternetAvailable(int networkType, int hostAddress) {
		return connectivityManager.requestRouteToHost(networkType, hostAddress);
	}

	public boolean checkInternetAccessWithUrl() {
		return checkInternetAccessWithUrl("http://www.google.com");
	}

	boolean checkInternetAccessWithUrl(String url) {
		boolean retVal = false;

		try {

			URL location = new URL(url);

			HttpURLConnection urlc = (HttpURLConnection) location.openConnection();
			urlc.setRequestProperty("User-Agent", "Android");
			urlc.setRequestProperty("Connection", "close");
			urlc.setConnectTimeout(1000 * 7); // mTimeout is in seconds
			urlc.connect();

			if (urlc.getResponseCode() == 200) {
				retVal = true;
			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return retVal;
	}
}
