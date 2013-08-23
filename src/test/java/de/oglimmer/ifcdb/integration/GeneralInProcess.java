package de.oglimmer.ifcdb.integration;

import org.junit.Test;

import de.oglimmer.ifcdb.DatabaseException;
import de.oglimmer.ifcdb.integration.cases.SimpleCRUDTestCases;
import de.oglimmer.ifcdb.integration.tester.InProcessTester;
import de.oglimmer.ifcdb.integration.tester.InterfaceType;

public class GeneralInProcess extends DBIntegrationBase {

	private InterfaceType getInterfaceType() {
		return new InProcessTester(driver);
	}

	@Test
	public void insertMass() throws DatabaseException {
		driver.getJanitor().start();
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

	@Test
	public void updateEqual() throws DatabaseException {
		SimpleCRUDTestCases.updateEqual(getInterfaceType());
	}

}
