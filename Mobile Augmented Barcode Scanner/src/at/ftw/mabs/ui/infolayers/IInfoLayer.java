package at.ftw.mabs.ui.infolayers;

import android.graphics.Bitmap;

public interface IInfoLayer {

	/**
	 * Set the ISBN of the book you want to get infos of.
	 * 
	 * @param isbn
	 */
	public void setISBN(String isbn);

	/**
	 * Returns a Bitmap with the contents/infos of the given ISBN.
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	public Bitmap getInfoLayer(int width, int height);

	/**
	 * Returns a Bitmap with the contents/infos of the given ISBN.
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	public Bitmap getInfoLayer(int width, int height, String isbn);
}
