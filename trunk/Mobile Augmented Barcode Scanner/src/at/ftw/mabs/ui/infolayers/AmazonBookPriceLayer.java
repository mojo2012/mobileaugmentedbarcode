//Author: Matthias Fuchs (meister.fuchs@gmail.com

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

public class AmazonBookPriceLayer implements IInfoLayer {
	static final String	TAG	= "MABS/AmazonBookPriceLayer";
	AmazonAccess		amazonAccess;
	Paint				paint;
	Paint				fontPaint;
	Paint				smallFontPaint;

	String				lastPrice;
	String				lastBookTitle;
	String				lastBarcode;
	Bitmap				lastBarcodeBitmap;

	public AmazonBookPriceLayer() {
		amazonAccess = AmazonAccess.getInstance();

		paint = new Paint();
		paint.setStyle(Style.FILL);
		paint.setColor(Color.YELLOW);
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
			lastPrice = amazonAccess.getPrice(isbn);
			lastBookTitle = amazonAccess.getBookTitle(isbn);

			String textToDraw = "";

			if (!lastPrice.equals("")) {
				textToDraw = lastPrice;
			} else {
				textToDraw = "n/a";
			}

			if (lastBookTitle.equals("")) {
				lastBookTitle = "ISBN not found";
			}

			lastBarcodeBitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);

			Canvas canvas = new Canvas(lastBarcodeBitmap);
			Rect background = new Rect(0, 0, width, height);
			canvas.drawRect(background, paint);

			canvas.drawText(lastBookTitle, (width / 2), 20, smallFontPaint);
			canvas.drawText(textToDraw, (width / 2), (height / 2), fontPaint);

			Log.v(TAG, "Rating: " + lastPrice);
		}

		return lastBarcodeBitmap;
	}

	@Override
	public void setISBN(String isbn) {
		lastBarcode = isbn;
		lastBookTitle = "";
		lastBarcodeBitmap = null;
	}
}
