//Author: Matthias Fuchs (meister.fuchs@gmail.com

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
import at.ftw.mabs.ui.infolayers.IInfoLayer;

import com.google.zxing.result.Result;
import com.google.zxing.result.ResultPoint;

public class AugmentedView extends View {
	static final String	TAG				= "MABS/AugmentedView";

	final Paint			paint;
	private final Rect	outerBox;
	private final Rect	innerBox;

	Timer				resetTimer;

	IInfoLayer			infoLayer;
	Bitmap				infoLayerBitmap	= null;

	boolean				barcodeFound	= false;

	Point[]				resultPoints;

	boolean				showFocusRect	= false;

	// This constructor is used when the class is built from an XML resource.
	public AugmentedView(Context context, AttributeSet attrs) {
		super(context, attrs);

		paint = new Paint();
		paint.setStrokeWidth(2.0f);
		paint.setAlpha(100);
		paint.setColor(Color.WHITE);

		outerBox = new Rect();
		innerBox = new Rect();
	}

	@Override
	public void onDraw(Canvas canvas) {
		Rect frame = CameraManager.get().getFramingRect();

		if (frame != null) {
			if (showFocusRect)
				drawFocusRect(canvas, frame);

			if (barcodeFound) {
				paint.setStyle(Style.FILL);

				int x;
				int y;

				if (resultPoints == null) {
					infoLayerBitmap = infoLayer.getInfoLayer(frame.width() - 122, frame.height() - 122);

					x = frame.left + 61;
					y = frame.top + 61;
				} else {
					infoLayerBitmap = infoLayer.getInfoLayer(resultPoints[1].x - resultPoints[0].x,
							resultPoints[1].y - resultPoints[0].y);

					x = resultPoints[0].x;
					y = resultPoints[0].y;
				}

				if (infoLayerBitmap != null)
					canvas.drawBitmap(infoLayerBitmap, x, y, paint);

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

	void drawFocusRect(Canvas canvas, Rect frame) {
		paint.setStyle(Style.STROKE);

		paint.setColor(Color.BLACK);
		outerBox.set(frame.left + 59, frame.top + 59, frame.right - 59, frame.bottom - 59);
		canvas.drawRect(outerBox, paint);

		paint.setColor(Color.WHITE);
		innerBox.set(frame.left + 61, frame.top + 61, frame.right - 61, frame.bottom - 61);
		canvas.drawRect(innerBox, paint);
	}

	void drawResultPoints(Canvas canvas) {
		if (resultPoints != null) {
			if (resultPoints.length == 2) {
				paint.setStrokeWidth(4.0f);
				paint.setColor(Color.DKGRAY);

				canvas.drawLine(resultPoints[0].x, resultPoints[0].y,
						resultPoints[1].x,
						resultPoints[0].y, paint);

				canvas.drawLine(resultPoints[1].x, resultPoints[0].y,
						resultPoints[1].x,
						resultPoints[1].y, paint);

				canvas.drawLine(resultPoints[0].x, resultPoints[0].y,
						resultPoints[0].x,
						resultPoints[1].y, paint);

				canvas.drawLine(resultPoints[0].x, resultPoints[1].y,
						resultPoints[1].x,
						resultPoints[1].y, paint);
			}
		}
	}

	/**
	 * Augment the screen with the downloaded information about the barcode.
	 * 
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void setBarcode(String isbn) {
		resultPoints = null;
		infoLayer.setISBN(isbn);

		barcodeFound = true;

		if (resetTimer != null)
			resetTimer.cancel();

		resetTimer = new Timer();
		resetTimer.schedule(new BarcodeResetTimerTask(this), 5000);

		invalidate();
	}

	/**
	 * Augment the screen with the downloaded information about the barcode.
	 * Draws the overlay to the given result points.
	 * 
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void setBarcode(Result result) {
		setBarcode(result.getText());

		resultPoints = getRectangularResultPoints(result.getResultPoints());

		invalidate();
	}

	/**
	 * Create rectangle coordinates surrounding the barcode on the screen.
	 * 
	 * @param rawScreenPoints
	 * @return
	 */
	Point[] getRectangularResultPoints(ResultPoint[] rawScreenPoints) {
		Point[] points = new Point[2];

		int distance = (int) (rawScreenPoints[1].getX() - rawScreenPoints[0].getX());
		int height = (int) (distance / 1.9f);

		int top = (int) (rawScreenPoints[0].getY() - (height / 2));
		int bottom = top + height;

		points[0] = new Point((int) rawScreenPoints[0].getX() - 10, top);
		points[1] = new Point((int) rawScreenPoints[1].getX() + 10, bottom);

		return points;
	}

	/**
	 * Set a IInfoLayer that will be used to show infos about the decoded ISBN.
	 * 
	 * @param infoLayer
	 */
	public void setInfoLayer(IInfoLayer infoLayer) {
		this.infoLayer = infoLayer;
	}

	public void setFocusRectVisiblity(boolean showFocusRect) {
		this.showFocusRect = showFocusRect;
		invalidate();
	}

	/**
	 * Timer that resets the info layer.
	 */
	class BarcodeResetTimerTask extends TimerTask {
		AugmentedView	view;

		public BarcodeResetTimerTask(AugmentedView view) {
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
