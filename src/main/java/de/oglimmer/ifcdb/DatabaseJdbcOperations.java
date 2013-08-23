package de.oglimmer.ifcdb;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import de.oglimmer.ifcdb.records.Blobdata;

/**
 * Plain vanilla JDBC implementation of db access.
 * 
 * @author Oli Zimpasser
 * 
 */
@Alternative
public class DatabaseJdbcOperations implements DatabaseOperations {

	private Logger log = LoggerFactory.getLogger(DatabaseJdbcOperations.class);

	private Driver driver;

	private ComboPooledDataSource cpds;

	@Inject
	public DatabaseJdbcOperations(Driver driver) {
		this.driver = driver;
	}

	@Override
	public void connect() {
		try {
			cpds = new ComboPooledDataSource();
			cpds.setDriverClass(driver.getConfig().getJdbcDriver());
			cpds.setJdbcUrl(driver.getConfig().getJdbcUrl());
			cpds.setUser(driver.getConfig().getJdbcUser());
			cpds.setPassword(driver.getConfig().getJdbcPassword());

			// the settings below are optional -- c3p0 can work with defaults
			cpds.setMinPoolSize(5);
			cpds.setAcquireIncrement(5);
			cpds.setMaxPoolSize(20);
		} catch (PropertyVetoException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addEntry(String key, String filename, int length, String mimetype) throws SQLException {
		try (Connection con = cpds.getConnection()) {
			con.setAutoCommit(false);
			try {
				Blobdata blobdata = getBlobdata(con, key);
				if (blobdata != null) {
					if (blobdata.isArchived()) {
						addRemovalfileRecord(con, blobdata.getRelativeFilename(), blobdata.getSize());
					}
					update(con, "update Blobdata set relativeFilename=?,size=?,offset=0,archived=0,lastUpdate=? where blobkey=?", filename,
							length, getDateFromFilename(filename), key);
				} else {
					update(con,
							"insert into Blobdata (blobkey, relativeFilename, size,offset,archived,lastUpdate,mimetype) values (?,?,?,0,0,?,?)",
							key, filename, length, getDateFromFilename(filename), mimetype);
				}
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		}
	}

	@Override
	public void addArchivedEntry(String key, String filename, int length) throws SQLException {
		try (Connection con = cpds.getConnection()) {
			con.setAutoCommit(false);
			try {
				int offset = getInt(con, "select size from FileRecord where filename=?", filename);

				update(con, "update Blobdata set relativeFilename=?,offset=?,size=?,archived=1,lastUpdate=? where blobkey=?", filename, offset,
						length, getDateFromFilename(filename), key);

				update(con, "update FileRecord set size=size+? where filename = ?", length, filename);
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		}
	}

	@Override
	public void createFileRecord(String filename) throws SQLException {
		try (Connection con = cpds.getConnection()) {
			con.setAutoCommit(false);
			try {
				update(con, "insert into FileRecord (filename,size) values (?,0)", filename);
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		}
	}

	@Override
	public void addRemovalfileRecord(String filename, int length) throws SQLException {
		try (Connection con = cpds.getConnection()) {
			con.setAutoCommit(false);
			try {
				addRemovalfileRecord(con, filename, length);
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		}
	}

	@Override
	public void removeBlobdata(String key) throws SQLException {
		try (Connection con = cpds.getConnection()) {
			con.setAutoCommit(false);
			try {
				update(con, "delete from Blobdata where blobkey=?", key);
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		}
	}

	@Override
	public int getSizeFromFileRecord(String filename) throws SQLException {
		return getInt("select size from FileRecord where filename=?", filename);
	}

	@Override
	public void removeRemovalfileRecord(String filename) throws SQLException {
		try (Connection con = cpds.getConnection()) {
			con.setAutoCommit(false);
			try {
				update(con, "delete from RemovalfileRecord where filename=?", filename);
				update(con, "delete from FileRecord where filename=?", filename);
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		}
	}

	@Override
	public void disconnect() {
		cpds.close();
	}

	@Override
	public Blobdata getBlobdata(String key) throws SQLException {
		try (Connection con = cpds.getConnection()) {
			return getBlobdata(con, key);
		}
	}

	@Override
	public Collection<Blobdata> getBlobdataByFilename(String filename) throws SQLException {
		try (Connection con = cpds.getConnection()) {
			String sql = "select blobkey, relativeFilename, size, offset, archived, mimetype from Blobdata where relativeFilename=?";
			Collection<Blobdata> ret = new ArrayList<>();
			try (PreparedStatement prst = con.prepareStatement(sql)) {
				prst.setString(1, filename);
				try (ResultSet rs = prst.executeQuery()) {
					while (rs.next()) {
						ret.add(new Blobdata(rs.getString("blobkey"), rs.getString("relativeFilename"), rs.getInt("size"), rs.getInt("offset"),
								rs.getBoolean("archived"), rs.getString("mimetype")));
					}
				}
			}
			return ret;
		}
	}

	@Override
	public Collection<String> getOpenRemovals() throws SQLException {
		try (Connection con = cpds.getConnection()) {
			Collection<String> ret = new ArrayList<>();
			try (PreparedStatement prst = con
					.prepareStatement("select filename from RemovalfileRecord where size > ? or size=(select size from FileRecord where FileRecord.filename=RemovalfileRecord.filename)")) {
				prst.setInt(1, driver.getConfig().getCleanThreshold());
				try (ResultSet rs = prst.executeQuery()) {
					while (rs.next()) {
						ret.add(rs.getString(1));
					}
				}
			}
			return ret;
		}

	}

	@Override
	public Collection<Blobdata> getToBeArchived() throws SQLException {
		try (Connection con = cpds.getConnection()) {
			String sql = "select blobkey, relativeFilename, size, offset, archived, mimetype from Blobdata where archived=0 and lastupdate < ?";
			Collection<Blobdata> ret = new ArrayList<>();
			try (PreparedStatement prst = con.prepareStatement(sql)) {
				prst.setLong(1, getArchivedToTime());
				try (ResultSet rs = prst.executeQuery()) {
					while (rs.next()) {
						ret.add(new Blobdata(rs.getString("blobkey"), rs.getString("relativeFilename"), rs.getInt("size"), rs.getInt("offset"),
								rs.getBoolean("archived"), rs.getString("mimetype")));
					}
				}
			}
			return ret;
		}

	}

	@Override
	public void removeArchived(String key, String filename, int size) throws SQLException {
		try (Connection con = cpds.getConnection()) {
			con.setAutoCommit(false);
			try {
				addRemovalfileRecord(con, filename, size);
				removeBlobdata(con, key);

				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		}
	}

	@Override
	public Integer getInt(String sql, Object... params) throws SQLException {
		try (Connection con = cpds.getConnection()) {
			return getInt(con, sql, params);
		}

	}

	private void removeBlobdata(Connection con, String key) throws SQLException {
		update(con, "delete from blobdata where blobkey=?", key);
	}

	private void addRemovalfileRecord(Connection con, String filename, int length) throws SQLException {
		if (getInt(con, "select 1 from RemovalfileRecord where filename=?", filename) != null) {
			update(con, "update RemovalfileRecord set size=size+? where filename=?", length, filename);
		} else {
			update(con, "insert into RemovalfileRecord (filename,size) values (?,?)", filename, length);
		}
	}

	private Blobdata getBlobdata(Connection con, String key) throws SQLException {
		Blobdata ret = null;
		String sql = "select blobkey, relativeFilename, size, offset,archived,mimetype from Blobdata where blobkey=?";
		try (PreparedStatement prst = con.prepareStatement(sql)) {
			prst.setString(1, key);
			try (ResultSet rs = prst.executeQuery()) {
				if (rs.next()) {
					ret = new Blobdata(rs.getString("blobkey"), rs.getString("relativeFilename"), rs.getInt("size"), rs.getInt("offset"),
							rs.getBoolean("archived"), rs.getString("mimetype"));
				}
			}
		}
		return ret;
	}

	private Integer getInt(Connection con, String sql, Object... params) throws SQLException {
		Integer ret;
		try (PreparedStatement prst = con.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) {
				prst.setObject(i + 1, params[i]);
			}
			try (ResultSet rs = prst.executeQuery()) {
				if (rs.next()) {
					ret = rs.getInt(1);
				} else {
					ret = null;
				}
			}
		}
		return ret;
	}

	public void update(String sql, Object... params) throws SQLException {
		try (Connection con = cpds.getConnection()) {
			con.setAutoCommit(false);
			try {
				update(con, sql, params);
				con.commit();
			} catch (SQLException e) {
				con.rollback();
				throw e;
			}
		}
	}

	private void update(Connection con, String sql, Object... params) throws SQLException {
		try (PreparedStatement prst = con.prepareStatement(sql)) {
			for (int i = 0; i < params.length; i++) {
				prst.setObject(i + 1, params[i]);
			}
			if (prst.executeUpdate() == 0) {
				log.debug("[{}] No row(s) changed with update {}", CallReference.getId(), sql);
			}
		}
	}

	long getArchivedToTime() {
		DateFormat df = new SimpleDateFormat(FileBase.PATH_STRUCTURE_AS_NUMBER);
		return Long.parseLong(df.format(new Date())) - driver.getConfig().getTimeMoveToArchive();
	}

	String getDateFromFilename(String filename) {
		int pos = 0;
		for (int i = 0; i < StringUtils.countMatches(FileBase.PATH_STRUCTURE, "/"); i++) {
			pos = filename.indexOf('/', pos) + 1;
		}
		return filename.substring(0, pos - 1).replace("/", "");
	}
}
