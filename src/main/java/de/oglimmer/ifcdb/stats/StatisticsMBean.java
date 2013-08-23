package de.oglimmer.ifcdb.stats;

public interface StatisticsMBean {

	int getNumberOfReads();

	int getNumberOfCreate();

	int getNumberOfUpdate();

	int getNumberOfRemove();

	String getCurrentGets();

	String getCurrentPuts();

	String getCurrentRemoves();
}
