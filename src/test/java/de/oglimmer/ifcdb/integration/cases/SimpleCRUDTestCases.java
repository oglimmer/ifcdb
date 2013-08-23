package de.oglimmer.ifcdb.integration.cases;

import de.oglimmer.ifcdb.DatabaseException;
import de.oglimmer.ifcdb.integration.tester.InterfaceType;
import de.oglimmer.ifcdb.integration.tester.SimpleCRUDTester;

public class SimpleCRUDTestCases {

	public static void insertMass(InterfaceType interfacetype) throws DatabaseException {
		SimpleCRUDTester.simpleMassInsert(interfacetype, 1, 1024);
		SimpleCRUDTester.simpleMassInsert(interfacetype, 100, 1024);
		SimpleCRUDTester.simpleMassInsert(interfacetype, 1000, 1024);
		SimpleCRUDTester.simpleMassInsert(interfacetype, 10000, 1024);
		SimpleCRUDTester.simpleMassInsert(interfacetype, 1000, 1024 * 100);
		SimpleCRUDTester.simpleMassInsert(interfacetype, 100, 1024 * 1024);
		SimpleCRUDTester.simpleMassInsert(interfacetype, 1000, 1024 * 1024);
		SimpleCRUDTester.simpleMassInsert(interfacetype, 10000, 1024 * 1024);
		SimpleCRUDTester.simpleMassInsert(interfacetype, 1, 1024 * 1024 * 50);
		SimpleCRUDTester.simpleMassInsert(interfacetype, 100, 1024 * 1024 * 50);
	}

	public static void insert(InterfaceType interfacetype) throws DatabaseException {
		SimpleCRUDTester.insert(interfacetype, 5);
	}

	public static void insertMultiple(InterfaceType interfacetype) throws DatabaseException, InterruptedException {
		SimpleCRUDTester.insert(interfacetype, "1", "2", "3");
		Thread.sleep(1000);
		SimpleCRUDTester.insert(interfacetype, "1", "2", "3", "4");
		Thread.sleep(1000);
		SimpleCRUDTester.insert(interfacetype, "1", "2", "3", "4", "5");
		Thread.sleep(1000);
		SimpleCRUDTester.insert(interfacetype, "1", "2", "3", "4");
		Thread.sleep(1000);
		SimpleCRUDTester.insert(interfacetype, "1", "2", "3");
	}

	public static void updateEqual(InterfaceType interfacetype) throws DatabaseException {
		SimpleCRUDTester.updateEqual(interfacetype);
	}

	public static void insertDelete(InterfaceType interfacetype) throws DatabaseException {
		SimpleCRUDTester.insertDelete(interfacetype, 50);
	}
}
