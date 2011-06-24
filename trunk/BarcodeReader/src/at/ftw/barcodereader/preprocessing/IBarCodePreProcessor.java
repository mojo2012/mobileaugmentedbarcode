package at.ftw.barcodereader.preprocessing;

import at.ftw.barcodereader.image.Bitmap;

public interface IBarCodePreProcessor {
	Bitmap preprocess(Bitmap image);

	Bitmap preprocess(Bitmap image, int magicNumber);
}
