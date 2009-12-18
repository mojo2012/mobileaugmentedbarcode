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
import android.graphics.drawable.BitmapDrawable;
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
import android.widget.ImageView;
import android.widget.TextView;
import at.ftw.mabs.R;
import at.ftw.mabs.camera.CameraManager;
import at.ftw.mabs.scanner.CaptureActivityHandler;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

/**
 * The barcode reader activity itself. This is loosely based on the
 * CameraPreview example included in the Android SDK.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {
	private static final String	TAG							= "CaptureActivity";

	private static final int	SHARE_ID					= Menu.FIRST;
	private static final int	HISTORY_ID					= Menu.FIRST + 1;
	private static final int	SETTINGS_ID					= Menu.FIRST + 2;
	private static final int	HELP_ID						= Menu.FIRST + 3;
	private static final int	ABOUT_ID					= Menu.FIRST + 4;

	private static final int	MAX_RESULT_IMAGE_SIZE		= 150;
	private static final long	INTENT_RESULT_DURATION		= 1500L;
	private static final float	BEEP_VOLUME					= 0.10f;
	private static final long	VIBRATE_DURATION			= 200L;

	private static final String	PACKAGE_NAME				= "com.google.zxing.client.android";
	private static final String	PRODUCT_SEARCH_URL_PREFIX	= "http://www.google";
	private static final String	PRODUCT_SEARCH_URL_SUFFIX	= "/m/products/scan";
	private static final String	ZXING_URL					= "http://zxing.appspot.com/scan";

	private enum Source {
		NATIVE_APP_INTENT,
		PRODUCT_SEARCH_LINK,
		ZXING_LINK,
		NONE
	}

	private CaptureActivityHandler					handler;

	private ViewfinderView							viewfinderView;
	private MediaPlayer								mediaPlayer;
	private Result									lastResult;
	private boolean									hasSurface;
	private boolean									playBeep;
	private boolean									vibrate;
	private boolean									copyToClipboard;
	private Source									source;
	private String									sourceUrl;
	private String									decodeMode;
	private String									versionName;
	// private HistoryManager historyManager;

	private final OnCompletionListener				beepListener	= new BeepListener();

	private final DialogInterface.OnClickListener	aboutListener	=
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
		setContentView(R.layout.capture);

		CameraManager.init(getApplication());
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

		Intent intent = getIntent();
		String action = intent == null ? null : intent.getAction();
		String dataString = intent == null ? null : intent.getDataString();
		if (intent != null && action != null) {
			if (action.equals("com.google.zxing.client.android.SCAN")) {
				// Scan the formats the intent requested, and return the result
				// to the calling activity.
				source = Source.NATIVE_APP_INTENT;
				decodeMode = intent.getStringExtra("SCAN_MODE");
				resetStatusView();
			} else if (dataString != null && dataString.contains(PRODUCT_SEARCH_URL_PREFIX) &&
					dataString.contains(PRODUCT_SEARCH_URL_SUFFIX)) {
				// Scan only products and send the result to mobile Product
				// Search.
				source = Source.PRODUCT_SEARCH_LINK;
				sourceUrl = dataString;
				decodeMode = "ONE_D_MODE";
				resetStatusView();
			} else if (dataString != null && dataString.equals(ZXING_URL)) {
				// Scan all formats and handle the results ourselves.
				// TODO: In the future we could allow the hyperlink to include a
				// URL to send the results to.
				source = Source.ZXING_LINK;
				sourceUrl = dataString;
				decodeMode = null;
				resetStatusView();
			} else {
				// Scan all formats and handle the results ourselves (launched
				// from Home).
				source = Source.NONE;
				decodeMode = null;
				resetStatusView();
			}
		} else {
			source = Source.NONE;
			decodeMode = null;
			if (lastResult == null) {
				resetStatusView();
			}
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

		if (barcode == null) {
			// This is from history -- no saved barcode
			handleDecodeInternally(rawResult, null);
		} else {
			drawResultPoints(barcode, rawResult);
			switch (source) {
				case NATIVE_APP_INTENT:
				case PRODUCT_SEARCH_LINK:
					handleDecodeExternally(rawResult, barcode);
					break;
				case ZXING_LINK:
				case NONE:
					handleDecodeInternally(rawResult, barcode);
					break;
			}
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
		}
	}

	// Put up our own UI for how to handle the decoded contents.
	private void handleDecodeInternally(Result rawResult, Bitmap barcode) {
		viewfinderView.setVisibility(View.GONE);

		if (barcode == null) {
			barcode = ((BitmapDrawable) getResources().getDrawable(R.drawable.icon)).getBitmap();
		}
		ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
		barcodeImageView.setVisibility(View.VISIBLE);
		barcodeImageView.setMaxWidth(MAX_RESULT_IMAGE_SIZE);
		barcodeImageView.setMaxHeight(MAX_RESULT_IMAGE_SIZE);
		barcodeImageView.setImageBitmap(barcode);

		TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
		formatTextView.setVisibility(View.VISIBLE);
		formatTextView.setText(getString(R.string.msg_default_format) + ": " +
				rawResult.getBarcodeFormat().toString());

		// ResultHandler resultHandler =
		// ResultHandlerFactory.makeResultHandler(this, rawResult);
		// TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
		// typeTextView.setText(getString(R.string.msg_default_type) + ": " +
		// resultHandler.getType().toString());
		//
		// TextView contentsTextView = (TextView)
		// findViewById(R.id.contents_text_view);
		// CharSequence title = getString(resultHandler.getDisplayTitle());
		// SpannableStringBuilder styled = new SpannableStringBuilder(title +
		// "\n\n");
		// styled.setSpan(new UnderlineSpan(), 0, title.length(), 0);
		// CharSequence displayContents = resultHandler.getDisplayContents();
		// styled.append(displayContents);
		// contentsTextView.setText(styled);
		//
		// int buttonCount = resultHandler.getButtonCount();
		// ViewGroup buttonView = (ViewGroup)
		// findViewById(R.id.result_button_view);
		// buttonView.requestFocus();
		// for (int x = 0; x < ResultHandler.MAX_BUTTON_COUNT; x++) {
		// TextView button = (TextView) buttonView.getChildAt(x);
		// if (x < buttonCount) {
		// button.setVisibility(View.VISIBLE);
		// button.setText(resultHandler.getButtonText(x));
		// button.setOnClickListener(new ResultButtonListener(resultHandler,
		// x));
		// } else {
		// button.setVisibility(View.GONE);
		// }
		// }

		// if (copyToClipboard) {
		// ClipboardManager clipboard = (ClipboardManager)
		// getSystemService(CLIPBOARD_SERVICE);
		// clipboard.setText(displayContents);
		// }
	}

	// Briefly show the contents of the barcode, then handle the result outside
	// Barcode Scanner.
	private void handleDecodeExternally(Result rawResult, Bitmap barcode) {
		viewfinderView.drawResultBitmap(barcode);

		// Since this message will only be shown for a second, just tell the
		// user what kind of
		// barcode was found (e.g. contact info) rather than the full contents,
		// which they won't
		// have time to read.
		// ResultHandler resultHandler =
		// ResultHandlerFactory.makeResultHandler(this, rawResult);
		// TextView textView = (TextView) findViewById(R.id.status_text_view);
		// textView.setGravity(Gravity.CENTER);
		// textView.setTextSize(18.0f);
		// textView.setText(getString(resultHandler.getDisplayTitle()));
		//
		// statusView.setBackgroundColor(getResources().getColor(R.color.transparent));
		//
		// if (copyToClipboard) {
		// ClipboardManager clipboard = (ClipboardManager)
		// getSystemService(CLIPBOARD_SERVICE);
		// clipboard.setText(resultHandler.getDisplayContents());
		// }
		//
		// if (source == Source.NATIVE_APP_INTENT) {
		// // Hand back whatever action they requested - this can be changed to
		// // Intents.Scan.ACTION when
		// // the deprecated intent is retired.
		// Intent intent = new Intent(getIntent().getAction());
		// intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
		// intent.putExtra(Intents.Scan.RESULT_FORMAT,
		// rawResult.getBarcodeFormat().toString());
		// Message message = Message.obtain(handler, R.id.return_scan_result);
		// message.obj = intent;
		// handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
		// } else if (source == Source.PRODUCT_SEARCH_LINK) {
		// // Reformulate the URL which triggered us into a query, so that the
		// // request goes to the same
		// // TLD as the scan URL.
		// Message message = Message.obtain(handler, R.id.launch_product_query);
		// int end = sourceUrl.lastIndexOf("/scan");
		// message.obj = sourceUrl.substring(0, end) + "?q=" +
		// resultHandler.getDisplayContents().toString() + "&source=zxing";
		// handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
		// }
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
			handler = new CaptureActivityHandler(this, "ONE_D_MODE", beginScanning);
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
