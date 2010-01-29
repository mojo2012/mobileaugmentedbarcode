package at.ftw.mabs.logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import android.util.Log;
import at.ftw.mabs.internet.helpers.TimestampHelper;

public class Logger {
	final static String		TAG				= "MABS/Logger";
	final static String		filePath		= "/sdcard/MABS/";

	static TimestampHelper	timestampHelper	= TimestampHelper.getInstance();

	static public void log(String message) {
		String file = filePath + timestampHelper.timestamp("yyyy-MM-dd") + ".csv";
		boolean isNewFile = false;

		OutputStreamWriter out;

		try {
			File folder = new File(filePath);

			if (!folder.isDirectory()) {
				folder.mkdir();
			}

			File outputFile = new File(file);

			if (!outputFile.exists()) {
				outputFile.createNewFile();
				isNewFile = true;
			}

			out = new OutputStreamWriter(new FileOutputStream(outputFile, true));

			if (isNewFile)
				out.append("log time; description; isbn; start time; end time;\n");

			String time = timestampHelper.timestamp("hh:mm:ss");

			out.append(time + "; " + message + "\n");

			out.flush();
			out.close();
		} catch (Exception e) {
			Log.e(TAG, "Error while writing to  card!");
			e.printStackTrace();
		}
	}
}
