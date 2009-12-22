package at.ftw.mabs.ui.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import at.ftw.mabs.R;
import at.ftw.mabs.camera.CameraManager;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

public class AugmentedView extends View {
	static final String	TAG				= "MABS/AugmentedView";
	static final int[]	SCANNER_ALPHA	= { 0, 64, 128, 192, 255, 192, 128, 64 };
	static final long	ANIMATION_DELAY	= 100L;

	final Paint			paint;
	final Rect			box;
	Result				result;
	final int			maskColor;
	final int			resultColor;
	final int			frameColor;
	int					scannerAlpha;

	// This constructor is used when the class is built from an XML resource.
	public AugmentedView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Initialize these once for performance rather than calling them every
		// time in onDraw().
		paint = new Paint();
		box = new Rect();
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		resultColor = resources.getColor(R.color.result_view);
		frameColor = resources.getColor(R.color.viewfinder_frame);
		scannerAlpha = 0;
	}

	@Override
	public void onDraw(Canvas canvas) {
		Rect frame = CameraManager.get().getFramingRect();

		if (frame == null) {
			return;
		}

		if (result != null) {
			ResultPoint[] points = result.getResultPoints();

			if (points.length == 2) {
				paint.setStrokeWidth(4.0f);
				paint.setColor(resultColor);

				canvas.drawLine(points[0].getX(), points[0].getY(),
						points[1].getX(),
						points[0].getY(), paint);

				canvas.drawLine(points[1].getX(), points[0].getY(),
						points[1].getX(),
						points[1].getY(), paint);

				canvas.drawLine(points[0].getX(), points[0].getY(),
						points[0].getX(),
						points[1].getY(), paint);

				canvas.drawLine(points[0].getX(), points[1].getY(), points[1].getX(),
						points[1].getY(), paint);
			}

			// box.set(0, 0, frame.width(), frame.height());

			// int width = result.getResultPoints()..getWidth();
			// int height = canvas.getHeight();

			// Draw the exterior (i.e. outside the framing rect) darkened
			// paint.setColor(resultBitmap != null ? resultColor : maskColor);
			// box.set(0, 0, width, frame.top);
			// canvas.drawRect(box, paint);
			// box.set(0, frame.top, frame.left, frame.bottom + 1);
			// canvas.drawRect(box, paint);
			// box.set(frame.right + 1, frame.top, width, frame.bottom + 1);
			// canvas.drawRect(box, paint);
			// box.set(0, frame.bottom + 1, width, height);
			// canvas.drawRect(box, paint);
			//
			// if (result != null) {
			// // Draw the opaque result bitmap over the scanning rectangle
			// paint.setAlpha(255);
			// // canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
			// } else {
			// // Draw a two pixel solid black border inside the framing rect
			// paint.setColor(frameColor);
			// box.set(frame.left, frame.top, frame.right + 1, frame.top + 2);
			// canvas.drawRect(box, paint);
			// box.set(frame.left, frame.top + 2, frame.left + 2, frame.bottom -
			// 1);
			// canvas.drawRect(box, paint);
			// box.set(frame.right - 1, frame.top, frame.right + 1, frame.bottom
			// -
			// 1);
			// canvas.drawRect(box, paint);
			// box.set(frame.left, frame.bottom - 1, frame.right + 1,
			// frame.bottom +
			// 1);
			// canvas.drawRect(box, paint);
			//
			// paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
			// scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
			// int middle = frame.height() / 2 + frame.top;
			// box.set(frame.left + 2, middle - 1, frame.right - 1, middle + 2);
			// canvas.drawRect(box, paint);
			//
			// // Request another update at the animation interval, but only
			// // repaint the laser line,
			// // not the entire viewfinder mask.

		}
	}

	/**
	 * Draw a bitmap with the result points highlighted instead of the live
	 * scanning display.
	 * 
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void drawResult(Result rawResult) {
		result = rawResult;
		invalidate();
	}
}
