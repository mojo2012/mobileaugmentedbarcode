package at.ftw.barcodereader.image;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ColorProfile {
	public int[]	histogram	= new int[256];

	public ColorProfile(BufferedImage image) {
		createHistogram(image);
	}

	void reset() {
		for (int n = 0; n < 256; n++) {
			histogram[n] = 0;
		}
	}

	/**
	 * Creates a histogram out of the given image. This is needed for the
	 * calculation of the low and high peak and the limit.
	 * 
	 * @param image
	 */
	void createHistogram(BufferedImage image) {
		reset();

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				Color pixel = new Color(image.getRGB(x, y));

				histogram[pixel.getBlue()] += 1;
			}
		}
	}

	/**
	 * Returns the peak in the left (darker) side of the histogram.
	 * 
	 * @return
	 */
	int getLowPeak() {
		return getPeak(0, 128);
	}

	/**
	 * Returns the peak in the right (bright) side of the histogram.
	 * 
	 * @return
	 */
	int getHighPeak() {
		return getPeak(128, 256);
	}

	/**
	 * Returns the maximum brightness in a given range range.
	 * 
	 * @param from
	 * @param to
	 * @return
	 */
	int getPeak(int from, int to) {
		int maxValue = 0;

		int tmp = 0;

		for (int n = from; n < to; n++) {
			if (histogram[n] > tmp) {
				maxValue = n;

				tmp = histogram[n];
			}
		}

		return maxValue;
	}

	/**
	 * Returns the limit that devides the dark from the bright half of the
	 * histogram.
	 * 
	 * @return
	 */
	public int getLimit() {
		return ((getLowPeak() + getHighPeak()) / 2);
	}

}