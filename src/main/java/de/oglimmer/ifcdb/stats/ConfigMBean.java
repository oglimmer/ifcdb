package de.oglimmer.ifcdb.stats;

public interface ConfigMBean {

	int getMaxFilesize();

	void setMaxFilesize(int maxFilesize);

	String getDataDir();

	int getCleanThreshold();

	void setCleanThreshold(int cleanThreshold);

	int getTimeMoveToArchive();

	void setTimeMoveToArchive(int timeMoveToArchive);

	int getNumberOfArchivingPerRun();

	void setNumberOfArchivingPerRun(int numberOfArchivingPerRun);

	String getJdbcUrl();

	String getJdbcUser();

	String getJdbcDriver();

	boolean isStartJanitor();

	String getHibernateDialect();

	String getDatabaseOperationsInfo();

	int getJanitorThreadWait();

	void setJanitorThreadWait(int janitorThreadWait);

	boolean isServerActive();

	void setServerActive(boolean serverActive);

	String getLockDir();

	boolean isFileLocking();
}
