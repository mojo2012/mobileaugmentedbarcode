/*
 * Copyright (C) 2008 ZXing authors
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

package at.ftw.mabs.ui;

import java.io.IOException;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import at.ftw.mabs.R;
import at.ftw.mabs.camera.CameraManager;
import at.ftw.mabs.scanner.CaptureActivityHandler;
import at.ftw.mabs.ui.views.ViewfinderView;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

/**
 * The barcode reader activity itself. This is loosely based on the
 * CameraPreview example included in the Android SDK.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class AugmentedRealityActivity extends Activity implements SurfaceHolder.Callback {
	static final String	TAG							= "MABS/CaptureActivity";

	static final int	SHARE_ID					= Menu.FIRST;
	static final int	HISTORY_ID					= Menu.FIRST + 1;
	static final int	SETTINGS_ID					= Menu.FIRST + 2;
	static final int	HELP_ID						= Menu.FIRST + 3;
	static final int	ABOUT_ID					= Menu.FIRST + 4;

	static final int	MAX_RESULT_IMAGE_SIZE		= 150;
	static final long	INTENT_RESULT_DURATION		= 1500L;
	static final float	BEEP_VOLUME					= 0.10f;
	static final long	VIBRATE_DURATION			= 200L;

	static final String	PRODUCT_SEARCH_URL_PREFIX	= "http://www.amazon.de";
	static final String	PRODUCT_SEARCH_URL_SUFFIX	= "/m/products/scan";

	enum Source {
		NATIVE_APP_INTENT,
		PRODUCT_SEARCH_LINK,
		ZXING_LINK,
		NONE
	}

	CaptureActivityHandler					handler;

	SurfaceView								previewView;
	ViewfinderView							viewfinderView;
	MediaPlayer								mediaPlayer;
	Result									lastResult;
	boolean									hasSurface;
	boolean									playBeep;
	boolean									vibrate;
	boolean									copyToClipboard;
	Source									source;
	String									sourceUrl;
	String									decodeMode;
	String									versionName;
	// private HistoryManager historyManager;

	final OnCompletionListener				beepListener	= new BeepListener();

	final DialogInterface.OnClickListener	aboutListener	=
																	new DialogInterface.OnClickListener() {
																public void onClick(
																		DialogInterface dialogInterface,
																		int i) {
																	Intent intent = new Intent(	Intent.ACTION_VIEW,
																								Uri.parse(getString(R.string.zxing_url)));
																	startActivity(intent);
																}
															};

	public Handler getHandler() {
		return handler;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.augmented_view);

		CameraManager.init(getApplication());

		previewView = (SurfaceView) findViewById(R.id.preview_view);
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		handler = null;
		lastResult = null;
		hasSurface = false;
	}

	@Override
	protected void onResume() {
		super.onResume();

		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();

		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		source = Source.NONE;
		decodeMode = null;

		if (lastResult == null) {
			resetStatusView();
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		vibrate = true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (source == Source.NATIVE_APP_INTENT) {
				setResult(RESULT_CANCELED);
				finish();
				return true;
			} else if ((source == Source.NONE || source == Source.ZXING_LINK) && lastResult != null) {
				resetStatusView();
				handler.sendEmptyMessage(R.id.restart_preview);
				return true;
			}
		} else if (keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA) {
			// Handle these events so they don't launch the Camera app
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onConfigurationChanged(Configuration config) {
		// Do nothing, this is to prevent the activity from being restarted when
		// the keyboard opens.
		super.onConfigurationChanged(config);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	/**
	 * A valid barcode has been found, so give an indication of success and show
	 * the results.
	 * 
	 * @param rawResult
	 *            The contents of the barcode.
	 * @param barcode
	 *            A greyscale bitmap of the camera data which was decoded.
	 */
	public void handleDecode(Result rawResult, Bitmap barcode) {
		lastResult = rawResult;

		Log.v(TAG, "Barcode found: " + rawResult.getText());

		if (barcode != null) {
			drawResultPoints(barcode, rawResult);
			viewfinderView.drawResultBitmap(barcode);

			TextView formatTextView = (TextView) findViewById(R.id.status_text_view);
			formatTextView.setVisibility(View.VISIBLE);
			formatTextView.setText(rawResult.getText());

			// handler.sendEmptyMessage(R.id.restart_preview);
		}
	}

	/**
	 * Superimpose a line for 1D or dots for 2D to highlight the key features of
	 * the barcode.
	 * 
	 * @param barcode
	 *            A bitmap of the captured image.
	 * @param rawResult
	 *            The decoded results which contains the points to draw.
	 */
	private void drawResultPoints(Bitmap barcode, Result rawResult) {
		ResultPoint[] points = rawResult.getResultPoints();

		if (points != null && points.length > 0) {
			Canvas canvas = new Canvas(barcode);
			Paint paint = new Paint();
			paint.setColor(getResources().getColor(R.color.result_image_border));
			paint.setStrokeWidth(3.0f);
			paint.setStyle(Paint.Style.STROKE);
			Rect border = new Rect(2, 2, barcode.getWidth() - 2, barcode.getHeight() - 2);
			canvas.drawRect(border, paint);

			paint.setColor(getResources().getColor(R.color.result_points));

			if (points.length == 2) {
				paint.setStrokeWidth(4.0f);
				canvas.drawLine(points[0].getX(), points[0].getY(), points[1].getX(),
						points[1].getY(), paint);
			} else {
				paint.setStrokeWidth(10.0f);
				for (ResultPoint point : points) {
					canvas.drawPoint(point.getX(), point.getY(), paint);
				}
			}

			previewView.draw(canvas);
		}
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			return;
		}
		if (handler == null) {
			boolean beginScanning = lastResult == null;
			handler = new CaptureActivityHandler(this, beginScanning);
		}
	}

	private void resetStatusView() {
		viewfinderView.setVisibility(View.VISIBLE);

		TextView textView = (TextView) findViewById(R.id.status_text_view);
		textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		textView.setTextSize(14.0f);
		textView.setText(R.string.msg_default_status);
		lastResult = null;
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private static class BeepListener implements OnCompletionListener {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	}
}
