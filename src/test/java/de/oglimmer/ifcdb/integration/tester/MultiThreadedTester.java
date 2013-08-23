package de.oglimmer.ifcdb.integration.tester;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.internal.ArrayComparisonFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.DatabaseException;
import de.oglimmer.ifcdb.Driver;
import de.oglimmer.ifcdb.LockTable;
import de.oglimmer.ifcdb.util.RandomString;

abstract public class MultiThreadedTester {

	private static Logger log = LoggerFactory.getLogger(MultiThreadedTester.class);

	protected int numThreads;
	protected int numBinaries;
	protected int sizeBinary;

	protected Integer stopAfterThreadsAreDone;

	protected AtomicInteger numberBinaries;
	protected AtomicInteger doneSyncer;
	protected AtomicInteger phaseSyncer;
	protected AtomicBoolean fail;

	protected InterfaceType interfaceType;

	public MultiThreadedTester(InterfaceType interfaceType, int numThreads, int numBinaries, int sizeBinary) {
		this.interfaceType = interfaceType;
		this.numThreads = numThreads;
		this.numBinaries = numBinaries;
		this.sizeBinary = sizeBinary;
	}

	public void setStopAfterThreadsAreDone(Integer stopAfterThreadsAreDone) {
		this.stopAfterThreadsAreDone = stopAfterThreadsAreDone;
	}

	public void start() {

		log.debug("numThreads:" + numThreads);
		log.debug("numBinaries:" + numBinaries);

		numberBinaries = new AtomicInteger(numBinaries);
		doneSyncer = new AtomicInteger(stopAfterThreadsAreDone != null ? stopAfterThreadsAreDone : numThreads);
		phaseSyncer = new AtomicInteger(numThreads);
		fail = new AtomicBoolean(false);

		Collection<Runner> runners = new ArrayList<>();
		for (int i = 0; i < numThreads; i++) {
			Runner r = new Runner(i);
			Thread t = new Thread(r);
			t.start();
			r.setThread(t);
			runners.add(r);
		}

		synchronized (doneSyncer) {
			try {
				doneSyncer.wait();
			} catch (InterruptedException e) {
				// no
			}
		}
		if (fail.get()) {
			Assert.assertTrue(false);
		}
		for (Runner r : runners) {
			r.stop();
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("-----");
	}

	abstract protected void test(int threadNo) throws Exception;

	protected void updateBinary(String keyPrefix, Data data) throws IOException, FileNotFoundException,
			DatabaseException {
		String key = keyPrefix + Integer.toString((int) (Math.random() * numBinaries));
		updateBinary(data, key);
	}

	protected void updateBinary(Data data, String key) throws IOException, FileNotFoundException, DatabaseException {
		try (Closeable c = data.getLock(key)) {
			byte[] bytes = data.createBinary(key);
			long time = System.currentTimeMillis();
			interfaceType.put(key, null, null, null, bytes);
			log.debug("update key=" + key + " [" + bytes.length + "]:" + (System.currentTimeMillis() - time));
		}
	}

	protected void readBinary(String keyPrefix, Data data) throws DatabaseException, ArrayComparisonFailure,
			IOException {
		String key = keyPrefix + Integer.toString((int) (Math.random() * numBinaries));
		readBinary(data, key);
	}

	protected void readBinary(Data data, String key) throws DatabaseException, ArrayComparisonFailure, IOException {
		try (Closeable c = data.getLock(key)) {
			long time = System.currentTimeMillis();
			byte[] bytes = interfaceType.get(key);
			log.debug("read key=" + key + " [" + (bytes != null ? bytes.length : -1) + "]:"
					+ (System.currentTimeMillis() - time));
			byte[] expected = data.getBinary(key);
			Assert.assertArrayEquals("read failed", expected, bytes);
		}
	}

	protected void removeBinary(Data data, String key) throws DatabaseException, ArrayComparisonFailure, IOException {
		try (Closeable c = data.getLock(key)) {
			interfaceType.remove(key);
		}
	}

	protected void createSharedBinaries(Data data) throws IOException, DatabaseException {
		int key;
		while ((key = numberBinaries.decrementAndGet()) >= 0) {
			createBinary(data, Integer.toString(key));
		}
	}

	protected void createUnsharedBinaries(String keyPrefix, Data data) throws IOException, DatabaseException {
		for (int key = 0; key < numBinaries; key++) {
			createBinary(data, keyPrefix + key);
		}
	}

	protected void createBinary(Data data, String keyStr) throws IOException, DatabaseException {
		byte[] bytes = data.createBinary(keyStr);
		long time = System.currentTimeMillis();
		interfaceType.put(keyStr, null, null, null, bytes);
		log.debug("wrote key=" + keyStr + " [" + bytes.length + "]:" + (System.currentTimeMillis() - time));
	}

	protected void waitForOthers() throws InterruptedException {
		int waitSync = phaseSyncer.decrementAndGet();
		synchronized (phaseSyncer) {
			if (waitSync == 0) {
				phaseSyncer.set(numThreads);
				phaseSyncer.notifyAll();
			} else {
				phaseSyncer.wait();
			}
		}
	}

	public Data[] getData(Class<? extends Data> clazz, int number) {
		try {
			Object array = Array.newInstance(clazz, number);
			for (int i = 0; i < number; i++) {
				Constructor<? extends Data> ctor = clazz.getConstructor(MultiThreadedTester.class);
				Object val = ctor.newInstance(this);
				Array.set(array, i, val);
			}
			return (Data[]) array;
		} catch (ArrayIndexOutOfBoundsException | NegativeArraySizeException | NoSuchMethodException
				| SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] generateData(String key) {
		return (key + "_" + RandomString.getRandomStringASCII(1 + (int) (sizeBinary * Math.random()))).getBytes();
	}

	public class FileBackedData implements Data {

		private Driver driver;

		public FileBackedData(Driver driver) {
			this.driver = driver;
			try {
				FileUtils.deleteDirectory(new File("/tmp/test/"));
			} catch (IOException e) {
				log.error("Failed to delete tmp");
			}
			new File("/tmp/test/").mkdirs();
		}

		@Override
		public byte[] getBinary(String key) throws IOException {
			return readFile(key);
		}

		private byte[] readFile(String string) throws IOException {
			return Files.readAllBytes(new File("/tmp/test/" + string).toPath());
		}

		@Override
		public byte[] createBinary(String key) throws IOException {
			byte[] data = generateData(key);
			try (FileOutputStream fos = new FileOutputStream("/tmp/test/" + key)) {
				fos.write(data);
			}
			return data;
		}

		@Override
		public Closeable getLock(String key) {
			return driver.getLockTable().lockMem("TEST_" + key);
		}
	}

	public class MemoryBackedData implements Data {

		protected LockTable locktable = new LockTable(null);
		private Map<String, byte[]> dataHolder = Collections.synchronizedMap(new HashMap<String, byte[]>());

		@Override
		public byte[] getBinary(String key) {
			return dataHolder.get(key);
		}

		@Override
		public byte[] createBinary(String key) throws IOException {
			byte[] data = generateData(key);
			dataHolder.put(key, data);
			return data;
		}

		@Override
		public Closeable getLock(String key) {
			return locktable.lockMem("TEST_" + key);
		}
	}

	class Runner implements Runnable {

		private volatile int threadNo;

		private Thread thread;

		public Runner(int threadNo) {
			this.threadNo = threadNo;
		}

		public void setThread(Thread thread) {
			this.thread = thread;
		}

		public void stop() {
			threadNo = -1;
			thread.interrupt();
		}

		@Override
		public void run() {
			try {
				test(threadNo);
			} catch (InterruptedException e) {
				// just ignore
			} catch (Exception | AssertionError e) {
				e.printStackTrace();
				fail.set(true);
				synchronized (doneSyncer) {
					doneSyncer.notify();
				}
				throw new RuntimeException(e);
			} finally {
				doneSyncer.decrementAndGet();
				if (doneSyncer.get() == 0) {
					synchronized (doneSyncer) {
						doneSyncer.notify();
					}
				}
			}
		}
	}
}
