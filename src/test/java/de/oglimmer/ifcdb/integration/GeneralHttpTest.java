package de.oglimmer.ifcdb.integration;

import org.junit.Test;

import de.oglimmer.ifcdb.DatabaseException;
import de.oglimmer.ifcdb.integration.cases.SimpleCRUDTestCases;
import de.oglimmer.ifcdb.integration.tester.HttpAPITester;
import de.oglimmer.ifcdb.integration.tester.InterfaceType;

public class GeneralHttpTest {

	private InterfaceType getInterfaceType() {
		return new HttpAPITester();
	}

	@Test
	public void insertMass() throws DatabaseException {
		SimpleCRUDTestCases.insertMass(getInterfaceType());
	}

	@Test
	public void insert() throws DatabaseException {
		SimpleCRUDTestCases.insert(getInterfaceType());
	}

	@Test
	public void insertDelete() throws DatabaseException {
		SimpleCRUDTestCases.insertDelete(getInterfaceType());
	}

	@Test
	public void insertMultiple() throws DatabaseException, InterruptedException {
		SimpleCRUDTestCases.insertMultiple(getInterfaceType());
	}

}
