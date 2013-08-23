package de.oglimmer.ifcdb.util;

import java.util.Random;

/**
 * Creates a random string. It could be specified which character codes should be used.
 * 
 * @author Oli Zimpasser
 */
final public class RandomString {

	private static final String[] alphabet = { "Alfa", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel",
			"India", "Juliett", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo", "Sierra",
			"Tango", "Uniform", "Victor", "Whiskey", "Xray", "Yankee", "Zulu" };

	private RandomString() {
		// no code here
	}

	private static final Random RAN = new Random(System.currentTimeMillis());

	/**
	 * Creates a size byte long unicode string. All chars are > 32.
	 * 
	 * @param size
	 * @return
	 */
	public static String getRandomStringUnicode(final int size) {
		final StringBuilder buff = new StringBuilder(size);

		for (int i = 0; i < size; i++) {
			buff.append((char) (RAN.nextInt(65503) + 32));
		}
		return buff.toString();
	}

	public static byte[] getRandomBytes(final int size) {
		final byte[] buff = new byte[size];
		RAN.nextBytes(buff);
		return buff;
	}

	/**
	 * Creates a size byte long unicode string. All chars are between 32 and 255
	 * 
	 * @param size
	 * @return
	 */
	public static String getRandomString8Bit(final int size) {
		final StringBuilder buff = new StringBuilder(size);

		for (int i = 0; i < size; i++) {
			buff.append((char) (RAN.nextInt(223) + 32));
		}
		return buff.toString();
	}

	/**
	 * Creates a size byte long string. All chars are between a..z and A..Z and 0..9
	 * 
	 * @param size
	 * @return
	 */
	public static String getRandomStringASCII(final int size) {
		final StringBuilder buff = new StringBuilder(size);

		for (int i = 0; i < size; i++) {
			char nextChar;
			do {
				nextChar = (char) (RAN.nextInt(75) + 48);
			} while ((nextChar >= 58 && nextChar <= 64) || (nextChar >= 91 && nextChar <= 96));
			buff.append(nextChar);
		}
		return buff.toString();
	}

	/**
	 * Creates a size byte long unicode string. All chars are from the set "stringSet"
	 * 
	 * @param size
	 * @param stringSet
	 * @return
	 */
	public static String getRandomString(final int size, final String stringSet) {

		final StringBuilder buff = new StringBuilder(size);

		for (int i = 0; i < size; i++) {
			final char nextChar = stringSet.charAt(RAN.nextInt(stringSet.length()));
			buff.append(nextChar);
		}
		return buff.toString();
	}

	/**
	 * Creates a size byte long unicode string. All chars are 0..9 and a..f.
	 * 
	 * @param size
	 * @return
	 */
	public static String getRandomStringHex(final int size) {
		return getRandomString(size, "0123456789ABCDEF");
	}

	/**
	 * Creates a human readble string
	 * 
	 * @param size
	 * @return
	 */
	public static String getReadableString(int size) {
		final StringBuilder buff = new StringBuilder(size * 8);
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				buff.append('-');
			}
			int rand = RAN.nextInt(alphabet.length);
			buff.append(alphabet[rand]);
		}
		return buff.toString();
	}
}