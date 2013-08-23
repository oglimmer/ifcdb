package de.oglimmer.ifcdb;

import java.lang.reflect.Field;
import java.util.Properties;

import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.stats.ConfigMBean;

/**
 * Holds the configuration.
 * 
 * @author Oli Zimpasser
 * 
 */
public class Config implements ConfigMBean {

	private Logger log = LoggerFactory.getLogger(Config.class);

	/**
	 * Max. size of an archive file in bytes. Default 50MB.
	 */
	private int maxFilesize = 1024 * 1024 * 50;

	/**
	 * Path to the data directory. Default: ./data/
	 */
	private String dataDir = "./data/";

	/**
	 * Threshold when to compress an archive file. Default 50% of maxFilesize
	 */
	private int cleanThreshold = (int) (maxFilesize * 0.5);

	/**
	 * Time in unites (default hours) when the janitor should archive single files. Default: 2 hours.
	 */
	private int timeMoveToArchive = 2;

	/**
	 * Max number of files archived per loop run. Default: 50 files.
	 */
	private int numberOfArchivingPerRun = 50;

	/**
	 * jdbc or hibernate
	 */
	private static String databaseOperations;

	/**
	 * Indicates whether the janitor should be started or not. Default: true
	 */
	private boolean startJanitor = true;

	/**
	 * Time in millis the janitor thread waits between two loop runs. Default: 15 secs.
	 */
	private int janitorThreadWait = 15_000;

	/**
	 * jdbc driver class. No default.
	 */
	private String jdbcDriver;

	/**
	 * jdbc url. No default.
	 */
	private String jdbcUrl;

	/**
	 * jdbc user. No default.
	 */
	private String jdbcUser;

	/**
	 * jdbc password. No default.
	 */
	private String jdbcPassword;

	/**
	 * Hibernate dialect. No default.
	 */
	private String hibernateDialect;

	/**
	 * Defines if the server (in general) is up and running
	 */
	private boolean serverActive = true;

	/**
	 * Path to the locks directory. Default: ./locks/
	 */
	private String lockDir = "./locks/";

	/**
	 * Defines whether file or memory based locking should be used. Necessary if repository is accessed from multiple
	 * JVMs. Default: true
	 */
	private boolean fileLocking = true;

	public void init(Properties prop) {
		copyProperties(prop);
		getAttributesFromEnv(prop);
		printConfig();
	}

	private void getAttributesFromEnv(Properties prop) {
		try {
			for (Field f : getClass().getDeclaredFields()) {
				String sysProp = System.getProperty(f.getName());
				if (null != sysProp) {
					if (f.getType().equals(int.class)) {
						f.set(this, Integer.valueOf(sysProp));
					} else if (f.getType().equals(boolean.class)) {
						f.set(this, Boolean.valueOf(sysProp));
					} else if (f.getType().equals(String.class)) {
						f.set(this, sysProp);
					}
				}
			}
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			log.error("Failed to get attribute from env", e);
		}
	}

	private void copyProperties(Properties prop) {
		if (null != prop) {
			for (Object p : prop.keySet()) {
				try {
					Field f = getClass().getDeclaredField(p.toString());
					if (f.getType().equals(int.class)) {
						f.set(this, Integer.valueOf(prop.get(p).toString()));
					} else if (f.getType().equals(boolean.class)) {
						f.set(this, Boolean.valueOf(prop.get(p).toString()));
					} else {
						f.set(this, prop.get(p));
					}
				} catch (NoSuchFieldException e) {
					log.error("There is no config value " + p);
				} catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
					log.error("Error while configuring config", e);
				}
			}
		}
	}

	private void printConfig() {
		log.info("Using");
		log.info("	dataDir={}", dataDir);
		log.info("	maxFilesize={}", maxFilesize);
		log.info("	cleanThreshold={}", cleanThreshold);
		log.info("	timeMoveToArchive={}", timeMoveToArchive);
		log.info("	numberOfArchivingPerRun={}", numberOfArchivingPerRun);
		log.info("	hibernateDialect={}", hibernateDialect);
		log.info("	jdbcDriver={}", jdbcDriver);
		log.info("	jdbcUrl={}", jdbcUrl);
		log.info("	jdbcUser={}", jdbcUser);
		log.info("	databaseOperations={}", databaseOperations);
		log.info("	startJanitor={}", startJanitor);
		log.info("	janitorThreadWait={}", janitorThreadWait);
		log.info("	lockDir={}", lockDir);
		log.info("	singleServer={}", fileLocking);
	}

	public int getMaxFilesize() {
		return maxFilesize;
	}

	public void setMaxFilesize(int maxFilesize) {
		this.maxFilesize = maxFilesize;
	}

	public String getDataDir() {
		return dataDir;
	}

	public void setDataDir(String dataDir) {
		this.dataDir = dataDir;
	}

	public int getCleanThreshold() {
		return cleanThreshold;
	}

	public void setCleanThreshold(int cleanThreshold) {
		this.cleanThreshold = cleanThreshold;
	}

	public int getTimeMoveToArchive() {
		return timeMoveToArchive;
	}

	public void setTimeMoveToArchive(int timeMoveToArchive) {
		this.timeMoveToArchive = timeMoveToArchive;
	}

	public int getNumberOfArchivingPerRun() {
		return numberOfArchivingPerRun;
	}

	public void setNumberOfArchivingPerRun(int numberOfArchivingPerRun) {
		this.numberOfArchivingPerRun = numberOfArchivingPerRun;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public String getJdbcUser() {
		return jdbcUser;
	}

	public void setJdbcUser(String jdbcUser) {
		this.jdbcUser = jdbcUser;
	}

	public String getJdbcPassword() {
		return jdbcPassword;
	}

	public void setJdbcPassword(String jdbcPassword) {
		this.jdbcPassword = jdbcPassword;
	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public boolean isStartJanitor() {
		return startJanitor;
	}

	public void setStartJanitor(boolean startJanitor) {
		this.startJanitor = startJanitor;
	}

	public String getHibernateDialect() {
		return hibernateDialect;
	}

	public void setHibernateDialect(String hibernateDialect) {
		this.hibernateDialect = hibernateDialect;
	}

	public String getDatabaseOperationsInfo() {
		return getDatabaseOperations();
	}

	public static String getDatabaseOperations() {
		return databaseOperations;
	}

	public static void setDatabaseOperations(String databaseOperations) {
		Config.databaseOperations = databaseOperations;
	}

	public int getJanitorThreadWait() {
		return janitorThreadWait;
	}

	public void setJanitorThreadWait(int janitorThreadWait) {
		this.janitorThreadWait = janitorThreadWait;
	}

	public boolean isServerActive() {
		return serverActive;
	}

	public void setServerActive(boolean serverActive) {
		this.serverActive = serverActive;
	}

	public String getLockDir() {
		return lockDir;
	}

	public void setLockDir(String lockDir) {
		this.lockDir = lockDir;
	}

	public boolean isFileLocking() {
		return fileLocking;
	}

	public void setFileLocking(boolean fileLocking) {
		this.fileLocking = fileLocking;
	}

	@Produces
	public DatabaseOperations createDatabaseOperations(@New DatabaseJdbcOperations jdbc,
			@New DatabaseHibernateOperations hibernate) {
		switch (databaseOperations) {
		case "jdbc":
			return jdbc;
		case "hibernate":
			return hibernate;
		default:
			throw new RuntimeException("Unknown database operation " + databaseOperations);
		}
	}

}
