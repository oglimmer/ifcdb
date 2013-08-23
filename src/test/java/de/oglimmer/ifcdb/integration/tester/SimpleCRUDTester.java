package de.oglimmer.ifcdb.integration.tester;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.DatabaseException;
import de.oglimmer.ifcdb.integration.cases.SimpleCRUDTestCases;
import de.oglimmer.ifcdb.util.RandomString;

public class SimpleCRUDTester {

	private static Logger log = LoggerFactory.getLogger(SimpleCRUDTestCases.class);

	public static void simpleMassInsert(InterfaceType interfaceType, int number, int size) throws DatabaseException {
		byte[] data = new byte[size];

		long timeGlo = System.currentTimeMillis();
		for (int i = 0; i < number; i++) {
			interfaceType.put(null, null, null, null, data);
		}
		log.info("time to write " + number + "/" + size + "b:" + (System.currentTimeMillis() - timeGlo));
	}

	public static void insert(InterfaceType interfaceType, int size) throws DatabaseException {
		insert(interfaceType, size, new String[0]);
	}

	public static void insert(InterfaceType interfaceType, String... ids) throws DatabaseException {
		insert(interfaceType, ids.length, ids);
	}

	private static String[] insert(InterfaceType interfaceType, int size, String... ids) throws DatabaseException {
		Assert.assertTrue(ids.length == 0 || size == ids.length);
		String[] data = new String[size];
		for (int i = 0; i < data.length; i++) {
			data[i] = RandomString.getRandomStringUnicode((int) (1024 * 15 * Math.random()) + 1);
		}
		String[] key = new String[data.length];
		for (int i = 0; i < data.length; i++) {
			long time = System.currentTimeMillis();
			key[i] = interfaceType.put(size == ids.length ? ids[i] : null, null, null, null, data[i].getBytes());
			log.debug("write:" + (System.currentTimeMillis() - time));
		}
		for (int i = 0; i < data.length; i++) {
			long time = System.currentTimeMillis();
			byte[] buff = interfaceType.get(key[i]);
			log.debug("read:" + (System.currentTimeMillis() - time));
			Assert.assertArrayEquals("read failed", data[i].getBytes(), buff);
		}
		return key;
	}

	public static void insertDelete(InterfaceType interfaceType, int size) throws DatabaseException {
		String[] keys = insert(interfaceType, size, new String[0]);
		for (String k : keys) {
			interfaceType.remove(k);
		}
		for (String k : keys) {
			byte[] buff = interfaceType.get(k);
			Assert.assertNull(buff);
		}
	}

	public static void updateEqual(InterfaceType interfaceType) throws DatabaseException {

		ByteArrayOutputStream outContent = new ByteArrayOutputStream();
		PrintStream originalPs = System.out;
		System.setOut(new PrintStream(outContent));

		byte[] data = RandomString.getRandomBytes(1024 * 1024);
		String key = interfaceType.put(null, null, null, null, data);

		byte[] buff = interfaceType.get(key);
		Assert.assertArrayEquals("read failed", data, buff);

		String newKey = interfaceType.put(key, null, null, null, buff);
		Assert.assertEquals(key, newKey);

		buff = interfaceType.get(key);
		Assert.assertArrayEquals("read failed", data, buff);

		System.setOut(originalPs);

		Assert.assertTrue(outContent.toString().contains("Binary hasn't changed, no update for " + newKey));

	}

}
