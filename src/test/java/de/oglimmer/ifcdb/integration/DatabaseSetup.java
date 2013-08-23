package de.oglimmer.ifcdb.integration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import de.oglimmer.ifcdb.Config;
import de.oglimmer.ifcdb.DatabaseJdbcOperations;
import de.oglimmer.ifcdb.DatabaseOperations;
import de.oglimmer.ifcdb.Driver;

public class DatabaseSetup {

	public static DatabaseSetup getInstance() {
		return new DatabaseSetup();
	}

	public static DatabaseSetup getInstanceJdbc() {
		Config.setDatabaseOperations("jdbc");
		return new DatabaseSetup();
	}

	public static DatabaseSetup getInstanceHibernate() {
		Config.setDatabaseOperations("hibernate");
		return new DatabaseSetup();
	}

	private WeldContainer wc;
	private Weld weld;

	public DatabaseSetup() {
		weld = new Weld();
		wc = weld.initialize();
	}

	public Driver warmUp() throws IOException, SQLException {
		return setup();
	}

	public void tearDown(Driver driver) throws IOException, SQLException, InterruptedException {
		driver.stopServer();
		wc.instance().destroy(driver);
		weld.shutdown();
	}

	public Properties getProperties() throws IOException {
		Properties prop = new Properties();
		prop.load(DatabaseSetup.class.getResourceAsStream("/config.properties"));

		if (null == Config.getDatabaseOperations()) {
			Config.setDatabaseOperations(prop.getProperty("databaseOperations"));
		}

		return prop;
	}

	protected Driver getDriver() {
		return wc.instance().select(Driver.class).get();
	}

	public WeldContainer getWeldContainer() {
		return wc;
	}

	protected Driver setup() throws IOException, SQLException {

		Properties prop = getProperties();

		Driver driver = getDriver();
		driver.init(prop);

		DatabaseOperations dbbo = driver.getDatabaseOperations();
		if (dbbo instanceof DatabaseJdbcOperations) {
			DatabaseJdbcOperations dbo = (DatabaseJdbcOperations) dbbo;
			if ("org.hsqldb.jdbc.JDBCDriver".equals(driver.getConfig().getJdbcUrl())) {
				dbo.update("CREATE MEMORY TABLE REMOVALFILERECORD (  filename varchar(255) NOT NULL ,  size int NOT NULL,  PRIMARY KEY (filename))");
				dbo.update("CREATE MEMORY TABLE FILERECORD (  filename varchar(255) NOT NULL ,  size int NOT NULL,  PRIMARY KEY (filename))");
				dbo.update("CREATE MEMORY TABLE BLOBDATA (  blobkey varchar(255) NOT NULL ,  filename varchar(255) NOT NULL ,  offset int NOT NULL,  size int NOT NULL,  lastupdate bigint NOT NULL,  archived tinyint NOT NULL, mimetype varchar(255),  PRIMARY KEY (blobkey))");
			}
		}

		return driver;
	}

}
