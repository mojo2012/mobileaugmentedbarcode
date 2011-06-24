package at.ftw.barcodereader.processing;

import at.ftw.barcodereader.image.Bitmap;

public interface IBarCodeProcessor {
	byte[] process(Bitmap bitmap) throws Exception;

	byte[] process(Bitmap bitmap, int magicNumber) throws Exception;
}
