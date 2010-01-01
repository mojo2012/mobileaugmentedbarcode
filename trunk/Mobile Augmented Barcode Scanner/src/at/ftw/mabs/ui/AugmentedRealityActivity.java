package at.ftw.mabs.ui;

import java.io.IOException;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import at.ftw.mabs.R;
import at.ftw.mabs.camera.CameraManager;
import at.ftw.mabs.internet.helpers.ConnectivityHelper;
import at.ftw.mabs.scanner.ActivityHandler;
import at.ftw.mabs.ui.infolayers.AmazonReviewLayer;
import at.ftw.mabs.ui.views.AugmentedView;

import com.google.zxing.result.Result;

/**
 * The barcode reader activity itself. This is loosely based on the
 * CameraPreview example included in the Android SDK.
 * 
 * @author meister.fuchs@gmail.com (Matthias Fuchs)
 */
public final class AugmentedRealityActivity extends Activity implements SurfaceHolder.Callback {
	static final String	TAG	= "MABS/AugmentedRealityActivity";

	ActivityHandler		handler;

	AugmentedView		augmentedView;
	SurfaceView			cameraView;
	TextView			statusTextView;

	SurfaceHolder		holder;
	ConnectivityHelper	connectivityHelper;

	boolean				hasSurface;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		makeFullScreen();

		loadViews();

		CameraManager.init(getApplication());
		connectivityHelper = ConnectivityHelper.getInstance(this);

		handler = null;
		hasSurface = false;

		showInfoMessage("Not internet connection available!\nNo ratings will be displayed");
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
		augmentedView.setInfoLayer(new AmazonReviewLayer());

		statusTextView = (TextView) findViewById(R.id.status_text_view);
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
		if (keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA) {
			// Handle these events so they don't launch the Camera app
			return true;
		}

		return super.onKeyDown(keyCode, event);
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

		if (connectivityHelper.isInternetAvailable()) {
			if (rawResult != null) {
				augmentedView.setBarcode(rawResult.getText());

				// setStatusText("Found ISBN: " + rawResult.getText());
			}
		} else {
			showInfoMessage("Not internet connection available!\nNo ratings will be displayed");
		}

		handler.sendEmptyMessage(R.id.restart_preview);
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
