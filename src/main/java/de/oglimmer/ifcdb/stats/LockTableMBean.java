package de.oglimmer.ifcdb.stats;

import java.util.Date;

public interface LockTableMBean {

	int getNumberMemLocks();

	int getNumberFileLocks();

	Date getLastDance();

	boolean isRunning();

}
