/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.reader;

import java.util.Hashtable;

import com.google.zxing.common.BarcodeFormat;
import com.google.zxing.common.BinaryBitmap;
import com.google.zxing.common.BitArray;
import com.google.zxing.common.DecodeHintType;
import com.google.zxing.common.ReaderException;
import com.google.zxing.result.Result;
import com.google.zxing.result.ResultMetadataType;
import com.google.zxing.result.ResultPoint;

/**
 * <p>
 * Implements decoding of the EAN-13 format.
 * </p>
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 * @author alasdair@google.com (Alasdair Mackintosh)
 */
public final class EAN13Reader {

	private static final int	INTEGER_MATH_SHIFT					= 8;
	static final int			PATTERN_MATCH_RESULT_SCALE_FACTOR	= 1 << INTEGER_MATH_SHIFT;

	private static final int	MAX_AVG_VARIANCE					= (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.42f);
	private static final int	MAX_INDIVIDUAL_VARIANCE				= (int) (PATTERN_MATCH_RESULT_SCALE_FACTOR * 0.7f);

	/**
	 * Start/end guard pattern.
	 */
	static final int[]			START_END_PATTERN					= { 1, 1, 1, };

	/**
	 * Pattern marking the middle of a UPC/EAN pattern, separating the two
	 * halves.
	 */
	static final int[]			MIDDLE_PATTERN						= { 1, 1, 1, 1, 1 };

	/**
	 * "Odd", or "L" patterns used to encode UPC/EAN digits.
	 */
	static final int[][]		L_PATTERNS							= {
																	{ 3, 2, 1, 1 }, // 0
			{ 2, 2, 2, 1 }, // 1
			{ 2, 1, 2, 2 }, // 2
			{ 1, 4, 1, 1 }, // 3
			{ 1, 1, 3, 2 }, // 4
			{ 1, 2, 3, 1 }, // 5
			{ 1, 1, 1, 4 }, // 6
			{ 1, 3, 1, 2 }, // 7
			{ 1, 2, 1, 3 }, // 8
			{ 3, 1, 1, 2 }											// 9
																	};

	/**
	 * As above but also including the "even", or "G" patterns used to encode
	 * UPC/EAN digits.
	 */
	static final int[][]		L_AND_G_PATTERNS;

	static {
		L_AND_G_PATTERNS = new int[20][];
		for (int i = 0; i < 10; i++) {
			L_AND_G_PATTERNS[i] = L_PATTERNS[i];
		}
		for (int i = 10; i < 20; i++) {
			int[] widths = L_PATTERNS[i - 10];
			int[] reversedWidths = new int[widths.length];
			for (int j = 0; j < widths.length; j++) {
				reversedWidths[j] = widths[widths.length - j - 1];
			}
			L_AND_G_PATTERNS[i] = reversedWidths;
		}
	}

	private final StringBuffer	decodeRowStringBuffer;

	// For an EAN-13 barcode, the first digit is represented by the parities
	// used
	// to encode the next six digits, according to the table below. For example,
	// if the barcode is 5 123456 789012 then the value of the first digit is
	// signified by using odd for '1', even for '2', even for '3', odd for '4',
	// odd for '5', and even for '6'. See http://en.wikipedia.org/wiki/EAN-13
	//
	// Parity of next 6 digits
	// Digit 0 1 2 3 4 5
	// 0 Odd Odd Odd Odd Odd Odd
	// 1 Odd Odd Even Odd Even Even
	// 2 Odd Odd Even Even Odd Even
	// 3 Odd Odd Even Even Even Odd
	// 4 Odd Even Odd Odd Even Even
	// 5 Odd Even Even Odd Odd Even
	// 6 Odd Even Even Even Odd Odd
	// 7 Odd Even Odd Even Odd Even
	// 8 Odd Even Odd Even Even Odd
	// 9 Odd Even Even Odd Even Odd
	//
	// Note that the encoding for '0' uses the same parity as a UPC barcode.
	// Hence
	// a UPC barcode can be converted to an EAN-13 barcode by prepending a 0.
	//
	// The encoding is represented by the following array, which is a bit
	// pattern
	// using Odd = 0 and Even = 1. For example, 5 is represented by:
	//
	// Odd Even Even Odd Odd Even
	// in binary:
	// 0 1 1 0 0 1 == 0x19
	//
	static final int[]			FIRST_DIGIT_ENCODINGS				= {
																	0x00, 0x0B, 0x0D, 0xE, 0x13, 0x19, 0x1C, 0x15,
																	0x16, 0x1A
																			};

	private final int[]			decodeMiddleCounters;

	public EAN13Reader() {
		decodeMiddleCounters = new int[4];
		decodeRowStringBuffer = new StringBuffer(20);
	}

	public final Result decode(BinaryBitmap image) throws ReaderException {
		return decode(image, null);
	}

	public final Result decode(BinaryBitmap image, Hashtable hints) throws ReaderException {
		try {
			return doDecode(image, hints);
		} catch (ReaderException re) {
			boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
			if (tryHarder && image.isRotateSupported()) {
				BinaryBitmap rotatedImage = image.rotateCounterClockwise();
				Result result = doDecode(rotatedImage, hints);
				// Record that we found it rotated 90 degrees CCW / 270 degrees
				// CW
				Hashtable metadata = result.getResultMetadata();
				int orientation = 270;
				if (metadata != null && metadata.containsKey(ResultMetadataType.ORIENTATION)) {
					// But if we found it reversed in doDecode(), add in that
					// result here:
					orientation = (orientation +
							((Integer) metadata.get(ResultMetadataType.ORIENTATION)).intValue()) % 360;
				}
				result.putMetadata(ResultMetadataType.ORIENTATION, new Integer(orientation));
				return result;
			} else {
				throw re;
			}
		}
	}

	/**
	 * We're going to examine rows from the middle outward, searching
	 * alternately above and below the middle, and farther out each time.
	 * rowStep is the number of rows between each successive attempt above and
	 * below the middle. So we'd scan row middle, then middle - rowStep, then
	 * middle + rowStep, then middle - (2 * rowStep), etc. rowStep is bigger as
	 * the image is taller, but is always at least 1. We've somewhat arbitrarily
	 * decided that moving up and down by about 1/16 of the image is pretty
	 * good; we try more of the image if "trying harder".
	 * 
	 * @param image
	 *            The image to decode
	 * @param hints
	 *            Any hints that were requested
	 * @return The contents of the decoded barcode
	 * @throws ReaderException
	 *             Any spontaneous errors which occur
	 */
	private Result doDecode(BinaryBitmap image, Hashtable hints) throws ReaderException {
		int width = image.getWidth();
		int height = image.getHeight();
		BitArray row = new BitArray(width);

		int middle = height >> 1;
		boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
		int rowStep = Math.max(1, height >> (tryHarder ? 7 : 4));
		int maxLines;
		if (tryHarder) {
			maxLines = height; // Look at the whole image, not just the center
		} else {
			maxLines = 9; // Nine rows spaced 1/16 apart is roughly the middle
			// half of the image
		}

		for (int x = 0; x < maxLines; x++) {

			// Scanning from the middle out. Determine which row we're looking
			// at next:
			int rowStepsAboveOrBelow = (x + 1) >> 1;
			boolean isAbove = (x & 0x01) == 0; // i.e. is x even?
			int rowNumber = middle + rowStep * (isAbove ? rowStepsAboveOrBelow : -rowStepsAboveOrBelow);
			if (rowNumber < 0 || rowNumber >= height) {
				// Oops, if we run off the top or bottom, stop
				break;
			}

			// Estimate black point for this row and load it:
			try {
				row = image.getBlackRow(rowNumber, row);
			} catch (ReaderException re) {
				continue;
			}

			// While we have the image data in a BitArray, it's fairly cheap to
			// reverse it in place to
			// handle decoding upside down barcodes.
			for (int attempt = 0; attempt < 2; attempt++) {
				if (attempt == 1) { // trying again?
					row.reverse(); // reverse the row and continue
				}
				try {
					// Look for a barcode
					Result result = decodeRow(rowNumber, row, hints);
					// We found our barcode
					if (attempt == 1) {
						// But it was upside down, so note that
						result.putMetadata(ResultMetadataType.ORIENTATION, new Integer(180));
						// And remember to flip the result points horizontally.
						ResultPoint[] points = result.getResultPoints();
						points[0] = new ResultPoint(width - points[0].getX() - 1, points[0].getY());
						points[1] = new ResultPoint(width - points[1].getX() - 1, points[1].getY());
					}
					return result;
				} catch (ReaderException re) {
					// continue -- just couldn't decode this row
				}
			}
		}

		throw ReaderException.getInstance();
	}

	protected int decodeMiddle(BitArray row, int[] startRange, StringBuffer resultString) throws ReaderException {
		int[] counters = decodeMiddleCounters;
		counters[0] = 0;
		counters[1] = 0;
		counters[2] = 0;
		counters[3] = 0;
		int end = row.getSize();
		int rowOffset = startRange[1];

		int lgPatternFound = 0;

		for (int x = 0; x < 6 && rowOffset < end; x++) {
			int bestMatch = decodeDigit(row, counters, rowOffset, L_AND_G_PATTERNS);
			resultString.append((char) ('0' + bestMatch % 10));
			for (int i = 0; i < counters.length; i++) {
				rowOffset += counters[i];
			}
			if (bestMatch >= 10) {
				lgPatternFound |= 1 << (5 - x);
			}
		}

		determineFirstDigit(resultString, lgPatternFound);

		int[] middleRange = findGuardPattern(row, rowOffset, true, MIDDLE_PATTERN);
		rowOffset = middleRange[1];

		for (int x = 0; x < 6 && rowOffset < end; x++) {
			int bestMatch = decodeDigit(row, counters, rowOffset, L_PATTERNS);
			resultString.append((char) ('0' + bestMatch));
			for (int i = 0; i < counters.length; i++) {
				rowOffset += counters[i];
			}
		}

		return rowOffset;
	}

	/**
	 * @param row
	 *            row of black/white values to search
	 * @param rowOffset
	 *            position to start search
	 * @param whiteFirst
	 *            if true, indicates that the pattern specifies
	 *            white/black/white/... pixel counts, otherwise, it is
	 *            interpreted as black/white/black/...
	 * @param pattern
	 *            pattern of counts of number of black and white pixels that are
	 *            being searched for as a pattern
	 * @return start/end horizontal offset of guard pattern, as an array of two
	 *         ints
	 * @throws ReaderException
	 *             if pattern is not found
	 */
	static int[] findGuardPattern(BitArray row, int rowOffset, boolean whiteFirst, int[] pattern)
			throws ReaderException {
		int patternLength = pattern.length;
		int[] counters = new int[patternLength];
		int width = row.getSize();
		boolean isWhite = false;
		while (rowOffset < width) {
			isWhite = !row.get(rowOffset);
			if (whiteFirst == isWhite) {
				break;
			}
			rowOffset++;
		}

		int counterPosition = 0;
		int patternStart = rowOffset;
		for (int x = rowOffset; x < width; x++) {
			boolean pixel = row.get(x);
			if (pixel ^ isWhite) {
				counters[counterPosition]++;
			} else {
				if (counterPosition == patternLength - 1) {
					if (patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE) < MAX_AVG_VARIANCE) {
						return new int[] { patternStart, x };
					}
					patternStart += counters[0] + counters[1];
					for (int y = 2; y < patternLength; y++) {
						counters[y - 2] = counters[y];
					}
					counters[patternLength - 2] = 0;
					counters[patternLength - 1] = 0;
					counterPosition--;
				} else {
					counterPosition++;
				}
				counters[counterPosition] = 1;
				isWhite ^= true; // isWhite = !isWhite;
			}
		}
		throw ReaderException.getInstance();
	}

	BarcodeFormat getBarcodeFormat() {
		return BarcodeFormat.EAN_13;
	}

	/**
	 * Based on pattern of odd-even ('L' and 'G') patterns used to encoded the
	 * explicitly-encoded digits in a barcode, determines the implicitly encoded
	 * first digit and adds it to the result string.
	 * 
	 * @param resultString
	 *            string to insert decoded first digit into
	 * @param lgPatternFound
	 *            int whose bits indicates the pattern of odd/even L/G patterns
	 *            used to encode digits
	 * @throws ReaderException
	 *             if first digit cannot be determined
	 */
	private static void determineFirstDigit(StringBuffer resultString, int lgPatternFound) throws ReaderException {
		for (int d = 0; d < 10; d++) {
			if (lgPatternFound == FIRST_DIGIT_ENCODINGS[d]) {
				resultString.insert(0, (char) ('0' + d));
				return;
			}
		}
		throw ReaderException.getInstance();
	}

	/**
	 * Attempts to decode a single UPC/EAN-encoded digit.
	 * 
	 * @param row
	 *            row of black/white values to decode
	 * @param counters
	 *            the counts of runs of observed black/white/black/... values
	 * @param rowOffset
	 *            horizontal offset to start decoding from
	 * @param patterns
	 *            the set of patterns to use to decode -- sometimes different
	 *            encodings for the digits 0-9 are used, and this indicates the
	 *            encodings for 0 to 9 that should be used
	 * @return horizontal offset of first pixel beyond the decoded digit
	 * @throws ReaderException
	 *             if digit cannot be decoded
	 */
	static int decodeDigit(BitArray row, int[] counters, int rowOffset, int[][] patterns)
			throws ReaderException {
		recordPattern(row, rowOffset, counters);
		int bestVariance = MAX_AVG_VARIANCE; // worst variance we'll accept
		int bestMatch = -1;
		int max = patterns.length;
		for (int i = 0; i < max; i++) {
			int[] pattern = patterns[i];
			int variance = patternMatchVariance(counters, pattern, MAX_INDIVIDUAL_VARIANCE);
			if (variance < bestVariance) {
				bestVariance = variance;
				bestMatch = i;
			}
		}
		if (bestMatch >= 0) {
			return bestMatch;
		} else {
			throw ReaderException.getInstance();
		}
	}

	/**
	 * Records the size of successive runs of white and black pixels in a row,
	 * starting at a given point. The values are recorded in the given array,
	 * and the number of runs recorded is equal to the size of the array. If the
	 * row starts on a white pixel at the given start point, then the first
	 * count recorded is the run of white pixels starting from that point;
	 * likewise it is the count of a run of black pixels if the row begin on a
	 * black pixels at that point.
	 * 
	 * @param row
	 *            row to count from
	 * @param start
	 *            offset into row to start at
	 * @param counters
	 *            array into which to record counts
	 * @throws ReaderException
	 *             if counters cannot be filled entirely from row before running
	 *             out of pixels
	 */
	static void recordPattern(BitArray row, int start, int[] counters) throws ReaderException {
		int numCounters = counters.length;
		for (int i = 0; i < numCounters; i++) {
			counters[i] = 0;
		}
		int end = row.getSize();
		if (start >= end) {
			throw ReaderException.getInstance();
		}
		boolean isWhite = !row.get(start);
		int counterPosition = 0;
		int i = start;
		while (i < end) {
			boolean pixel = row.get(i);
			if (pixel ^ isWhite) { // that is, exactly one is true
				counters[counterPosition]++;
			} else {
				counterPosition++;
				if (counterPosition == numCounters) {
					break;
				} else {
					counters[counterPosition] = 1;
					isWhite ^= true; // isWhite = !isWhite; Is this too clever?
					// shorter byte code, no conditional
				}
			}
			i++;
		}
		// If we read fully the last section of pixels and filled up our
		// counters -- or filled
		// the last counter but ran off the side of the image, OK. Otherwise, a
		// problem.
		if (!(counterPosition == numCounters || (counterPosition == numCounters - 1 && i == end))) {
			throw ReaderException.getInstance();
		}
	}

	/**
	 * Determines how closely a set of observed counts of runs of black/white
	 * values matches a given target pattern. This is reported as the ratio of
	 * the total variance from the expected pattern proportions across all
	 * pattern elements, to the length of the pattern.
	 * 
	 * @param counters
	 *            observed counters
	 * @param pattern
	 *            expected pattern
	 * @param maxIndividualVariance
	 *            The most any counter can differ before we give up
	 * @return ratio of total variance between counters and pattern compared to
	 *         total pattern size, where the ratio has been multiplied by 256.
	 *         So, 0 means no variance (perfect match); 256 means the total
	 *         variance between counters and patterns equals the pattern length,
	 *         higher values mean even more variance
	 */
	static int patternMatchVariance(int[] counters, int[] pattern, int maxIndividualVariance) {
		int numCounters = counters.length;
		int total = 0;
		int patternLength = 0;
		for (int i = 0; i < numCounters; i++) {
			total += counters[i];
			patternLength += pattern[i];
		}
		if (total < patternLength) {
			// If we don't even have one pixel per unit of bar width, assume
			// this is too small
			// to reliably match, so fail:
			return Integer.MAX_VALUE;
		}
		// We're going to fake floating-point math in integers. We just need to
		// use more bits.
		// Scale up patternLength so that intermediate values below like
		// scaledCounter will have
		// more "significant digits"
		int unitBarWidth = (total << INTEGER_MATH_SHIFT) / patternLength;
		maxIndividualVariance = (maxIndividualVariance * unitBarWidth) >> INTEGER_MATH_SHIFT;

		int totalVariance = 0;
		for (int x = 0; x < numCounters; x++) {
			int counter = counters[x] << INTEGER_MATH_SHIFT;
			int scaledPattern = pattern[x] * unitBarWidth;
			int variance = counter > scaledPattern ? counter - scaledPattern : scaledPattern - counter;
			if (variance > maxIndividualVariance) {
				return Integer.MAX_VALUE;
			}
			totalVariance += variance;
		}
		return totalVariance / total;
	}

	public final Result decodeRow(int rowNumber, BitArray row, Hashtable hints)
			throws ReaderException {
		return decodeRow(rowNumber, row, findStartGuardPattern(row));
	}

	static int[] findStartGuardPattern(BitArray row) throws ReaderException {
		boolean foundStart = false;
		int[] startRange = null;
		int nextStart = 0;
		while (!foundStart) {
			startRange = findGuardPattern(row, nextStart, false, START_END_PATTERN);
			int start = startRange[0];
			nextStart = startRange[1];
			// Make sure there is a quiet zone at least as big as the start
			// pattern before the barcode.
			// If this check would run off the left edge of the image, do not
			// accept this barcode,
			// as it is very likely to be a false positive.
			int quietStart = start - (nextStart - start);
			if (quietStart >= 0) {
				foundStart = row.isRange(quietStart, start, false);
			}
		}
		return startRange;
	}

	int[] decodeEnd(BitArray row, int endStart) throws ReaderException {
		return findGuardPattern(row, endStart, false, START_END_PATTERN);
	}

	/**
	 * @return {@link #checkStandardUPCEANChecksum(String)}
	 */
	boolean checkChecksum(String s) throws ReaderException {
		return checkStandardUPCEANChecksum(s);
	}

	/**
	 * Computes the UPC/EAN checksum on a string of digits, and reports whether
	 * the checksum is correct or not.
	 * 
	 * @param s
	 *            string of digits to check
	 * @return true iff string of digits passes the UPC/EAN checksum algorithm
	 * @throws ReaderException
	 *             if the string does not contain only digits
	 */
	private static boolean checkStandardUPCEANChecksum(String s) throws ReaderException {
		int length = s.length();
		if (length == 0) {
			return false;
		}

		int sum = 0;
		for (int i = length - 2; i >= 0; i -= 2) {
			int digit = s.charAt(i) - '0';
			if (digit < 0 || digit > 9) {
				throw ReaderException.getInstance();
			}
			sum += digit;
		}
		sum *= 3;
		for (int i = length - 1; i >= 0; i -= 2) {
			int digit = s.charAt(i) - '0';
			if (digit < 0 || digit > 9) {
				throw ReaderException.getInstance();
			}
			sum += digit;
		}
		return sum % 10 == 0;
	}

	public final Result decodeRow(int rowNumber, BitArray row, int[] startGuardRange)
			throws ReaderException {
		StringBuffer result = decodeRowStringBuffer;
		result.setLength(0);
		int endStart = decodeMiddle(row, startGuardRange, result);
		int[] endRange = decodeEnd(row, endStart);

		// Make sure there is a quiet zone at least as big as the end pattern
		// after the barcode. The
		// spec might want more whitespace, but in practice this is the maximum
		// we can count on.
		int end = endRange[1];
		int quietEnd = end + (end - endRange[0]);
		if (quietEnd >= row.getSize() || !row.isRange(end, quietEnd, false)) {
			throw ReaderException.getInstance();
		}

		String resultString = result.toString();
		if (!checkChecksum(resultString)) {
			throw ReaderException.getInstance();
		}

		float left = (startGuardRange[1] + startGuardRange[0]) / 2.0f;
		float right = (endRange[1] + endRange[0]) / 2.0f;

		return new Result(resultString,
							null, // no natural byte representation for these
							// barcodes
							new ResultPoint[] {
									new ResultPoint(left, rowNumber),
									new ResultPoint(right, rowNumber) },
							getBarcodeFormat());
	}
}