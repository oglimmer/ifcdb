package de.oglimmer.ifcdb.integration;

import org.junit.Test;

import de.oglimmer.ifcdb.integration.cases.MultiThreadedTestCases;
import de.oglimmer.ifcdb.integration.tester.InProcessTester;
import de.oglimmer.ifcdb.integration.tester.InterfaceType;

public class MultiThreadedInProcessTest extends DBIntegrationBase {

	private InterfaceType getInterfaceType() {
		return new InProcessTester(driver);
	}

	@Test
	public void multiThreadedViaMemNotsharedFast() {
		MultiThreadedTestCases.multiThreadedViaMemNotsharedFast(getInterfaceType());
	}

	@Test
	public void multiThreadedViaMemNotshared() {
		MultiThreadedTestCases.multiThreadedViaMemNotshared(getInterfaceType());
	}

	@Test
	public void multiThreadedViaMemNotsharedLarge() {
		MultiThreadedTestCases.multiThreadedViaMemNotsharedLarge(getInterfaceType());
	}

	@Test
	public void multiThreadedViaMemShared1() {
		MultiThreadedTestCases.multiThreadedViaMemShared1(getInterfaceType());
	}

	@Test
	public void multiThreadedViaMemShared2() {
		MultiThreadedTestCases.multiThreadedViaMemShared2(getInterfaceType());
	}

	@Test
	public void multiThreadedViaMemShared3() {
		MultiThreadedTestCases.multiThreadedViaMemShared3(getInterfaceType());
	}

	@Test
	public void multiThreadedViaMemShared4() {
		MultiThreadedTestCases.multiThreadedViaMemShared4(getInterfaceType());
	}

	@Test
	public void multiThreadedViaMemShared5() {
		MultiThreadedTestCases.multiThreadedViaMemShared5(getInterfaceType());
	}

	@Test
	public void multiThreadedViaMemNonShared6() {
		MultiThreadedTestCases.multiThreadedViaMemNonShared6(getInterfaceType());
	}

	@Test
	public void multiThreadedViaMemShared6() {
		MultiThreadedTestCases.multiThreadedViaMemShared6(getInterfaceType());
	}

}
