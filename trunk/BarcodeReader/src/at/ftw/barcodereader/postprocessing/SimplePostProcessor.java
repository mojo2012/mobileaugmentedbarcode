package at.ftw.barcodereader.postprocessing;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Dictionary;
import java.util.Hashtable;

public class SimplePostProcessor implements IBarCodePostProcessor {
	static final String			VALID_SYMBOLS	= "0123456789";

	// fontset A and B are used for the left half of the EAN code, fontset C is
	// use only for the right part.
	Dictionary<String, Byte>	fontSetA		= null;
	Dictionary<String, Byte>	fontSetB		= null;
	Dictionary<String, Byte>	fontSetC		= null;

	public SimplePostProcessor() {
		loadFontSets();
	}

	// TODO check if start, middle and end are correct
	/**
	 * Decodes the modules and returns the result (EAN code)
	 */
	public Result postprocess(byte[] modules) {
		byte[] start = getPartOfArray(modules, 0, 3);
		// byte[] middle = getPartOfArray(modules, 45, 5);
		// byte[] end = getPartOfArray(modules, 92, 3);
		String[] ean13content = new String[12];
		String[] firstDigitEncoding = new String[6];

		int firstModulePosition = start.length;

		// System.out.println("Modules: " + convertArrayToString(modules));

		String strDigit = "";

		for (int x = 0; x < ean13content.length; x++) {
			if (x < 6) {
				strDigit = convertArrayToString(getPartOfArray(modules, firstModulePosition + (x * 7), 7));

				// System.out.println(strDigit);

				if (fontSetA.get(strDigit) != null) {
					ean13content[x] = fontSetA.get(strDigit).toString();
					firstDigitEncoding[x] = "A";
				} else if (fontSetB.get(strDigit) != null) {
					ean13content[x] = fontSetB.get(strDigit).toString();
					firstDigitEncoding[x] = "B";
				} else {
					// System.out.println(strDigit);
					ean13content[x] = firstDigitEncoding[x] = "_";
				}

			} else {
				strDigit = convertArrayToString(getPartOfArray(modules, 50 + ((x - 6) * 7), 7));
				if (fontSetC.get(strDigit) != null) {
					ean13content[x] = fontSetC.get(strDigit).toString();
				} else {
					// System.out.println(strDigit);
					ean13content[x] = "_";
				}
			}
		}

		// converts the ean13 code array to a string
		String code = convertArrayToString(ean13content);

		// for (int c = 0; c < ean13content.length; c++) {
		// code += ean13content[c];
		// }

		// creates the 1st number of ean13 out of the number 2-6.
		String encoding = convertArrayToString(firstDigitEncoding);

		// for (int c = 0; c < firstDigitEncoding.length; c++) {
		// encoding += firstDigitEncoding[c];
		// }

		// combines the decoded first digit and the rest of the code
		code = decodeFirstDigit(code, encoding) + code;

		Result retVal = new Result();
		retVal.content = code;

		// check if the code is valid
		if (checkValidity(code)) {
			retVal.isValid = isChecksumOk(code);
		}

		// Return even invalid codes. ResultCombiner deals with that.
		return retVal;
	}

	/**
	 * Decodes the first digit out of the order of the fontsets of the digits
	 * 2-6.
	 * 
	 * @param code
	 * @param firstDigitEncoding
	 * @return
	 */
	String decodeFirstDigit(String code, String firstDigitEncoding) {
		String retVal = "";

		Dictionary<String, String> firstDigitCodeTable = new Hashtable<String, String>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader("/Users/ash/Projekte/Eclipse/BarcodeReader/specs/ean13_first_digit.csv"));

			String line = reader.readLine();

			while ((line = reader.readLine()) != null) {
				String[] lineArray = line.split(";");

				firstDigitCodeTable.put(lineArray[1], lineArray[0]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		retVal = firstDigitCodeTable.get(firstDigitEncoding);

		if (retVal == null) {
			retVal = "_";
		}

		return retVal;
	}

	String convertArrayToString(byte[] array) {
		String strDigit = "";
		for (byte b : array) {
			strDigit += b;
		}

		return strDigit;
	}

	String convertArrayToString(String[] array) {
		String str = "";

		for (int c = 0; c < array.length; c++) {
			str += array[c];
		}

		return str;
	}

	/**
	 * Returns a new array created out of another one.
	 * 
	 * @param modules
	 * @param start
	 * @param length
	 * @return
	 */
	byte[] getPartOfArray(byte[] modules, int start, int length) {
		byte[] part = new byte[length];

		for (byte b = 0; b < part.length; b++) {
			part[b] = -1;
		}

		// System.out.println(start + "-" + (start + length));

		for (int i = 0; (start + i) < (start + length); i++) {
			part[i] = modules[i + start];
		}

		return part;
	}

	/**
	 * Loads the fontsets stored in a textfile.
	 */
	void loadFontSets() {
		fontSetA = new Hashtable<String, Byte>();
		fontSetB = new Hashtable<String, Byte>();
		fontSetC = new Hashtable<String, Byte>();

		try {
			BufferedReader reader = new BufferedReader(new FileReader("/Users/ash/Projekte/Eclipse/BarcodeReader/specs/ean13.csv"));

			String line = reader.readLine();

			while ((line = reader.readLine()) != null) {
				String[] lineArray = line.split(";");

				fontSetA.put(lineArray[1], Byte.decode(lineArray[0]));
				fontSetB.put(lineArray[2], Byte.decode(lineArray[0]));
				fontSetC.put(lineArray[3], Byte.decode(lineArray[0]));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Checks if the code contains only valid symbols (0-9).
	 * 
	 * @param code
	 * @return
	 */
	boolean checkValidity(String code) {
		boolean retVal = true;

		for (int c = 0; c < code.length(); c++) {
			if (!VALID_SYMBOLS.contains(code.substring(c, c + 1))) {
				retVal = false;
				break;
			}
		}

		return retVal;
	}

	/**
	 * Checks if the checksum of a code is OK.
	 * 
	 * @param code
	 * @return
	 */
	boolean isChecksumOk(String code) {
		boolean retVal = false;
		int checksum = 0;

		boolean multiplyWithThree = false;

		for (byte b = 0; b < (code.length() - 1); b++) {
			byte digit = Byte.parseByte(code.substring(b, b + 1));

			if (multiplyWithThree) {
				checksum += digit * 3;
			} else {
				checksum += digit;
			}

			multiplyWithThree = !multiplyWithThree;
		}

		byte checkDigitFromCode = Byte.parseByte(code.substring(12, 13));
		byte mod = (byte) (checksum % 10);
		byte calculatedCheckDigit;

		if (mod != 0) {
			calculatedCheckDigit = (byte) (10 - mod);
		} else {
			calculatedCheckDigit = mod;
		}

		if (checkDigitFromCode == calculatedCheckDigit) {
			retVal = true;
		}

		return retVal;
	}
}
