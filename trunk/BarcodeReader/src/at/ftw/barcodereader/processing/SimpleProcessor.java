package at.ftw.barcodereader.processing;

import at.ftw.barcodereader.image.Bitmap;

public class SimpleProcessor implements IBarCodeProcessor {
	final int	MODULE_COUNT	= 95;

	// TODO create special NotDecodableEception?
	/**
	 * Segments a barcode image and tries to decode it. Use the magicNumber to
	 * influence the decode process and the results.
	 */
	public byte[] process(Bitmap bitmap, int magicNumber) throws Exception {
		return decode(bitmap, magicNumber);
	}

	/**
	 * Segments an barcode image and tries to decode it.
	 * 
	 * @throws Exception
	 */
	public byte[] process(Bitmap bitmap) throws Exception {
		return decode(bitmap, 128);
	}

	/**
	 * Devides the bitmap into 95 modules and tries to interpret the color value
	 * of each module.
	 * 
	 * @param bitmap
	 * @param magicNumber
	 *            Is used for deciding if it's a black or white bar. If the mean
	 *            value of the module is below the magicNumber, it is
	 *            interpreted as black, if it's above then it's interpreted as
	 *            white.
	 * @return
	 * @throws Exception
	 */
	byte[] decode(Bitmap bitmap, int magicNumber) throws Exception {
		byte[] modules = new byte[95];

		if (bitmap.getWidth() > 95) {
			// TODO scaling greyscale would yield better results?
			scale(bitmap);
		} else {
			throw new Exception();
		}

		float moduleWidth = (float) bitmap.getWidth() / 95;

		// if (moduleWidth - (bitmap.getWidth() / 95) != 0) {
		// bitmap.scale(95 * (int) moduleWidth, bitmap.getHeight());
		// }

		// TODO possible usage of threads?yie
		for (int i = 0; i < MODULE_COUNT; i++) {
			modules[i] = getModule(i, (int) moduleWidth, bitmap, magicNumber);
		}

		return modules;
	}

	void scale(Bitmap image) {
		int moduleWidth = image.getWidth() / 95;
		// moduleWidth /= 4;
		image.scale(95 * moduleWidth, image.getHeight());
	}

	byte getModule(int number, int width, Bitmap image, int blackWhiteLimit) {
		int pixelSum = 0;
		int begin = number * width;

		for (int x = 0; x < width; x++) {
			pixelSum += image.getPixel(begin + x, 0).getBlue();
		}

		if (pixelSum > 0) {
			int tmpSum = pixelSum / width;

			// this one is the thing you can use to tweak the transformation
			// from pixels to black and white modules
			if (tmpSum < blackWhiteLimit) {
				return 0;
			} else {
				return 1;
			}
		} else {
			return 0;
		}
	}
}
