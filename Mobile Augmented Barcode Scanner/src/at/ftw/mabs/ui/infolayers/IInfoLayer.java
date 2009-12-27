package at.ftw.mabs.ui.infolayers;

import android.graphics.Canvas;

public interface IInfoLayer {

	public void setISBN(String ISBN);

	public Canvas getInfoLayer(int width, int height);
}
