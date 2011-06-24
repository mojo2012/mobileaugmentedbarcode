package at.ftw.barcodereader.postprocessing;


public interface IBarCodePostProcessor {
	Result postprocess(byte[] modules);
}
