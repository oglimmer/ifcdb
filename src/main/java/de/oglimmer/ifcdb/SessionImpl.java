package de.oglimmer.ifcdb;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.api.Session;
import de.oglimmer.ifcdb.records.Blobdata;
import de.oglimmer.ifcdb.records.ByteResult;
import de.oglimmer.ifcdb.stats.StatisticsMBean;

/**
 * Implements the main business logic. Methods should operate on the file system first and database last. A
 * public method must not call two or more methods on the DatabaseOperations interface in sequence (to be
 * transactional).
 * 
 * @author Oli Zimpasser
 * 
 */
public class SessionImpl implements Session, StatisticsMBean {

	private Logger log = LoggerFactory.getLogger(SessionImpl.class);

	private Driver driver;

	private int numberOfReads;
	private int numberOfCreate;
	private int numberOfUpdate;
	private int numberOfRemove;

	private Set<String> currentGet = Collections.synchronizedSet(new HashSet<String>());
	private Set<String> currentPut = Collections.synchronizedSet(new HashSet<String>());
	private Set<String> currentRemove = Collections.synchronizedSet(new HashSet<String>());

	@Inject
	public SessionImpl(Driver driver) {
		this.driver = driver;
	}

	public String put(String key, String mimetype, String prefix, String postfix, byte[] data) throws DatabaseException {
		try {
			if (null == key) {
				String threadName = Thread.currentThread().getName();
				currentPut.add(threadName);
				try {
					key = putNoKey(mimetype, prefix, postfix, data);
				} finally {
					currentPut.remove(threadName);
				}
			} else {
				currentPut.add(key);
				try (Closeable c = driver.getLockTable().lock(key)) {
					currentPut.add(key);
					putKey(key, mimetype, data);
				} finally {
					currentPut.remove(key);
				}
			}
			return key;
		} catch (SQLException | IOException e) {
			throw new DatabaseException(e);
		}
	}

	public ByteResult get(String key) throws DatabaseException {
		if (key == null || key.trim().isEmpty()) {
			return null;
		}
		try {
			currentGet.add(key);
			try (Closeable c = driver.getLockTable().lock(key)) {
				currentGet.add(key);
				ByteResult result = driver.getFileOperations().read(key);
				numberOfReads++;
				return result;
			} finally {
				currentGet.remove(key);
			}
		} catch (IOException | SQLException e) {
			throw new DatabaseException(e);
		}
	}

	public boolean remove(String key) throws DatabaseException {
		if (key == null || key.trim().isEmpty()) {
			return false;
		}
		try {
			log.debug("[{}] Will remove entry for {}", CallReference.getId(), key);
			Blobdata blodata;
			currentRemove.add(key);
			try (Closeable c = driver.getLockTable().lock(key)) {
				currentRemove.add(key);
				blodata = driver.getDatabaseOperations().getBlobdata(key);
				if (blodata != null) {
					DatabaseOperations dbo = driver.getDatabaseOperations();
					if (blodata.isArchived()) {
						dbo.removeArchived(key, blodata.getRelativeFilename(), blodata.getSize());
					} else {
						dbo.removeBlobdata(key);
						driver.getFileOperations().remove(blodata.getRelativeFilename());
					}
				}
			} finally {
				currentRemove.remove(key);
			}
			numberOfRemove++;
			return blodata != null;
		} catch (SQLException | IOException e) {
			throw new DatabaseException(e);
		}
	}

	private void putKey(String key, String mimetype, byte[] data) throws SQLException, IOException {
		Blobdata blobdata = driver.getDatabaseOperations().getBlobdata(key);
		if (null == blobdata) {
			log.debug("[{}] Will create entry for {}", CallReference.getId(), key);
			numberOfCreate++;
			addEntry(data, mimetype, key);
		} else {
			numberOfUpdate++;
			updateEntry(key, data, blobdata.getRelativeFilename(), false, mimetype, blobdata.isArchived());
		}
	}

	private String putNoKey(String mimetype, String prefix, String postfix, byte[] data) throws SQLException, IOException {
		numberOfCreate++;
		return createEntry(data, mimetype, prefix, postfix);
	}

	void putArchive(Blobdata blobdata) throws SQLException, IOException {
		byte[] data = driver.getFileOperations().readFileOp(blobdata).getData();
		String filename = driver.getFileArchiveOperations().write(blobdata.getBlobkey(), data);
		driver.getDatabaseOperations().addArchivedEntry(blobdata.getBlobkey(), filename, data.length);
	}

	private String createEntry(byte[] data, String mimetype, String prefix, String postfix) throws SQLException, IOException {
		String key = driver.getIdGenerator().getId();
		if (null != prefix) {
			key = prefix + key;
		}
		if (null != postfix) {
			key += postfix;
		}
		log.debug("[{}] Will create entry for {}", CallReference.getId(), key);
		addEntry(data, mimetype, key);
		return key;
	}

	void updateEntry(String key, byte[] data, String filename, boolean forceUpdate, String mimetype, boolean archived) throws SQLException,
			IOException {

		if (forceUpdate || !ArrayUtils.isEquals(driver.getFileOperations().read(key).getData(), data)) {
			log.debug("[{}] Will update {} currently in {}", CallReference.getId(), key, filename);
			if (!archived) {
				driver.getFileOperations().remove(filename);
			}
			addEntry(data, mimetype, key);
		} else {
			log.debug("[{}] Binary hasn't changed, no update for {}", CallReference.getId(), key);
		}
	}

	private void addEntry(byte[] data, String mimetype, String key) throws SQLException, IOException {
		String filename = driver.getFileOperations().write(key, data);
		driver.getDatabaseOperations().addEntry(key, filename, data.length, mimetype);
	}

	@Override
	public int getNumberOfReads() {
		return numberOfReads;
	}

	@Override
	public int getNumberOfCreate() {
		return numberOfCreate;
	}

	@Override
	public int getNumberOfUpdate() {
		return numberOfUpdate;
	}

	@Override
	public int getNumberOfRemove() {
		return numberOfRemove;
	}

	@Override
	public String getCurrentGets() {
		Object[] oarr = currentGet.toArray();
		StringBuilder build = new StringBuilder();
		for (Object o : oarr) {
			build.append(o);
		}
		return build.toString();
	}

	@Override
	public String getCurrentPuts() {
		Object[] oarr = currentPut.toArray();
		StringBuilder build = new StringBuilder();
		for (Object o : oarr) {
			build.append(o);
		}
		return build.toString();
	}

	@Override
	public String getCurrentRemoves() {
		Object[] oarr = currentRemove.toArray();
		StringBuilder build = new StringBuilder();
		for (Object o : oarr) {
			build.append(o);
		}
		return build.toString();
	}
}
