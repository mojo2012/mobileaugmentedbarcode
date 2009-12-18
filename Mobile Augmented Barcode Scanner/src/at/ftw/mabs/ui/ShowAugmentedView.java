package at.ftw.mabs.ui;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.WindowManager;
import at.ftw.mabs.camera.CameraManager;
import at.ftw.mabs.scanner.CaptureActivityHandler;

import com.google.zxing.Result;

public class ShowAugmentedView extends Activity {
	static final String				TAG	= "MABS/ShowAugmentedView";

	private AugmentedView			previewView;

	private CaptureActivityHandler	handler;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		CameraManager.init(getApplication());
		handler = null;

		// Create our Preview view and set it as the content of our activity.
		previewView = new AugmentedView(this);
		setContentView(previewView);

		// setContentView(R.layout.augmented_view);
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

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			return;
		}
		if (handler == null) {
			boolean beginScanning = true;
			handler = new CaptureActivityHandler(this, "ONE_D_MODE", beginScanning);
		}
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
	public void drawViewfinder() {

	}

	public Handler getHandler() {
		return handler;
	}
}