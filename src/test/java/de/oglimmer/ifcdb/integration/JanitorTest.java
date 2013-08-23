package de.oglimmer.ifcdb.integration;

import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.ArrayComparisonFailure;

import de.oglimmer.ifcdb.DatabaseException;
import de.oglimmer.ifcdb.util.RandomString;

public class JanitorTest extends DBIntegrationBase {

	@Test
	public void janitorTest() throws InterruptedException, DatabaseException, SQLException {

		byte[] data = createBinary();

		String key = createSingleFile(data);

		archiveFile(data, key);

		data = updateFile(key);

		archiveFile(data, key);

	}

	private String createSingleFile(byte[] data) throws DatabaseException, SQLException {

		String key = driver.getSession().put(null, null, null, null, data);

		assertAgainstDb(key, 0);

		readAndAssert(data, key);
		return key;
	}

	private byte[] updateFile(String key) throws DatabaseException, SQLException, ArrayComparisonFailure {
		driver.getJanitor().stop();

		byte[] data = createBinary();

		String newKey = driver.getSession().put(key, null, null, null, data);
		Assert.assertEquals(key, newKey);

		assertAgainstDb(key, 0);

		readAndAssert(data, key);
		return data;
	}

	private void archiveFile(byte[] data, String key) throws SQLException, DatabaseException, ArrayComparisonFailure, InterruptedException {
		driver.getJanitor().start();
		Thread.sleep(getWaitTime());

		assertAgainstDb(key, 1);

		readAndAssert(data, key);
	}

	private void readAndAssert(byte[] data, String key) throws DatabaseException, ArrayComparisonFailure {
		byte[] buff = driver.getSession().get(key).getData();
		Assert.assertArrayEquals("read failed", data, buff);
	}

	private void assertAgainstDb(String key, int archived) throws SQLException {
		String sql = "select count(*) from Blobdata where archived=? and blobkey=? and relativeFilename " + (archived == 1 ? "not" : "")
				+ " like ?";
		Assert.assertEquals(new Integer(1), driver.getDatabaseOperations().getInt(sql, archived, key, "%" + key));
	}

	private byte[] createBinary() {
		return RandomString.getRandomBytes((int) (1024 * 100 * Math.random()) + 1);
	}

	private int getWaitTime() {
		int t = (int) ((driver.getConfig().getTimeMoveToArchive() * 1000 + driver.getConfig().getJanitorThreadWait() * 2) * 1.5);
		return t;
	}

}
