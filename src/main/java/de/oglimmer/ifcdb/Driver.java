package de.oglimmer.ifcdb;

import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.api.Session;
import de.oglimmer.ifcdb.stats.ConfigMBean;
import de.oglimmer.ifcdb.stats.ControlMBean;
import de.oglimmer.ifcdb.stats.JanitorMBean;
import de.oglimmer.ifcdb.stats.LockTableMBean;
import de.oglimmer.ifcdb.stats.StatisticsMBean;

/**
 * Central (applicaton scoped) entry class. Holds references to all business logic classes, the public interface and the
 * configuration.
 * 
 * @author Oli Zimpasser
 * 
 */
@ApplicationScoped
public class Driver implements ControlMBean {

	private static final String MBEAN_NAME_JANITOR = "IFCDB:type=statistics,application=Janitor";
	private static final String MBEAN_NAME_STATS = "IFCDB:type=statistics,application=Statistics";
	public static final String MBEAN_NAME_CONFIG = "IFCDB:type=statistics,application=Config";
	public static final String MBEAN_NAME_CONTROL = "IFCDB:type=statistics,application=Control";
	private static final String MBEAN_NAME_LOCKTABLE = "IFCDB:type=statistics,application=LockTable";
	public static final String MBEAN_NAME_TIMESTAT = "IFCDB:type=statistics,application=TimeStatistics";
	private static final String[] MBEAN_NAMES = { MBEAN_NAME_JANITOR, MBEAN_NAME_STATS, MBEAN_NAME_CONFIG,
			MBEAN_NAME_CONTROL, MBEAN_NAME_LOCKTABLE };

	private Logger log = LoggerFactory.getLogger(Driver.class);

	@Inject
	private Config config;

	@Inject
	private IDGenerator idGen;

	@Inject
	private SessionImpl session;

	@Inject
	private FileOperations fileOperations;

	@Inject
	private FileArchiveOperations fileArchiveOperations;

	@Inject
	private DatabaseOperations databaseOperations;

	@Inject
	private Janitor janitor;

	@Inject
	private LockTable lockTable;

	public void init(Properties prop) {
		config.init(prop);
		databaseOperations.connect();
		fileArchiveOperations.init();
		janitor.init();
		lockTable.init();
		createMBeanServer();
	}

	public Session getSession() {
		return session;
	}

	public Config getConfig() {
		return config;
	}

	IDGenerator getIdGenerator() {
		return idGen;
	}

	FileOperations getFileOperations() {
		return fileOperations;
	}

	public FileArchiveOperations getFileArchiveOperations() {
		return fileArchiveOperations;
	}

	public DatabaseOperations getDatabaseOperations() {
		return databaseOperations;
	}

	public Janitor getJanitor() {
		return janitor;
	}

	public LockTable getLockTable() {
		return lockTable;
	}

	public void stopServer() {
		lockTable.stop();
		janitor.stop();
		databaseOperations.disconnect();
		destroyMBeanServer();
		log.info("IFCDB driver successfully shutdown");
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			log.debug("Failed to wait on shutdown");
		}
	}

	private void createMBeanServer() {
		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			server.registerMBean(new StandardMBean(session, StatisticsMBean.class), new ObjectName(MBEAN_NAME_STATS));
			server.registerMBean(new StandardMBean(janitor, JanitorMBean.class), new ObjectName(MBEAN_NAME_JANITOR));
			server.registerMBean(new StandardMBean(config, ConfigMBean.class), new ObjectName(MBEAN_NAME_CONFIG));
			server.registerMBean(new StandardMBean(this, ControlMBean.class), new ObjectName(MBEAN_NAME_CONTROL));
			server.registerMBean(new StandardMBean(lockTable, LockTableMBean.class), new ObjectName(
					MBEAN_NAME_LOCKTABLE));
		} catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException
				| NotCompliantMBeanException e) {
			log.error("Failed to init MBean", e);
		}
	}

	private void destroyMBeanServer() {
		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			for (String mbeanname : MBEAN_NAMES) {
				ObjectName on = new ObjectName(mbeanname);
				if (server.isRegistered(on)) {
					server.unregisterMBean(on);
				}
			}
		} catch (MBeanRegistrationException | InstanceNotFoundException | MalformedObjectNameException e) {
			log.error("Failed to unregister MBean", e);
		}
	}

	@Override
	public void shutdown() {
		janitor.stop();
		config.setServerActive(false);
		Executors.newSingleThreadExecutor().execute(new _Runnable());
	}

	class _Runnable implements Runnable {
		@Override
		public void run() {
			try {
				int shutdownCounter = 0;
				long lastAction = -1;
				while (shutdownCounter < 3) {
					long thisAction = session.getNumberOfCreate() + session.getNumberOfReads()
							+ session.getNumberOfRemove() + session.getNumberOfUpdate();
					if (lastAction == thisAction) {
						shutdownCounter++;
					} else {
						shutdownCounter = 0;
					}
					lastAction = thisAction;
					Thread.sleep(1000);
					log.warn("System is shutting down...");
				}
			} catch (InterruptedException e) {
				log.error("InterruptedException", e);
			}
			Runtime.getRuntime().exit(0);
		}
	}
}
