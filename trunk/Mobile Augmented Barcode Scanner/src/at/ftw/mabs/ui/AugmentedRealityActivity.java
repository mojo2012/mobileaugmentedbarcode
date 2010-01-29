//Author: Matthias Fuchs (meister.fuchs@gmail.com

package at.ftw.mabs.ui;

import java.io.IOException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import at.ftw.mabs.R;
import at.ftw.mabs.camera.CameraManager;
import at.ftw.mabs.internet.helpers.ConnectivityHelper;
import at.ftw.mabs.internet.helpers.TimestampHelper;
import at.ftw.mabs.logging.Logger;
import at.ftw.mabs.scanner.ActivityHandler;
import at.ftw.mabs.ui.infolayers.AmazonBookPriceLayer;
import at.ftw.mabs.ui.infolayers.AmazonRatingLayer;
import at.ftw.mabs.ui.infolayers.IInfoLayer;
import at.ftw.mabs.ui.views.AugmentedView;

import com.google.zxing.result.Result;

/**
 * The barcode reader activity itself. This is loosely based on the
 * CaptureActivity of the ZXing example code.
 * 
 * @author meister.fuchs@gmail.com (Matthias Fuchs)
 */
public final class AugmentedRealityActivity extends Activity implements SurfaceHolder.Callback {
	static final String			TAG								= "MABS/AugmentedRealityActivity";

	ActivityHandler				handler;

	AugmentedView				augmentedView;
	SurfaceView					cameraView;
	TextView					statusTextView;

	private MediaPlayer			mediaPlayer;
	private static final float	BEEP_VOLUME						= 0.10f;

	ConnectivityHelper			connectivityHelper;

	SharedPreferences			settings						= null;
	SharedPreferences.Editor	settingsEditor					= null;

	boolean						showFocusRect;
	String						infoLayerClassName				= "";

	String						lastBarcode						= "";

	String						logBarcodeDetectionStartTime	= "";

	enum infoLayers {
		ratingLayer,
		bookPriceLayer
	}

	boolean	hasSurface;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		makeFullScreen();

		loadViews();
		loadSettings();

		CameraManager.init(getApplication());
		connectivityHelper = ConnectivityHelper.getInstance(this);

		handler = null;
		hasSurface = false;

		if (!connectivityHelper.isInternetAvailable())
			showInfoMessage("No internet connection available!\nNo ratings will be displayed");
	}

	/**
	 * Show the activity in fullscreen.
	 */
	void makeFullScreen() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	/**
	 * Load all views and connect them to the activity.
	 */
	void loadViews() {
		setContentView(R.layout.augmented_view);

		cameraView = (SurfaceView) findViewById(R.id.preview_view);
		augmentedView = (AugmentedView) findViewById(R.id.augmented_view);
		statusTextView = (TextView) findViewById(R.id.status_text_view);
	}

	/**
	 * Initialize the camera and install the surface holder callback.
	 * 
	 * @param surfaceHolder
	 */
	void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			return;
		}

		if (handler == null) {
			handler = new ActivityHandler(this, true);
		}
	}

	void loadSettings() {
		settings = getSharedPreferences("at.ftw.mabs", 0);
		settingsEditor = settings.edit();

		showFocusRect = settings.getBoolean("focus_rect_visibility", false);
		augmentedView.setFocusRectVisiblity(showFocusRect);

		infoLayerClassName = settings.getString("infolayer_class_name", "AmazonRatingLayer");
		augmentedView.setInfoLayer(getInfoLayer(infoLayerClassName));

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

	IInfoLayer getInfoLayer(String className) {
		IInfoLayer infoLayer;

		if (className.equals("AmazonRatingLayer")) {
			infoLayer = new AmazonRatingLayer();
		} else if (className.equals("AmazonBookPriceLayer")) {
			infoLayer = new AmazonBookPriceLayer();
		} else {
			infoLayer = null;
		}

		return infoLayer;
	}

	void saveSettings() {
		settingsEditor.putBoolean("focus_rect_visibility", showFocusRect);
		settingsEditor.putString("infolayer_class_name", infoLayerClassName);

		settingsEditor.commit();
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
		Log.v(TAG, "Barcode found: " + rawResult.getText());

		if (!lastBarcode.equals(rawResult.getText())) {
			lastBarcode = rawResult.getText();
		}

		if (connectivityHelper.isInternetAvailable()) {
			if (rawResult != null) {
				augmentedView.setBarcode(rawResult, true);

				if (!logBarcodeDetectionStartTime.equals("")) {
					Logger.log("Starting detection: " + logBarcodeDetectionStartTime);
					Logger.log("Barcode found: " + rawResult.getText());

					logBarcodeDetectionStartTime = "";
				} else {
					Logger.log("!!!!!!!!!NO START TIME SET!!!!!!!!!");
					Logger.log("Barcode found: " + rawResult.getText());
				}

				handler.sendEmptyMessage(R.id.restart_preview);
			}
		} else {
			showInfoMessage("No internet connection available!\nNo ratings will be displayed");
		}
	}

	void showInfoMessage(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}

	void setStatusText(String text) {
		statusTextView.setText(text);
	}

	void resetStatusText() {
		statusTextView.setText(R.string.msg_default_status);
	}

	public Handler getHandler() {
		return handler;
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}

		CameraManager.get().closeDriver();

		lastBarcode = "";

		// saveSettings();
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.review_info_layer:
				item.setChecked(true);
				infoLayerClassName = "AmazonRatingLayer";

				augmentedView.setInfoLayer(getInfoLayer(infoLayerClassName));

				saveSettings();

				break;
			case R.id.price_info_layer:
				infoLayerClassName = "AmazonBookPriceLayer";

				augmentedView.setInfoLayer(getInfoLayer(infoLayerClassName));

				saveSettings();

				break;
			case R.id.toggle_focus_rect:
				showFocusRect = !showFocusRect;
				augmentedView.setFocusRectVisiblity(showFocusRect);

				saveSettings();

				break;
			case R.id.force_quit:
				finish();

				break;
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			logBarcodeDetectionStartTime = TimestampHelper.getInstance().timestamp("hh:mm:ss");

			return true;
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
}
