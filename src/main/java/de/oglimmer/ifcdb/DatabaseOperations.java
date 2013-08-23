package de.oglimmer.ifcdb;

import java.sql.SQLException;
import java.util.Collection;

import de.oglimmer.ifcdb.records.Blobdata;

/**
 * Defines all possible database operations.
 * 
 * @author Oli Zimpasser
 * 
 */
public interface DatabaseOperations {

	void connect();

	void disconnect();

	void addEntry(String key, String filename, int length, String mimetype) throws SQLException;

	void addArchivedEntry(String blobkey, String filename, int length) throws SQLException;

	Blobdata getBlobdata(String key) throws SQLException;

	Collection<Blobdata> getBlobdataByFilename(String filename) throws SQLException;

	void removeBlobdata(String key) throws SQLException;

	Collection<String> getOpenRemovals() throws SQLException;

	Collection<Blobdata> getToBeArchived() throws SQLException;

	int getSizeFromFileRecord(String filename) throws SQLException;

	void createFileRecord(String filename) throws SQLException;

	void addRemovalfileRecord(String filename, int size) throws SQLException;

	void removeRemovalfileRecord(String filename) throws SQLException;

	void removeArchived(String key, String filename, int size) throws SQLException;

	Integer getInt(String sql, Object... params) throws SQLException;

	void update(String sql, Object... params) throws SQLException;
}
