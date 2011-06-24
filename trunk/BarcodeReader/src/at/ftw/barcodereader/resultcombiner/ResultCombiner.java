package at.ftw.barcodereader.resultcombiner;

import java.util.List;

import at.ftw.barcodereader.postprocessing.Result;

public class ResultCombiner {

	public String combineResults(List<Result> results) {
		String eanCode = "";

		// TODO take wrong/not detected symbols and create a new code out of it?
		if (results != null) {
			for (Result result : results) {
				System.out.println(result.content);
			}
		}

		return eanCode;
	}

}
