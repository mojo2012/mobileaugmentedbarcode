package at.ftw.barcodereader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import at.ftw.barcodereader.image.Bitmap;
import at.ftw.barcodereader.postprocessing.IBarCodePostProcessor;
import at.ftw.barcodereader.postprocessing.Result;
import at.ftw.barcodereader.postprocessing.SimplePostProcessor;
import at.ftw.barcodereader.preprocessing.IBarCodePreProcessor;
import at.ftw.barcodereader.preprocessing.SimplePreProcessor;
import at.ftw.barcodereader.processing.IBarCodeProcessor;
import at.ftw.barcodereader.processing.SimpleProcessor;
import at.ftw.barcodereader.resultcombiner.ResultCombiner;

public class TestApplication {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IBarCodePreProcessor preprocessor = new SimplePreProcessor();
		IBarCodeProcessor processor = new SimpleProcessor();
		IBarCodePostProcessor postprocessor = new SimplePostProcessor();

		// contains all barcode sample images
		File dir = new File("/Users/ash/Projekte/Eclipse/BarcodeReader/res/");

		// iterate over all images
		String[] children = dir.list();
		if (children != null) {
			List<Result> foundEanCodes = null;

			for (String child : children) {
				String filename = dir + "/" + child;
				File file = new File(filename);

				if (!file.isDirectory()) {
					System.out.println("Trying: " + filename);

					foundEanCodes = new ArrayList<Result>();

					try {
						Bitmap barcodeImage = new Bitmap(filename);

						// splits the image in horizontal lines and processes
						// all lines
						// potential usage of threads here
						int tries = 20;
						int distance = barcodeImage.getHeight() / (tries + 1);

						for (int i = 0; i < tries; i++) {
							int x1 = 0;
							int y1 = (1 + i) * distance;
							int x2 = barcodeImage.getWidth();
							int y2 = y1 + 1;

							// cut out a single line of the barcode image and
							// create a new image
							Bitmap line = barcodeImage.getSubBitmap(x1, y1, x2, y2);

							// line.writeTo(filename + "_line_before.png");

							// preprocess a line of the barcode image
							// (monochrome conversion, trimming)
							line = preprocessor.preprocess(line, line.getLimit() + 58);

							// line.writeTo(filename + "_line_after.png");

							byte[] modules = null;
							try {
								// transform the image to 95 modules.
								// use the magic number to optimize the results
								modules = processor.process(line, 128);
							} catch (Exception e) {
								// System.out.println(e.getMessage());
								continue;
							}

							// decode the modules and add it to the array of
							// found barcodes (even those that are incorrect)
							foundEanCodes.add(postprocessor.postprocess(modules));
							// System.out.print(ean13code + ", ");
						}
						System.out.println();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			// take all the found barcodes and
			ResultCombiner combiner = new ResultCombiner();
			combiner.combineResults(foundEanCodes);
		}
	}
}
