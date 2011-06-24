package at.ftw.barcodereader.preprocessing;

import java.awt.Color;

import at.ftw.barcodereader.image.Bitmap;
import at.ftw.barcodereader.image.Bitmap.ColorValue;

public class SimplePreProcessor implements IBarCodePreProcessor {
	public SimplePreProcessor() {
	}

	/**
	 * Transformes and trims a single line of a barcode image. Try to play
	 * around with the magic number to optimize the results of the monochrome
	 * transformation.
	 */
	public Bitmap preprocess(Bitmap image, int magicNumber) {
		image.convertToGreyscaleImage();

		// this is the one thing you can tweak to otimize the transformation
		// from color to monochrome
		image.convertToMonochromeImage(magicNumber, ColorValue.blue);

		// TODO possible optimization: calculate the trim limits of the
		// monochrome image and then crop the greyscale image
		// ==> result of scaling greyscale images might be better
		// (in processing)
		crop(image);

		return image;
	}

	/**
	 * Transformes and trims a single line of a barcode image.
	 */
	public Bitmap preprocess(Bitmap image) {
		return preprocess(image, image.getLimit());
	}

	/**
	 * Calculates the coordinates of the border bars and trims the whole barcode
	 * line.
	 * 
	 * @param image
	 */
	void crop(Bitmap image) {
		int leftStartSymbol = 0;
		int rightEndSymbol = image.getWidth() - 1;
		int middle = image.getWidth() / 2;
		boolean whiteAreaFound = false;

		int y = (image.getHeight() / 2);

		// TODO possible usage of threads
		for (; leftStartSymbol < (middle - 1); leftStartSymbol++) {
			Color pixel = image.getPixel(leftStartSymbol, y);

			if (!whiteAreaFound) {
				if (pixel.getBlue() == 255) {
					whiteAreaFound = true;
				}
			} else {
				if (pixel.getBlue() == 0) {
					break;
				}
			}
		}

		whiteAreaFound = false;

		for (; rightEndSymbol > (middle + 1); rightEndSymbol--) {
			Color pixel = image.getPixel(rightEndSymbol, y);

			if (!whiteAreaFound) {
				if (pixel.getBlue() == 255) {
					whiteAreaFound = true;
				}
			} else {
				if (pixel.getBlue() == 0) {
					break;
				}
			}
		}

		image.cropImage(leftStartSymbol, 0, rightEndSymbol, image.getHeight());
	}
}
