package at.ftw.mabs.ui.views;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import at.ftw.mabs.camera.CameraManager;
import at.ftw.mabs.ui.infolayers.IInfoLayer;

public class AugmentedView extends View {
	static final String	TAG				= "MABS/AugmentedView";

	final Paint			paint;
	private final Rect	outerBox;
	private final Rect	innerBox;

	Timer				resetTimer;

	IInfoLayer			infoLayer;
	Bitmap				infoLayerBitmap	= null;

	boolean				barcodeFound	= false;

	// This constructor is used when the class is built from an XML resource.
	public AugmentedView(Context context, AttributeSet attrs) {
		super(context, attrs);

		paint = new Paint();

		paint.setStrokeWidth(2.0f);
		paint.setAlpha(100);

		outerBox = new Rect();
		innerBox = new Rect();
	}

	@Override
	public void onDraw(Canvas canvas) {
		Rect frame = CameraManager.get().getFramingRect();

		if (frame != null) {
			drawViewfinderBorder(canvas, frame);

			if (barcodeFound) {
				paint.setStyle(Style.FILL);

				infoLayerBitmap = infoLayer.getInfoLayer(frame.width() - 42, frame.height() - 42);

				if (infoLayerBitmap != null)
					canvas.drawBitmap(infoLayerBitmap, frame.left + 21, frame.top + 21, paint);

			} else {
				Log.v(TAG, "No info layer set.");
			}
		} else {
			if (infoLayerBitmap != null) {
				Log.v(TAG, "Removing old info layer bitmap.");

				infoLayerBitmap = null;
			}
		}
	}

	void drawViewfinderBorder(Canvas canvas, Rect frame) {
		paint.setStyle(Style.STROKE);

		paint.setColor(Color.BLACK);
		outerBox.set(frame.left + 19, frame.top + 19, frame.right - 19, frame.bottom - 19);
		canvas.drawRect(outerBox, paint);

		paint.setColor(Color.WHITE);
		innerBox.set(frame.left + 21, frame.top + 21, frame.right - 21, frame.bottom - 21);
		canvas.drawRect(innerBox, paint);
	}

	/**
	 * Augment the screen with the downloaded information about the barcode.
	 * 
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void setBarcode(String isbn) {
		infoLayer.setISBN(isbn);

		barcodeFound = true;

		if (resetTimer != null)
			resetTimer.cancel();

		resetTimer = new Timer();
		resetTimer.schedule(new BarcodeResetTimer(this), 5000);

		invalidate();
	}

	/**
	 * Set a IInfoLayer that will be used to show infos about the decoded ISBN.
	 * 
	 * @param infoLayer
	 */
	public void setInfoLayer(IInfoLayer infoLayer) {
		this.infoLayer = infoLayer;
	}

	/**
	 * Timer that resets the info layer.
	 */
	class BarcodeResetTimer extends TimerTask {
		AugmentedView	view;

		public BarcodeResetTimer(AugmentedView view) {
			this.view = view;
		}

		@Override
		public void run() {
			barcodeFound = false;
			resetTimer.cancel();
			this.cancel();

			Log.v(TAG, "Resetting augmented view.");

			postInvalidate();
		}
	}
}
