package at.ftw.mabs.ui.infolayers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.Log;
import at.ftw.mabs.internet.AmazonAccess;

public class AmazonReviewLayer implements IInfoLayer {
	static final String	TAG	= "MABS/AmazonReviewLayer";
	AmazonAccess	amazonAccess;
	Paint				paint;
	Paint				fontPaint;

	Double				lastRating;
	String				lastBarcodeString;
	Bitmap				lastBarcodeBitmap;

	public AmazonReviewLayer() {
		amazonAccess = AmazonAccess.getInstance();

		paint = new Paint();
		paint.setStyle(Style.FILL);
		paint.setColor(Color.DKGRAY);
		paint.setAlpha(100);
		paint.setTextSize(12f);

		fontPaint = new Paint();
		fontPaint.setStyle(Paint.Style.FILL);
		fontPaint.setColor(Color.WHITE);
		fontPaint.setTextAlign(Align.CENTER);
		fontPaint.setTextSize(30);
	}

	@Override
	public Bitmap getInfoLayer(int width, int height) {
		return getInfoLayer(width, height, lastBarcodeString);
	}

	@Override
	public Bitmap getInfoLayer(int width, int height, String isbn) {
		if (!isbn.equals(lastBarcodeString)) {
			lastRating = amazonAccess.getRating(isbn);

			lastBarcodeBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);

			Canvas canvas = new Canvas(lastBarcodeBitmap);

			Rect background = new Rect(0, 0, width, height);
			canvas.drawRect(background, paint);

			String textToDraw = "";

			if (lastRating >= 0) {
				textToDraw = "Rating: " + lastRating;
			} else {
				textToDraw = "ISBN not found";
			}

			canvas.drawText(textToDraw, (width / 2), (height / 2), fontPaint);

			Log.v(TAG, "Rating: " + lastRating);
		}

		return lastBarcodeBitmap;
	}

	@Override
	public void setISBN(String isbn) {
		lastBarcodeString = isbn;
	}

}
