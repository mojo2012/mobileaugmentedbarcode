//Author: Matthias Fuchs (meister.fuchs@gmail.com

package at.ftw.mabs.ui.infolayers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.Log;
import at.ftw.mabs.R;
import at.ftw.mabs.internet.AmazonAccess;

public class AmazonRatingLayer implements IInfoLayer {
	static final String	TAG	= "MABS/AmazonRatingLayer";
	AmazonAccess		amazonAccess;
	Paint				paint;
	Paint				fontPaint;
	Paint				smallFontPaint;

	Double				lastRating;
	String				lastBookTitle;
	String				lastBarcode;
	Bitmap				lastBarcodeBitmap;

	public AmazonRatingLayer() {
		amazonAccess = AmazonAccess.getInstance();

		paint = new Paint();
		paint.setStyle(Style.FILL);
		paint.setColor(Color.DKGRAY);
		paint.setAlpha(100);
		paint.setTextSize(12);

		fontPaint = new Paint();
		fontPaint.setStyle(Paint.Style.FILL);
		fontPaint.setColor(Color.WHITE);
		fontPaint.setTextAlign(Align.CENTER);
		fontPaint.setTextSize(30);

		smallFontPaint = new Paint();
		smallFontPaint.setStyle(Paint.Style.FILL);
		smallFontPaint.setColor(Color.WHITE);
		smallFontPaint.setTextAlign(Align.CENTER);
		smallFontPaint.setTextSize(12);
	}

	@Override
	public Bitmap getInfoLayer(int width, int height) {
		if (lastBarcodeBitmap == null)
			return getInfoLayer(width, height, lastBarcode);
		else
			return lastBarcodeBitmap;
	}

	@Override
	public Bitmap getInfoLayer(int width, int height, String isbn) {
		if ((!isbn.equals(lastBarcode)) || (lastBarcodeBitmap == null)) {
			lastBarcode = isbn;
			lastRating = amazonAccess.getRating(isbn);
			lastBookTitle = amazonAccess.getBookTitle(isbn);

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

			canvas.drawText(lastBookTitle, (width / 2), 20, smallFontPaint);
			canvas.drawText(textToDraw, (width / 2), (height / 2), fontPaint);

			Log.v(TAG, "Rating: " + lastRating);
		}

		return lastBarcodeBitmap;
	}

	Bitmap generateRatingStars(double rating) {
		Bitmap starFull = BitmapFactory.decodeResource(null, R.drawable.star_full);

		Bitmap ratingStars = Bitmap.createBitmap(24 * 5, 24, Config.ARGB_8888);

		return ratingStars;
	}

	@Override
	public void setISBN(String isbn) {
		lastBarcode = isbn;
		lastBookTitle = "";
		lastBarcodeBitmap = null;
	}
}
