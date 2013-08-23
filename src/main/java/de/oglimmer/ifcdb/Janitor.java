package de.oglimmer.ifcdb;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.records.Blobdata;
import de.oglimmer.ifcdb.stats.JanitorMBean;

/**
 * Separated thread to archive and compress the file system structure. Must not run twice on a particular file system.
 * 
 * @author Oli Zimpasser
 * 
 */
public class Janitor implements Runnable, JanitorMBean {

	private Logger log = LoggerFactory.getLogger(Janitor.class);

	private Driver driver;
	private boolean running;
	private Thread thread;
	private FileLock fl;

	private int numberOfKeysArchived;
	private int numberOfArchivesCompressed;
	private int numberOfFilesKeptDuringCompression;
	private Date lastDance;
	private long totalTimeArchive;
	private long totalTimeCompress;

	@Inject
	public Janitor(Driver driver) {
		this.driver = driver;
	}

	public void init() {
		if (driver.getConfig().isStartJanitor()) {
			start();
		}
	}

	private boolean trySetLockfile() {
		File lockFile = new File(driver.getConfig().getDataDir() + "lock-file");
		if (!lockFile.exists()) {
			try {
				FileOutputStream fos = new FileOutputStream(lockFile);
				fl = fos.getChannel().tryLock();
				if (fl == null) {
					log.warn("Could not start Janitor because lock-file is locked");
				}
			} catch (IOException e) {
				log.error("Failed to create lock-file", e);
			}
		} else {
			log.warn("Could not start Janitor because lock-file exists");
		}

		return fl != null;
	}

	@Override
	public void run() {
		log.debug("Janitor started");
		while (running) {
			try {
				lastDance = new Date();
				processLoop();
				if (running) {
					Thread.sleep(driver.getConfig().getJanitorThreadWait());
				}
			} catch (InterruptedException e) {
				// on shutdown
			} catch (Exception e) {
				log.error("Exception in Janitor-thread", e);
				try {
					Thread.sleep(1000 * 15);
				} catch (InterruptedException e1) {
					// on shutdown
				}
			}
		}
		log.debug("Janitor successfully shut down");
	}

	private void processLoop() throws SQLException, DatabaseException, IOException {
		int numArchived = archive();
		if (numArchived == 0) {
			compress();
		}
	}

	private int archive() throws SQLException, IOException {
		long time = System.currentTimeMillis();
		Collection<Blobdata> toArchive = driver.getDatabaseOperations().getToBeArchived();

		int counter = 0;
		for (Iterator<Blobdata> it = toArchive.iterator(); it.hasNext()
				&& counter < driver.getConfig().getNumberOfArchivingPerRun(); counter++) {
			Blobdata blobdata = it.next();
			it.remove();
			SessionImpl session = (SessionImpl) driver.getSession();

			try (Closeable c = driver.getLockTable().lock(blobdata.getBlobkey())) {

				// try to load the key, so ensure that the key is still existent (and not deleted)
				blobdata = driver.getDatabaseOperations().getBlobdata(blobdata.getBlobkey());
				if (null != blobdata) {
					log.debug("Archive {} currently in {} with size {}", blobdata.getBlobkey(),
							blobdata.getRelativeFilename(), blobdata.getSize());
					session.putArchive(blobdata);
					deleteSimpleFile(blobdata.getRelativeFilename());
					numberOfKeysArchived++;
				}

			}
		}
		if (counter > 0) {
			totalTimeArchive += (System.currentTimeMillis() - time);
			log.debug("Archive took {} and cleaned {} files", System.currentTimeMillis() - time, counter);
		}
		return counter;
	}

	private void compress() throws SQLException, DatabaseException, IOException {

		Collection<String> filenames = driver.getDatabaseOperations().getOpenRemovals();

		if (!filenames.isEmpty()) {
			String filename = filenames.iterator().next();

			if (!filename.equals(driver.getFileArchiveOperations().getCurrentFile())) {
				compress(filename);
			}
		}
	}

	private void compress(String filename) throws SQLException, DatabaseException, IOException {
		long time = System.currentTimeMillis();
		Collection<Blobdata> col = driver.getDatabaseOperations().getBlobdataByFilename(filename);
		SessionImpl session = (SessionImpl) driver.getSession();

		for (Blobdata blobdata : col) {
			String key = blobdata.getBlobkey();
			try (Closeable c = driver.getLockTable().lock(key)) {

				// try to load the key, so ensure that the key is still existent (and not deleted) and you
				// have the right mimetype (if it was changed in the last moment)
				Blobdata bd = driver.getDatabaseOperations().getBlobdata(key);
				if (null != bd) {
					log.debug("Compress {} currently in {} with size {}", bd.getBlobkey(), bd.getRelativeFilename(),
							bd.getSize());					
					byte[] data = driver.getFileOperations().read(key).getData();
					session.updateEntry(key, data, bd.getRelativeFilename(), true, bd.getMimetype(), bd.isArchived());
					numberOfFilesKeptDuringCompression++;
				}

			}
		}

		if (!(new File(driver.getConfig().getDataDir() + filename)).delete()) {
			log.error("Failed to delete {}", driver.getConfig().getDataDir() + filename);
		}

		driver.getDatabaseOperations().removeRemovalfileRecord(filename);
		numberOfArchivesCompressed++;
		totalTimeCompress += (System.currentTimeMillis() - time);
		log.debug("Compress took {}, compressed {} kept {} entries", System.currentTimeMillis() - time, filename,
				col.size());
	}

	private void deleteSimpleFile(String filename) {
		if (!(new File(driver.getConfig().getDataDir() + filename)).delete()) {
			log.error("failed to delete {}", driver.getConfig().getDataDir() + filename);
		}
	}

	public void start() {
		if (trySetLockfile()) {
			if (null == thread) {
				thread = new Thread(this, "Janitor");
				thread.setDaemon(true);
				running = true;
				thread.start();
			}
		}
	}

	public void stop() {
		if (running) {
			if (null != fl) {
				try {
					fl.release();
					fl.acquiredBy().close();
					fl = null;
					new File(driver.getConfig().getDataDir() + "lock-file").delete();
				} catch (IOException e) {
					log.error("Failed to close FileLock", e);
				}
			} else {
				log.warn("Janitor was running but no file-lock object!");
			}
			running = false;
		}
		if (null != thread) {
			thread.interrupt();
			thread = null;
		}
	}

	@Override
	public int getNumberOfKeysArchived() {
		return numberOfKeysArchived;
	}

	@Override
	public int getNumberOfArchivesCompressed() {
		return numberOfArchivesCompressed;
	}

	@Override
	public int getNumberOfFilesKeptDuringCompression() {
		return numberOfFilesKeptDuringCompression;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public Date getLastDance() {
		return lastDance;
	}

	@Override
	public int getAverageArchiveTime() {
		return (int) (totalTimeArchive / numberOfKeysArchived);
	}

	@Override
	public int getAverageCompressTime() {
		return (int) (totalTimeCompress / numberOfArchivesCompressed);
	}
}
