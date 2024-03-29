/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.ftw.mabs.scanner;

import java.util.Hashtable;
import java.util.Vector;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import at.ftw.mabs.R;
import at.ftw.mabs.camera.CameraManager;
import at.ftw.mabs.ui.AugmentedRealityActivity;

import com.google.zxing.common.BarcodeFormat;
import com.google.zxing.common.BinaryBitmap;
import com.google.zxing.common.DecodeHintType;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.ReaderException;
import com.google.zxing.common.colormodels.BaseLuminanceSource;
import com.google.zxing.reader.EAN13Reader;
import com.google.zxing.result.Result;

/**
 * This thread does all the heavy lifting of decoding the images.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class DecodeThread extends Thread {
	private static final String				TAG				= "MABS/DecodeThread";

	public static final String				BARCODE_BITMAP	= "barcode_bitmap";

	private Handler							handler;
	private final AugmentedRealityActivity	activity;
	private final EAN13Reader				reader;

	DecodeThread(AugmentedRealityActivity activity) {
		this.activity = activity;
		reader = new EAN13Reader();

		setDecode1DMode();
	}

	Handler getHandler() {
		return handler;
	}

	@Override
	public void run() {
		Looper.prepare();

		handler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				switch (message.what) {
					case R.id.decode:
										decode((byte[]) message.obj, message.arg1, message.arg2);
						break;
					case R.id.quit:
										Looper.myLooper().quit();
						break;
				}
			}
		};
		Looper.loop();
	}

	/**
	 * Select the 1D formats we want this client to decode by hand.
	 */
	private void setDecode1DMode() {
		Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>(3);
		Vector<BarcodeFormat> vector = new Vector<BarcodeFormat>(3);
		vector.addElement(BarcodeFormat.EAN_13);
		hints.put(DecodeHintType.POSSIBLE_FORMATS, vector);
	}

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took. For efficiency, reuse the same reader objects from one decode to
	 * the next.
	 * 
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		long start = System.currentTimeMillis();
		boolean success;

		Result rawResult = null;
		BaseLuminanceSource source = CameraManager.get().buildLuminanceSource(data, width, height);
		BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));

		try {
			rawResult = reader.decode(bitmap);
			success = true;
		} catch (ReaderException e) {
			success = false;
		}

		long end = System.currentTimeMillis();

		if (success) {
			Log.v(TAG, "Found barcode (" + (end - start) + " ms):\n" + rawResult.toString());

			Message message = Message.obtain(activity.getHandler(), R.id.decode_succeeded, rawResult);
			Bundle bundle = new Bundle();
			bundle.putParcelable(BARCODE_BITMAP, source.renderCroppedGreyscaleBitmap());
			message.setData(bundle);
			message.sendToTarget();
		} else {
			Message message = Message.obtain(activity.getHandler(), R.id.decode_failed);
			message.sendToTarget();
		}
	}
}
