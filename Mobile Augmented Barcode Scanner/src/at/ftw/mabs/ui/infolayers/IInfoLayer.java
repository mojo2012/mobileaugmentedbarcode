package at.ftw.mabs.ui.infolayers;

import android.graphics.Bitmap;

public interface IInfoLayer {

	public void setISBN(String isbn);

	public Bitmap getInfoLayer(int width, int height);

	public Bitmap getInfoLayer(int width, int height, String isbn);
}
