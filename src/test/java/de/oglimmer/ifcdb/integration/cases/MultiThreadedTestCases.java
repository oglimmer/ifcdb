package de.oglimmer.ifcdb.integration.cases;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.oglimmer.ifcdb.integration.tester.Data;
import de.oglimmer.ifcdb.integration.tester.InterfaceType;
import de.oglimmer.ifcdb.integration.tester.MultiThreadedTester;

public class MultiThreadedTestCases {

	public static void multiThreadedViaMemNotsharedFast(InterfaceType interfaceType) {
		final int availableProcessors = Runtime.getRuntime().availableProcessors();

		// MEM: 5kB*CORES
		MultiThreadedTester mt = new MultiThreadedTester(interfaceType, availableProcessors, 5, 1024) {
			private Data[] data = getData(MemoryBackedData.class, availableProcessors);

			protected void test(int no) throws Exception {
				createUnsharedBinaries(Integer.toString(no) + "_", data[no]);
				waitForOthers();
				for (int i = 0; i < 100; i++) {
					readBinary(Integer.toString(no) + "_", data[no]);
					updateBinary(Integer.toString(no) + "_", data[no]);
					readBinary(Integer.toString(no) + "_", data[no]);
				}
			}
		};
		mt.start();
	}

	public static void multiThreadedViaMemNotshared(InterfaceType interfaceType) {
		final int availableProcessors = Runtime.getRuntime().availableProcessors();

		// MEM: 250MB*CORES
		MultiThreadedTester mt = new MultiThreadedTester(interfaceType, availableProcessors, 25, 1024 * 1024) {
			private Data[] data = getData(MemoryBackedData.class, availableProcessors);

			protected void test(int no) throws Exception {
				createUnsharedBinaries(Integer.toString(no) + "_", data[no]);
				waitForOthers();
				for (int i = 0; i < 500; i++) {
					readBinary(Integer.toString(no) + "_", data[no]);
					updateBinary(Integer.toString(no) + "_", data[no]);
					readBinary(Integer.toString(no) + "_", data[no]);
				}
			}
		};
		mt.start();
	}

	public static void multiThreadedViaMemNotsharedLarge(InterfaceType interfaceType) {
		// MEM: 400MB
		MultiThreadedTester mt = new MultiThreadedTester(interfaceType, 1, 4, 1024 * 1024 * 100) {
			private Data[] data = getData(MemoryBackedData.class, 1);

			protected void test(int no) throws Exception {
				createUnsharedBinaries(Integer.toString(no) + "_", data[no]);
				waitForOthers();
				for (int i = 0; i < 25; i++) {
					readBinary(Integer.toString(no) + "_", data[no]);
					updateBinary(Integer.toString(no) + "_", data[no]);
					readBinary(Integer.toString(no) + "_", data[no]);
				}
			}
		};
		mt.start();
	}

	public static void multiThreadedViaMemShared1(InterfaceType interfaceType) {

		// MEM: 600MB
		// BEHAVIOR: 600 binaries, concurrent operations, not so frequent archiving and
		// compressing of each binary, write:read=1:1
		MultiThreadedTester mt = new MultiThreadedTester(interfaceType, Runtime.getRuntime().availableProcessors(),
				600, 1024 * 1024) {
			private Data data = new MemoryBackedData();

			protected void test(int no) throws Exception {
				createSharedBinaries(data);
				waitForOthers();
				for (int i = 0; i < 500; i++) {
					updateBinary("", data);
					readBinary("", data);
				}
			}
		};
		mt.start();
	}

	public static void multiThreadedViaMemShared2(InterfaceType interfaceType) {

		// MEM: 10MB
		// BEHAVIOR: small number of binaries, strong concurrent operations, ultra high frequent archiving and
		// compressing of each binary, write:read=1:1
		MultiThreadedTester mt = new MultiThreadedTester(interfaceType, Runtime.getRuntime().availableProcessors(), 10,
				1024 * 1024) {
			private Data data = new MemoryBackedData();

			protected void test(int no) throws Exception {
				createSharedBinaries(data);
				waitForOthers();
				for (int j = 0; j < 150; j++) {
					Thread.sleep(1000);
					for (int i = 0; i < 5; i++) {
						updateBinary("", data);
						readBinary("", data);
					}
				}
			}
		};
		mt.start();
	}

	public static void multiThreadedViaMemShared3(InterfaceType interfaceType) {
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		final int WRITERS = availableProcessors / 4;
		// MEM: 1MB*CORES
		// BEHAVIOR: small number of binaries, strong concurrent operations, ultra high frequent archiving and
		// compressing of each binary, write:read=1:3
		MultiThreadedTester mt = new MultiThreadedTester(interfaceType, availableProcessors, availableProcessors,
				1024 * 1024) {
			private Data data = new MemoryBackedData();

			protected void test(int no) throws Exception {
				createSharedBinaries(data);
				waitForOthers();
				for (int j = 0; j < 1500 && no != -1; j++) {
					if (no < WRITERS) {
						Thread.sleep(700);
						updateBinary("", data);
					} else {
						readBinary("", data);
					}
				}
			}
		};
		mt.setStopAfterThreadsAreDone(availableProcessors - WRITERS);
		mt.start();
	}

	public static void multiThreadedViaMemShared4(InterfaceType interfaceType) {
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		// MEM: 1MB*CORES
		// BEHAVIOR: small number of binaries, strong concurrent operations, ultra high frequent archiving and
		// compressing of each binary, changes all binaries, then waits for archive.
		MultiThreadedTester mt = new MultiThreadedTester(interfaceType, availableProcessors, availableProcessors, 24) {
			private Data data = new MemoryBackedData();

			protected void test(int no) throws Exception {
				createSharedBinaries(data);
				waitForOthers();
				for (int j = 0; j < 150000 && no != -1; j++) {
					if (no < 1) {
						Thread.sleep(5000);
						for (int i = 0; i < 30 && no != -1; i++) {
							updateBinary("", data);
						}
					} else {
						readBinary("", data);
					}
				}
			}
		};
		mt.setStopAfterThreadsAreDone(availableProcessors - 1);
		mt.start();
	}

	public static void multiThreadedViaMemShared5(InterfaceType interfaceType) {
		// MEM: 16bytes
		// BEHAVIOR: 1 binary, strong concurrent operation on one object, no archiving and
		// compressing of each binary, write:read=1:1
		MultiThreadedTester mt = new MultiThreadedTester(interfaceType, 2, 1, 16) {
			private Data data = new MemoryBackedData();

			protected void test(int no) throws Exception {
				createSharedBinaries(data);
				waitForOthers();
				for (int j = 0; j < 20000; j++) {
					updateBinary(data, "0");
					readBinary(data, "0");
				}
			}
		};
		mt.start();
	}

	public static void multiThreadedViaMemNonShared6(InterfaceType interfaceType) {
		int availableProcessors = Runtime.getRuntime().availableProcessors();
		// MEM:
		// BEHAVIOR:
		MultiThreadedTester mt = new MultiThreadedTester(interfaceType, availableProcessors, 100, 1024) {
			private Data data = new MemoryBackedData();

			protected void test(int no) throws Exception {
				List<String> keys = new ArrayList<>();
				for (int i = 0; i < numBinaries; i++) {
					keys.add(no + "_" + i);
					createBinary(data, no + "_" + i);
				}
				waitForOthers();
				for (int j = 0; j < 70; j++) {
					String key = keys.remove((int) (Math.random() * keys.size()));
					updateBinary(data, key);
					readBinary(data, key);
					removeBinary(data, key);
				}
			}
		};
		mt.start();
	}

	public static void multiThreadedViaMemShared6(InterfaceType interfaceType) {
		final int availableProcessors = Runtime.getRuntime().availableProcessors();
		final int rounds = 500;
		// MEM:
		// BEHAVIOR:
		MultiThreadedTester mt = new MultiThreadedTester(interfaceType, availableProcessors, (int) (rounds * 1.5), 1024) {
			private Data data = new MemoryBackedData();
			List<String> keys = Collections.synchronizedList(new ArrayList<String>());

			protected void test(int no) throws Exception {
				if (no == 0) {
					for (int i = 0; i < numBinaries; i++) {
						keys.add(no + "_" + i);
						createBinary(data, no + "_" + i);
					}
				}
				waitForOthers();
				for (int j = 0; j < rounds / availableProcessors; j++) {
					String key = keys.get((int) (Math.random() * keys.size()));
					try (Closeable c = data.getLock(key)) {
						if (keys.remove(key)) {
							updateBinary(data, key);
							readBinary(data, key);
							removeBinary(data, key);
						}
					}
				}
			}
		};
		mt.start();
	}
}
