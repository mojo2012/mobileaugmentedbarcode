package at.ftw.mabs.ui.views;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import at.ftw.mabs.camera.CameraManager;
import at.ftw.mabs.internet.helpers.TimestampHelper;
import at.ftw.mabs.ui.AugmentedRealityActivity;
import at.ftw.mabs.ui.infolayers.IInfoLayer;

public class AugmentedView extends View {
	static final String			TAG				= "MABS/AugmentedView";

	final Paint					paint;
	private final Rect			outerBox;
	private final Rect			innerBox;

	Timer						resetTimer;
	AugmentedRealityActivity	activity		= null;
	IInfoLayer					infoLayer;
	Bitmap						infoLayerBitmap	= null;

	boolean						barcodeFound	= false;
	String						lastBarcode		= "";
	String						timeToHideBarcode;

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

			if (!barcodeFound) {
				if (infoLayerBitmap != null) {
					Log.v(TAG, "resetting");
					infoLayerBitmap = null;
				}
			} else {
				paint.setStyle(Style.FILL);

				if (infoLayerBitmap == null)
					infoLayerBitmap = infoLayer.getInfoLayer(frame.width() - 42, frame.height() - 42);

				canvas.drawBitmap(infoLayerBitmap, frame.left + 21, frame.top + 21, paint);
			}
		}
	}

	/**
	 * Create rectangle coordinates surrounding the barcode on the screen.
	 * 
	 * @param rawScreenPoints
	 * @return
	 */
	Point[] getRectangularResultPoints(Point[] rawScreenPoints) {
		Point[] points = new Point[2];

		int distance = rawScreenPoints[1].x - rawScreenPoints[0].x;
		int height = (int) (distance / 2.1f);

		Rect frame = CameraManager.get().getFramingRect();

		int top = frame.centerY() - (height / 2);
		int bottom = top + height;

		points[0] = new Point(rawScreenPoints[0].x, top);
		points[1] = new Point(rawScreenPoints[1].x, bottom);

		return points;
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
	 * Timer that resets the info layer.
	 */
	class BarcodeResetTimer extends TimerTask {
		View	view;

		public BarcodeResetTimer(View view) {
			this.view = view;
		}

		@Override
		public void run() {
			barcodeFound = false;
			view.invalidate();

			Log.v(TAG, "cancel timer");

			this.cancel();
		}
	}

	public void setActivity(AugmentedRealityActivity activity) {
		this.activity = activity;
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

		TimestampHelper.getInstance().timestamp("ss");

		// if (resetTimer != null)
		// resetTimer.cancel();
		//
		// resetTimer = new Timer();
		// resetTimer.schedule(new BarcodeResetTimer(this), 5000);

		invalidate();
	}

	public void setInfoLayer(IInfoLayer infoLayer) {
		this.infoLayer = infoLayer;
	}
}
