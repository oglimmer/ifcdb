package de.oglimmer.ifcdb;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.stats.LockTableMBean;

/**
 * Offers synchronization depended on a string object (key)
 * 
 * @author Oli Zimpasser
 * 
 */
public class LockTable implements Runnable, LockTableMBean {

	private Logger log = LoggerFactory.getLogger(LockTable.class);

	private static final int TIME_TO_SLEEP = 60_000; // run every minute

	private static final long TIME_DELTA_TO_REMOVE_MEM = 60_000; // remove older than a minute
	private static final long TIME_DELTA_TO_REMOVE_FILE = 60 * 60_000; // remove older than an hour

	private Driver driver;
	private Thread thread;
	private boolean running;

	private Date lastDance;

	private Map<String, LongHolder> locksMem = Collections.synchronizedMap(new HashMap<String, LongHolder>());
	private Map<String, FileLock> locksFile = Collections.synchronizedMap(new HashMap<String, FileLock>());

	@Inject
	public LockTable(Driver driver) {
		this.driver = driver;
	}

	public void init() {
		new File(driver.getConfig().getLockDir()).mkdirs();
		running = true;
		thread = new Thread(this, "LockTable-Remover");
		thread.setDaemon(true);
		thread.start();
	}

	public _Closeable lock(String key) {
		if (driver.getConfig().isFileLocking()) {
			return lockFile(key);
		} else {
			return lockMem(key);
		}
	}

	public synchronized _Closeable lockMem(String key) {
		LongHolder o = locksMem.get(key);
		if (null == o) {
			o = new LongHolder();
			locksMem.put(key, o);
			o.lock.lock();
		} else {
			o.lastAccessTime = System.currentTimeMillis();
			o.lock.lock();
		}
		return new _Closeable(key, true);
	}

	public _Closeable lockFile(String key) {
		key = key.replace("/", "-");
		int tryCounter = 3;
		while (true) {
			try {
				FileLock fl = new FileOutputStream(driver.getConfig().getLockDir() + key).getChannel().lock();
				locksFile.put(key, fl);
				return new _Closeable(key, false);
			} catch (OverlappingFileLockException e) {
				tryCounter--;
				if (tryCounter == 0) {
					throw e;
				}
				try {
					Thread.sleep(250);
				} catch (InterruptedException e1) {
					log.error("Thread interrupted", e1);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void run() {
		while (running) {
			try {
				lastDance = new Date();

				removeMemLockObjects();

				removeLockFiles();

				Thread.sleep(TIME_TO_SLEEP);
			} catch (InterruptedException e) {
				// stop event
			} catch (Exception e) {
				log.error("Lock-Remover failed", e);
				try {
					Thread.sleep(10 * 1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	private void removeLockFiles() {
		final long removeTime = System.currentTimeMillis() - TIME_DELTA_TO_REMOVE_FILE;
		File directory = new File(driver.getConfig().getLockDir());
		if (directory.exists()) {
			int counter = 0;
			File[] listFiles = directory.listFiles();
			for (File listFile : listFiles) {
				if (locksFile.containsKey(listFile.getName())) {
					log.warn("Tried to remove lock-file: {} but still in use", listFile.getName());
				} else {
					if (listFile.lastModified() < removeTime) {
						counter++;
						if (!listFile.delete()) {
							log.warn("Tried to remove lock-file: {} but delete failed.", listFile.getName());
						}
					}
				}
			}
			if (counter > 0) {
				log.debug("Removed {} file-locks", counter);
			}
		}
	}

	private void removeMemLockObjects() {
		Collection<String> toRemove = new ArrayList<>(1000);
		long removeTime = System.currentTimeMillis() - TIME_DELTA_TO_REMOVE_MEM;
		synchronized (this) {
			int counter = 0;
			for (Iterator<Map.Entry<String, LongHolder>> it = locksMem.entrySet().iterator(); it.hasNext() && running;) {
				Map.Entry<String, LongHolder> en = it.next();
				if (en.getValue().lastAccessTime < removeTime) {
					toRemove.add(en.getKey());
					counter++;
				}
			}
			if (counter > 0) {
				log.debug("Removed {} mem-locks", counter);
			}
		}
		for (String itemToRemove : toRemove) {
			locksMem.remove(itemToRemove);
		}
	}

	public void stop() {
		running = false;
		if (null != thread) {
			thread.interrupt();
			thread = null;
		}
	}

	/*
	 * @Test public void test() throws InterruptedException { final LockTable lt = new LockTable(); lt.init();
	 * 
	 * new Thread(new Runnable() {
	 * 
	 * @Override public void run() { int i = 0; while (true) { i++; lt.getLock("" + i + (int) (Math.random() * 1000)); }
	 * }
	 * 
	 * }).start();
	 * 
	 * Thread.sleep(20000);
	 * 
	 * }
	 */

	static class LongHolder {
		public Lock lock = new ReentrantLock();
		public long lastAccessTime = System.currentTimeMillis();;
	}

	class _Closeable implements Closeable {

		private String key;
		private boolean inMem;

		_Closeable(String key, boolean inMem) {
			this.key = key;
			this.inMem = inMem;
		}

		@Override
		public void close() throws IOException {
			if (inMem) {
				releaseLockMem(key);
			} else {
				releaseLock(key);
			}
		}

		public void releaseLockMem(String key) {
			LongHolder o = locksMem.get(key);
			o.lock.unlock();
		}

		public void releaseLock(String key) {
			try {
				locksFile.get(key).release();
				locksFile.get(key).acquiredBy().close();
				locksFile.remove(key);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public int getNumberMemLocks() {
		return locksMem.size();
	}

	@Override
	public int getNumberFileLocks() {
		return locksFile.size();
	}

	@Override
	public Date getLastDance() {
		return lastDance;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

}
