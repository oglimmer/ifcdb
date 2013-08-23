package de.oglimmer.ifcdb.stats;

import java.util.Date;

public interface JanitorMBean {

	int getNumberOfKeysArchived();

	int getNumberOfArchivesCompressed();

	int getNumberOfFilesKeptDuringCompression();

	Date getLastDance();

	int getAverageArchiveTime();

	int getAverageCompressTime();

	boolean isRunning();

	void start();

	void stop();
}
