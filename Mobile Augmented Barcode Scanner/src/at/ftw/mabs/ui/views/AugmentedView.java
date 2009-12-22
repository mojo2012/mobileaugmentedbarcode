package at.ftw.mabs.ui.views;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import at.ftw.mabs.camera.CameraManager;
import at.ftw.mabs.ui.AugmentedRealityActivity;

import com.google.zxing.ResultPoint;

public class AugmentedView extends View {
	static final String			TAG				= "MABS/AugmentedView";
	static final int[]			SCANNER_ALPHA	= { 0, 64, 128, 192, 255, 192, 128, 64 };
	static final long			ANIMATION_DELAY	= 100L;

	final Paint					paint;
	Point[]						resultPoints;
	boolean						barcodeFound	= false;

	Timer						resetTimer;
	AugmentedRealityActivity	activity		= null;

	// This constructor is used when the class is built from an XML resource.
	public AugmentedView(Context context, AttributeSet attrs) {
		super(context, attrs);

		paint = new Paint();

		paint.setStrokeWidth(4.0f);
		paint.setColor(Color.BLACK);
		paint.setAlpha(100);
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (barcodeFound) {
			if (resultPoints != null) {
				if (resultPoints.length == 2) {
					Rect barcodeBorder = new Rect(resultPoints[0].x,
													resultPoints[0].y,
													resultPoints[1].x,
													resultPoints[1].y);

					canvas.drawRect(barcodeBorder, paint);
				}
			}
		} else {
			canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
		}
	}

	/**
	 * Augment the screen with the downloaded information about the barcode.
	 * 
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void drawResult(ResultPoint[] rawResultPoints) {
		if (resetTimer != null)
			resetTimer.cancel();

		resultPoints = getRectangularResultPoints(CameraManager.get().convertResultPoints(
				rawResultPoints));

		barcodeFound = true;

		invalidate();

		resetTimer = new Timer();
		resetTimer.schedule(new BarcodeResetTimer(), 3000);
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

	/**
	 * Timer that resets the info layer.
	 */
	class BarcodeResetTimer extends TimerTask {
		@Override
		public void run() {
			barcodeFound = false;
			activity.resetStatusView();

			this.cancel();
		}
	}

	public void setActivity(AugmentedRealityActivity activity) {
		this.activity = activity;
	}
}
