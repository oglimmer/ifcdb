package de.oglimmer.ifcdb.api;

import de.oglimmer.ifcdb.DatabaseException;
import de.oglimmer.ifcdb.records.ByteResult;

/**
 * Interface for public operations on the database.
 * 
 * @author Oli Zimpasser
 * 
 */
public interface Session {

	/**
	 * inserts/updates data for a given key (and associating the properties)
	 * 
	 * @param key
	 *            if null or not existent yet data will be inserted, if not null and key exists data is updated
	 * @param mimetype
	 * @param prefix
	 * @param postfix
	 * @param data
	 *            binary data
	 * @return the given or newly created key
	 * @throws DatabaseException
	 */
	String put(String key, String mimetype, String prefix, String postfix, byte[] data) throws DatabaseException;

	/**
	 * reads data for the given key
	 * 
	 * @param key
	 *            that should be red
	 * @return
	 * @throws DatabaseException
	 */
	ByteResult get(String key) throws DatabaseException;

	/**
	 * removes the data and key return: true if key existed
	 * 
	 * @param key
	 * @return
	 * @throws DatabaseException
	 */
	boolean remove(String key) throws DatabaseException;
}
