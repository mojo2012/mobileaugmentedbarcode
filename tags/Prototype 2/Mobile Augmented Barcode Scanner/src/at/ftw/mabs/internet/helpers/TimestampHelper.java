//Author: Matthias Fuchs (meister.fuchs@gmail.com

package at.ftw.mabs.internet.helpers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class TimestampHelper {
	static TimestampHelper	instance	= null;

	TimestampHelper() {
	}

	public static TimestampHelper getInstance() {
		if (instance == null) {
			instance = new TimestampHelper();
		}

		return instance;
	}

	/**
	 * Creates a timestamp needed for the signing process of the REST request.
	 * 
	 * @return
	 */
	public String timestamp(String format) {
		String timestamp = null;
		Calendar cal = Calendar.getInstance();
		DateFormat dfm = new SimpleDateFormat(format);
		dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
		timestamp = dfm.format(cal.getTime());

		return timestamp;
	}
}
