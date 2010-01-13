//Author: Matthias Fuchs (meister.fuchs@gmail.com

package at.ftw.mabs.ui.views;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import at.ftw.mabs.R;
import at.ftw.mabs.camera.CameraManager;
import at.ftw.mabs.ui.infolayers.IInfoLayer;

import com.google.zxing.result.Result;
import com.google.zxing.result.ResultPoint;

public class AugmentedView extends View {
	static final String	TAG				= "MABS/AugmentedView";

	final Paint			paint;
	final Rect			outerBox;
	final Rect			innerBox;
	boolean				showFocusRect	= false;

	MediaPlayer			mediaPlayer;
	static final float	BEEP_VOLUME		= 0.10f;
	Timer				resetTimer;

	IInfoLayer			infoLayer;
	Bitmap				infoLayerBitmap	= null;
	boolean				barcodeFound	= false;
	Point[]				resultPoints;

	// This constructor is used when the class is built from an XML resource.
	public AugmentedView(Context context, AttributeSet attrs) {
		super(context, attrs);

		paint = new Paint();
		paint.setStrokeWidth(2.0f);
		paint.setAlpha(100);
		paint.setColor(Color.WHITE);

		outerBox = new Rect();
		innerBox = new Rect();

		initBeepSound();
	}

	@Override
	public void onDraw(Canvas canvas) {
		Rect frame = CameraManager.get().getFramingRect();

		if (frame != null) {
			if (showFocusRect)
				drawFocusRect(canvas, frame);

			if (barcodeFound) {
				paint.setStyle(Style.FILL);

				if (infoLayerBitmap == null) {
					playBeepSoundAndVibrate();
				}

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

				if (infoLayerBitmap != null) {
					canvas.drawBitmap(infoLayerBitmap, x, y, paint);
					startTimer(5);
				}
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
		outerBox.set(frame.left + 79, frame.top + 79, frame.right - 79, frame.bottom - 79);
		canvas.drawRect(outerBox, paint);

		paint.setColor(Color.WHITE);
		innerBox.set(frame.left + 81, frame.top + 81, frame.right - 81, frame.bottom - 81);
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

	void startTimer(int seconds) {
		if (resetTimer != null)
			resetTimer.cancel();

		resetTimer = new Timer();
		resetTimer.schedule(new BarcodeResetTimerTask(), seconds * 1000);
	}

	/**
	 * Augment the screen with the downloaded information about the barcode.
	 * Draws the overlay to the given result points.
	 * 
	 * @param barcode
	 *            An image of the decoded barcode.
	 */
	public void setBarcode(Result result, boolean trackBarcode) {
		resultPoints = null;

		if (trackBarcode) {
			resultPoints = getRectangularResultPoints(result.getResultPoints());
		}

		infoLayer.setISBN(result.getText());

		barcodeFound = true;

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
	 * Creates the beep MediaPlayer in advance so that the sound can be
	 * triggered with the least latency possible.
	 */
	void initBeepSound() {
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);

		try {
			mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(),
					file.getLength());
			file.close();
			mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
			mediaPlayer.prepare();
		} catch (IOException e) {
			mediaPlayer = null;
		}
	}

	void playBeepSoundAndVibrate() {
		if (mediaPlayer != null) {
			mediaPlayer.start();
		}
	}

	/**
	 * Timer that resets the info layer.
	 */
	class BarcodeResetTimerTask extends TimerTask {
		@Override
		public void run() {
			barcodeFound = false;
			infoLayerBitmap = null;
			resetTimer.cancel();
			cancel();

			Log.v(TAG, "Resetting augmented view.");

			postInvalidate();
		}
	}
}
