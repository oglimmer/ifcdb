package de.oglimmer.ifcdb;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Assert;
import org.junit.Test;

import de.oglimmer.ifcdb.integration.DatabaseSetup;

public class DatabaseHibernateOperationsTest {

	private DatabaseSetup dbsetup = DatabaseSetup.getInstanceHibernate();

	@Test
	public void checkFilenameToDateSecond() throws IOException, SQLException {

		FileBase.PATH_STRUCTURE = "yyyy/MM/dd/HH/mm/ss/";
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");

		DatabaseHibernateOperations dho = (DatabaseHibernateOperations) dbsetup.getWeldContainer().instance()
				.select(DatabaseOperations.class).get();

		Date d = dho.getDateFromFilename("2013/08/10/17/55/33/1");
		Calendar cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 55, 33);
		Assert.assertEquals(cal.getTime().toString(), d.toString());

	}

	@Test
	public void checkFilenameToDateMinute() throws IOException, SQLException {

		FileBase.PATH_STRUCTURE = "yyyy/MM/dd/HH/mm/";
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");

		DatabaseHibernateOperations dho = (DatabaseHibernateOperations) dbsetup.getWeldContainer().instance()
				.select(DatabaseOperations.class).get();

		Date d = dho.getDateFromFilename("2013/08/10/17/55/1376150150270-2575622166778-1");
		Calendar cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 55, 00);
		Assert.assertEquals(cal.getTime().toString(), d.toString());

		d = dho.getDateFromFilename("2013/08/10/17/55/whatever/1376150150270-2575622166778-1");
		cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 55, 00);
		Assert.assertEquals(cal.getTime().toString(), d.toString());

		d = dho.getDateFromFilename("2013/08/10/17/55/whatever/more_sub_dirs/1376150150270-2575622166778-1");
		cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 55, 00);
		Assert.assertEquals(cal.getTime().toString(), d.toString());
	}

	@Test
	public void checkFilenameToDateHour() throws IOException, SQLException {

		FileBase.PATH_STRUCTURE = "yyyy/MM/dd/HH/";
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");

		DatabaseHibernateOperations dho = (DatabaseHibernateOperations) dbsetup.getWeldContainer().instance()
				.select(DatabaseOperations.class).get();

		Date d = dho.getDateFromFilename("2013/08/10/17/1376150150270-2575622166778-1");
		Calendar cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 00, 00);
		Assert.assertEquals(cal.getTime().toString(), d.toString());

		d = dho.getDateFromFilename("2013/08/10/17/whatever/1376150150270-2575622166778-1");
		cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 00, 00);
		Assert.assertEquals(cal.getTime().toString(), d.toString());

		d = dho.getDateFromFilename("2013/08/10/17/whatever/more_sub_dirs/1376150150270-2575622166778-1");
		cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 00, 00);
		Assert.assertEquals(cal.getTime().toString(), d.toString());
	}

	@Test
	public void checkForSecondsPatternTest() throws IOException, SQLException {

		FileBase.PATH_STRUCTURE = "yyyy/MM/dd/HH/mm/ss/";
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");

		DatabaseHibernateOperations dho = (DatabaseHibernateOperations) dbsetup.getWeldContainer().instance()
				.select(DatabaseOperations.class).get();

		Config config = dbsetup.getWeldContainer().instance().select(Config.class).get();
		config.init(dbsetup.getProperties());

		Date d = dho.getArchivedToDate();

		Calendar cal = GregorianCalendar.getInstance();
		cal.add(Calendar.SECOND, -config.getTimeMoveToArchive());

		Assert.assertEquals(cal.getTime().toString(), d.toString());

	}

	@Test
	public void checkForMinutePatternTest() throws IOException, SQLException {

		FileBase.PATH_STRUCTURE = "yyyy/MM/dd/HH/mm/";
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");

		DatabaseHibernateOperations dho = (DatabaseHibernateOperations) dbsetup.getWeldContainer().instance()
				.select(DatabaseOperations.class).get();

		Config config = dbsetup.getWeldContainer().instance().select(Config.class).get();
		config.init(dbsetup.getProperties());

		Date d = dho.getArchivedToDate();

		Calendar cal = GregorianCalendar.getInstance();
		cal.add(Calendar.MINUTE, -config.getTimeMoveToArchive());
		cal.set(Calendar.SECOND, 00);

		Assert.assertEquals(cal.getTime().toString(), d.toString());

	}

	@Test
	public void checkForHourPatternTest() throws IOException, SQLException {

		FileBase.PATH_STRUCTURE = "yyyy/MM/dd/HH/";
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");

		DatabaseHibernateOperations dho = (DatabaseHibernateOperations) dbsetup.getWeldContainer().instance()
				.select(DatabaseOperations.class).get();

		Config config = dbsetup.getWeldContainer().instance().select(Config.class).get();
		config.init(dbsetup.getProperties());

		Date d = dho.getArchivedToDate();

		Calendar cal = GregorianCalendar.getInstance();
		cal.add(Calendar.HOUR, -config.getTimeMoveToArchive());
		cal.set(Calendar.SECOND, 00);
		cal.set(Calendar.MINUTE, 00);

		Assert.assertEquals(cal.getTime().toString(), d.toString());

	}

}
