package de.oglimmer.ifcdb.integration;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import de.oglimmer.ifcdb.DatabaseOperations;
import de.oglimmer.ifcdb.Driver;
import de.oglimmer.ifcdb.FileBase;

public class DBIntegrationBase {

	private static String oldPathStructure;

	protected static Driver driver;

	@BeforeClass
	public static void init() throws SQLException, IOException {

		oldPathStructure = FileBase.PATH_STRUCTURE;

		FileBase.PATH_STRUCTURE = getPathStructure();
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");

		driver = DatabaseSetup.getInstance().warmUp();
	}

	protected static String getPathStructure() {
		return "yyyy/MM/dd/HH/mm/ss/";
	}

	@AfterClass
	public static void destroy() throws IOException, SQLException, InterruptedException {
		DatabaseSetup.getInstance().tearDown(driver);

		FileBase.PATH_STRUCTURE = oldPathStructure;
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");
	}
	
	@Before
	public void warmUp() throws IOException, SQLException {
		FileUtils.deleteDirectory(new File("junitData"));
		driver.getFileArchiveOperations().init();

		DatabaseOperations dbo = driver.getDatabaseOperations();
		dbo.update("delete from RemovalfileRecord");
		dbo.update("delete from FileRecord");
		dbo.update("delete from Blobdata");
	}
}
