package de.oglimmer.ifcdb.integration.tester;

import de.oglimmer.ifcdb.DatabaseException;

public interface InterfaceType {

	String put(String key, String mimetype, String prefix, String postfix, byte[] data) throws DatabaseException;

	byte[] get(String key) throws DatabaseException;

	boolean remove(String key) throws DatabaseException;
}
