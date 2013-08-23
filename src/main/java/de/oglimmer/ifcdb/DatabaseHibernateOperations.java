package de.oglimmer.ifcdb;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.StaleStateException;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.records.Blobdata;
import de.oglimmer.ifcdb.records.FileRecord;
import de.oglimmer.ifcdb.records.RemovalfileRecord;

/**
 * Hibernate specific implementation of db access.
 * 
 * @author Oli Zimpasser
 * 
 */
@Alternative
public class DatabaseHibernateOperations implements DatabaseOperations {

	private Logger log = LoggerFactory.getLogger(DatabaseHibernateOperations.class);

	private Driver driver;

	private SessionFactory factory;

	@Inject
	public DatabaseHibernateOperations(Driver driver) {
		this.driver = driver;
	}

	@Override
	public void connect() {
		Configuration configuration = new Configuration();
		configuration.configure().addAnnotatedClass(Blobdata.class).addAnnotatedClass(FileRecord.class)
				.addAnnotatedClass(RemovalfileRecord.class);
		Properties prop = configuration.getProperties();
		useConfig(prop);

		ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(prop).buildServiceRegistry();
		factory = configuration.buildSessionFactory(serviceRegistry);
	}

	private void useConfig(Properties prop) {
		if (null != driver.getConfig().getJdbcDriver()) {
			prop.setProperty("hibernate.connection.driver_class", driver.getConfig().getJdbcDriver());
		}
		if (null != driver.getConfig().getJdbcUser()) {
			prop.setProperty("hibernate.connection.username", driver.getConfig().getJdbcUser());
		}
		if (null != driver.getConfig().getJdbcPassword()) {
			prop.setProperty("hibernate.connection.password", driver.getConfig().getJdbcPassword());
		}
		if (null != driver.getConfig().getJdbcUrl()) {
			prop.setProperty("hibernate.connection.url", driver.getConfig().getJdbcUrl());
		}
		if (null != driver.getConfig().getHibernateDialect()) {
			prop.setProperty("hibernate.dialect", driver.getConfig().getHibernateDialect());
		}
	}

	@Override
	public Blobdata getBlobdata(String key) {
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();
			Blobdata ret = getBlobdata(key, session);
			trans.commit();
			return ret;
		} finally {
			session.close();
		}
	}

	@Override
	public void addEntry(String key, String filename, int length, String mimetype) {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();

			Blobdata blobdata = getBlobdata(key, session);
			if (blobdata != null) {
				if (blobdata.isArchived()) {
					// since the archived file could be added from different keys, we could try this one from
					// multiple threads in parallel, therefore do it the "safe" way
					addRemovalfileRecordSafe(blobdata.getRelativeFilename(), blobdata.getSize());
				}

				blobdata.setRelativeFilename(filename);
				blobdata.setSize(length);
				blobdata.setLastUpdate(getDateFromFilename(filename));
				blobdata.setArchived(false);
				blobdata.setOffset(0);

				session.update(blobdata);
			} else {

				blobdata = new Blobdata(key, filename, length, 0, false, mimetype);
				blobdata.setLastUpdate(getDateFromFilename(filename));

				session.insert(blobdata);
			}
			trans.commit();
			log.debug("[{}] Added entry for {} in {}ms", CallReference.getId(), key,
					(System.currentTimeMillis() - time));
		} catch (Exception e) {
			if (null != trans) {
				trans.rollback();
			}
			throw new RuntimeException(e);
		} finally {
			session.close();
		}
	}

	@Override
	public void addArchivedEntry(String key, String filename, int length) {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();

			FileRecord fr = (FileRecord) session.get(FileRecord.class, filename);

			int offset = fr.getSize();

			Blobdata blobdata = getBlobdata(key, session);

			blobdata.setRelativeFilename(filename);
			blobdata.setOffset(offset);
			blobdata.setSize(length);
			blobdata.setLastUpdate(getDateFromFilename(filename));
			blobdata.setArchived(true);

			session.update(blobdata);

			fr.setSize(fr.getSize() + length);

			session.update(fr);

			trans.commit();
			log.debug("[{}] Added archivedEntry for {} in {}ms", CallReference.getId(), key,
					(System.currentTimeMillis() - time));
		} catch (Exception e) {
			if (null != trans) {
				trans.rollback();
			}
			throw new RuntimeException(e);
		} finally {
			session.close();
		}
	}

	@Override
	public void createFileRecord(String filename) {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();

			FileRecord fr = new FileRecord();
			fr.setFilename(filename);

			session.insert(fr);

			trans.commit();
			log.debug("[{}] Created FileRecord for {} in {}ms", CallReference.getId(), filename,
					(System.currentTimeMillis() - time));
		} catch (Exception e) {
			if (null != trans) {
				trans.rollback();
			}
			throw new RuntimeException(e);
		} finally {
			session.close();
		}
	}

	private void addRemovalfileRecordSafe(String filename, int length) {
		int tryCounter = 3;
		while (tryCounter > 0) {
			try {
				addRemovalfileRecord(filename, length);
				tryCounter = 0;
			} catch (RuntimeException e) {
				if (e.getCause() instanceof ConstraintViolationException) {
					tryCounter--;
					if (tryCounter == 0) {
						throw e;
					}
					try {
						Thread.sleep(250);
					} catch (InterruptedException e1) {
						log.error("Thread interrupted", e1);
					}
				} else {
					throw e;
				}
			}
		}
	}

	@Override
	public void addRemovalfileRecord(String filename, int length) {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();

			addRemovalfileRecord(session, filename, length);

			trans.commit();
			log.debug("[{}] Created RemovalfileRecord for {} in {}ms", CallReference.getId(), filename,
					(System.currentTimeMillis() - time));
		} catch (Exception e) {
			if (null != trans) {
				trans.rollback();
			}
			throw new RuntimeException(e);
		} finally {
			session.close();
		}
	}

	@Override
	public void removeBlobdata(String key) {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();

			session.delete(getBlobdata(key, session));

			trans.commit();
			log.debug("[{}] Removed Blobdata for {} in {}ms", CallReference.getId(), key,
					(System.currentTimeMillis() - time));
		} catch (Exception e) {
			if (null != trans) {
				trans.rollback();
			}
			throw new RuntimeException(e);
		} finally {
			session.close();
		}
	}

	@Override
	public int getSizeFromFileRecord(String filename) {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();
			FileRecord fr = (FileRecord) session.get(FileRecord.class, filename);
			trans.commit();
			log.debug("[{}] getSizeFromFileRecord for {} will return {} in {}ms", CallReference.getId(), filename, fr.getSize(),
					(System.currentTimeMillis() - time));
			return fr.getSize();
		} finally {
			session.close();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<String> getOpenRemovals() {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();

			String sql = "select filename from RemovalfileRecord where  size > :size "
					+ " or size=(select size from FileRecord where FileRecord.filename=RemovalfileRecord.filename)";

			Collection<String> ret = (Collection<String>) session.createSQLQuery(sql).setLong("size", driver.getConfig().getCleanThreshold())
					.list();

			trans.commit();
			log.debug("[{}] getOpenRemovals will return {} in {}ms", CallReference.getId(), ret.size(),
					(System.currentTimeMillis() - time));
			return ret;
		} finally {
			session.close();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Blobdata> getBlobdataByFilename(String filename) {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();
			Collection<Blobdata> ret = (Collection<Blobdata>) session.createQuery("from Blobdata where relativeFilename= :relativeFilename")
					.setString("relativeFilename", filename).list();

			trans.commit();
			log.debug("[{}] getBlobdataByFilename for {} will return {} in {}ms", CallReference.getId(), filename, ret.size(),
					(System.currentTimeMillis() - time));
			return ret;
		} finally {
			session.close();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<Blobdata> getToBeArchived() {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();
			Date date = getArchivedToDate();
			Collection<Blobdata> ret = (Collection<Blobdata>) session.createQuery("from Blobdata where archived=0 and lastupdate < :lastupdate")
					.setTimestamp("lastupdate", date).list();

			trans.commit();
			log.debug("[{}] getToBeArchived since {} will return {} in {}ms", CallReference.getId(), date, ret.size(),
					(System.currentTimeMillis() - time));
			return ret;
		} finally {
			session.close();
		}
	}

	@Override
	public void removeRemovalfileRecord(String filename) {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();

			FileRecord fr = (FileRecord) session.get(FileRecord.class, filename);
			session.delete(fr);

			RemovalfileRecord rfr = (RemovalfileRecord) session.get(RemovalfileRecord.class, filename);
			session.delete(rfr);

			trans.commit();
			log.debug("[{}] Remove RemovalfileRecord for {} in {}ms", CallReference.getId(), filename,
					(System.currentTimeMillis() - time));
		} catch (StaleStateException e) {
			if (null != trans) {
				trans.rollback();
			}
			if (e.getMessage().contains("Batch update returned unexpected row count from update")) {
				// ignore that. the records was already deleted.
				log.debug(
						"[{}] Remove RemovalfileRecord failed for {} due to 'Batch update returned unexpected row count from update' in {}ms",
						CallReference.getId(), filename, (System.currentTimeMillis() - time));
			} else {
				throw e;
			}
		} catch (Exception e) {
			if (null != trans) {
				trans.rollback();
			}
			throw new RuntimeException(e);
		} finally {
			session.close();
		}
	}

	@Override
	public void removeArchived(String key, String filename, int size) {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();

			addRemovalfileRecord(session, filename, size);
			removeBlobdata(session, key);

			trans.commit();
			log.debug("[{}] Remove Archived for {}/{} in {}ms", CallReference.getId(), key, filename,
					(System.currentTimeMillis() - time));
		} catch (Exception e) {
			if (null != trans) {
				trans.rollback();
			}
			throw new RuntimeException(e);
		} finally {
			session.close();
		}
	}

	@Override
	public void disconnect() {
		if (null != factory) {
			factory.close();
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Integer getInt(String sql, Object... params) {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();

			int i = 0;
			Query q = session.createSQLQuery(sql);
			for (Object o : params) {
				q.setParameter(i++, o);
			}

			Number res = null;

			List list = q.list();
			if (!list.isEmpty()) {
				res = (Number) list.get(0);
			}

			trans.commit();
			log.debug("[{}] getInt for {} in {}ms", CallReference.getId(), sql,
					(System.currentTimeMillis() - time));
			return res != null ? res.intValue() : null;
		} finally {
			session.close();
		}
	}

	private void removeBlobdata(StatelessSession session, String key) {
		long time = System.currentTimeMillis();
		session.delete(getBlobdata(key, session));
		log.debug("[{}] Remove Blobdata for {} in {}ms", CallReference.getId(), key,
				(System.currentTimeMillis() - time));
	}

	private Blobdata getBlobdata(String key, StatelessSession session) {
		long time = System.currentTimeMillis();
		Blobdata bd = (Blobdata) session.get(Blobdata.class, key);
		log.debug("[{}] Get Blobdata for {} [{}] in {}ms", CallReference.getId(), key, bd != null, (System.currentTimeMillis() - time));
		return bd;
	}

	private void addRemovalfileRecord(StatelessSession session, String filename, int length) {
		RemovalfileRecord rfr = (RemovalfileRecord) session.get(RemovalfileRecord.class, filename);

		if (rfr != null) {

			rfr.setSize(rfr.getSize() + length);

			session.update(rfr);
		} else {

			rfr = new RemovalfileRecord();
			rfr.setFilename(filename);
			rfr.setSize(length);

			session.insert(rfr);
		}
	}

	Date getArchivedToDate() {
		try {
			DateFormat df = new SimpleDateFormat(FileBase.PATH_STRUCTURE_AS_NUMBER);
			long lastUpdate = Long.parseLong(df.format(new Date())) - driver.getConfig().getTimeMoveToArchive();
			return df.parse(Long.toString(lastUpdate));
		} catch (ParseException e) {
			throw new RuntimeException("Failed to getArchivedToDate", e);
		}
	}

	Date getDateFromFilename(String filename) {
		try {
			int pos = 0;
			for (int i = 0; i < StringUtils.countMatches(FileBase.PATH_STRUCTURE, "/"); i++) {
				pos = filename.indexOf('/', pos) + 1;
			}
			String asString = filename.substring(0, pos - 1).replace("/", "");
			DateFormat df = new SimpleDateFormat(FileBase.PATH_STRUCTURE_AS_NUMBER);
			return df.parse(asString);
		} catch (ParseException e) {
			throw new RuntimeException("Failed to date-time from relativeFilename " + filename, e);
		}
	}

	@Override
	public void update(String sql, Object... params) throws SQLException {
		long time = System.currentTimeMillis();
		StatelessSession session = factory.openStatelessSession();
		Transaction trans = null;
		try {
			trans = session.beginTransaction();

			int i = 0;
			Query q = session.createSQLQuery(sql);
			for (Object o : params) {
				q.setParameter(i++, o);
			}

			q.executeUpdate();

			trans.commit();
			log.debug("[{}] Update for {} in {}ms", CallReference.getId(), sql,
					(System.currentTimeMillis() - time));
		} catch (Exception e) {
			if (null != trans) {
				trans.rollback();
			}
			throw new RuntimeException(e);
		} finally {
			session.close();
		}
	}

}
