package at.ftw.barcodereader.image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public final class Bitmap {
	BufferedImage	image		= null;
	int				imageType	= BufferedImage.TYPE_INT_RGB;

	public enum ColorValue {
		red,
		green,
		blue,
	}

	public Bitmap(String filename) throws IOException {
		readFrom(filename);
	}

	public Bitmap(BufferedImage image) {
		this.image = image;
		this.imageType = image.getType();
	}

	/**
	 * Scales the image to the new height and width.
	 * 
	 * @param width
	 * @param height
	 */
	public void scale(int width, int height) {
		BufferedImage scaledImage = new BufferedImage(width, height, imageType);

		Graphics2D g = scaledImage.createGraphics();
		AffineTransform at = AffineTransform.getScaleInstance((double) width / image.getWidth(), (double) height
				/ image.getHeight());

		g.drawRenderedImage(image, at);

		image = scaledImage;
	}

	/**
	 * Converts the image to a greyscale image. Uses default values for
	 * conversion.
	 */
	public void convertToGreyscaleImage() {
		imageType = BufferedImage.TYPE_BYTE_GRAY;

		BufferedImage temp = new BufferedImage(image.getWidth(), image.getHeight(), imageType);
		Graphics g = temp.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();

		image = temp;
	}

	/**
	 * Converts the image to monochrome. All values below the given
	 * blackWhiteLimit are converted to black, all above are converted to white.
	 * 
	 * @param color
	 *            specifies the color channel that is use by the conversion.
	 */
	public void convertToMonochromeImage(int blackWhiteLimit, ColorValue color) {
		imageType = BufferedImage.TYPE_BYTE_BINARY;

		int value = 0;

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				Color pixel = new Color(image.getRGB(x, y));

				if (color == ColorValue.red) {
					value = pixel.getRed();
				} else if (color == ColorValue.green) {
					value = pixel.getGreen();
				} else if (color == ColorValue.blue) {
					value = pixel.getBlue();
				}

				if (value < blackWhiteLimit)
					pixel = new Color(0, 0, 0);
				else
					pixel = new Color(255, 255, 255);

				image.setRGB(x, y, pixel.getRGB());
			}
		}
	}

	/**
	 * Converts the image to monochrome. Uses default limits for conversion.
	 */
	public void convertToMonochromeImage() {
		convertToMonochromeImage(getLimit(), ColorValue.blue);
	}

	/**
	 * Crops the image.
	 * 
	 * @param topleftX
	 * @param topleftY
	 * @param bottomRightX
	 * @param bottomRightY
	 */
	public void cropImage(int topleftX, int topleftY, int bottomRightX, int bottomRightY) {
		image = getPartOfBitmap(topleftX, topleftY, bottomRightX, bottomRightY);
	}

	/**
	 * Writes the current image to disk.
	 */
	public void writeTo(String filename) throws IOException {
		ImageIO.write(image, "JPG", new File(filename));
	}

	/**
	 * Reads an image from disk.
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public void readFrom(String filename) throws IOException {
		image = ImageIO.read(new File(filename));
	}

	public int getWidth() {
		return image.getWidth();
	}

	public int getHeight() {
		return image.getHeight();
	}

	/**
	 * Returns the color value of a certain pixel.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public Color getPixel(int x, int y) {
		return new Color(image.getRGB(x, y));
	}

	/**
	 * Set the color value of a pixel.
	 * 
	 * @param x
	 * @param y
	 * @param color
	 */
	public void setPixel(int x, int y, Color color) {
		image.setRGB(x, y, color.getRGB());
	}

	BufferedImage getPartOfBitmap(int topleftX, int topleftY, int bottomRightX, int bottomRightY) {
		BufferedImage subImage = new BufferedImage(bottomRightX - topleftX,
													bottomRightY - topleftY,
													imageType);

		Graphics graphics = subImage.getGraphics();

		graphics.drawImage(image, 0, 0, subImage.getWidth(), subImage.getHeight(), topleftX, topleftY,
				bottomRightX, bottomRightY, null);

		graphics.dispose();

		return subImage;
	}

	/**
	 * Creates a new image out of the existing one.
	 * 
	 * @param topleftX
	 * @param topleftY
	 * @param bottomRightX
	 * @param bottomRightY
	 * @return
	 */
	public Bitmap getSubBitmap(int topleftX, int topleftY, int bottomRightX, int bottomRightY) {
		BufferedImage bitmap = getPartOfBitmap(topleftX, topleftY, bottomRightX, bottomRightY);

		return new Bitmap(bitmap);
	}

	/**
	 * Returns the limit that devides the dark from the bright half of the
	 * histogram.
	 * 
	 * @return
	 */
	public int getLimit() {
		ColorProfile profile = new ColorProfile(image);

		return profile.getLimit();
	}
}
