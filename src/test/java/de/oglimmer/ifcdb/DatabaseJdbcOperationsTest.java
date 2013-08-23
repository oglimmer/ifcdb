package de.oglimmer.ifcdb;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Assert;
import org.junit.Test;

import de.oglimmer.ifcdb.integration.DatabaseSetup;

public class DatabaseJdbcOperationsTest {

	private DatabaseSetup dbsetup = DatabaseSetup.getInstanceJdbc();

	@Test
	public void checkFilenameToDateMinute() throws IOException, SQLException {

		FileBase.PATH_STRUCTURE = "yyyy/MM/dd/HH/mm/";
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");
		DateFormat df = new SimpleDateFormat(FileBase.PATH_STRUCTURE_AS_NUMBER);

		DatabaseJdbcOperations dho = (DatabaseJdbcOperations) dbsetup.getWeldContainer().instance()
				.select(DatabaseOperations.class).get();

		String dateStr = dho.getDateFromFilename("2013/08/10/17/55/1376150150270-2575622166778-1");
		Calendar cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 55, 00);
		Assert.assertEquals(df.format(cal.getTime()), dateStr);

		dateStr = dho.getDateFromFilename("2013/08/10/17/55/whatever/1376150150270-2575622166778-1");
		cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 55, 00);
		Assert.assertEquals(df.format(cal.getTime()), dateStr);

		dateStr = dho.getDateFromFilename("2013/08/10/17/55/whatever/more_sub_dirs/1376150150270-2575622166778-1");
		cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 55, 00);
		Assert.assertEquals(df.format(cal.getTime()), dateStr);
	}

	@Test
	public void checkFilenameToDateHour() throws IOException, SQLException {

		FileBase.PATH_STRUCTURE = "yyyy/MM/dd/HH/";
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");
		DateFormat df = new SimpleDateFormat(FileBase.PATH_STRUCTURE_AS_NUMBER);

		DatabaseJdbcOperations dho = (DatabaseJdbcOperations) dbsetup.getWeldContainer().instance()
				.select(DatabaseOperations.class).get();

		String dateStr = dho.getDateFromFilename("2013/08/10/17/1376150150270-2575622166778-1");
		Calendar cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 00, 00);
		Assert.assertEquals(df.format(cal.getTime()), dateStr);

		dateStr = dho.getDateFromFilename("2013/08/10/17/whatever/1376150150270-2575622166778-1");
		cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 00, 00);
		Assert.assertEquals(df.format(cal.getTime()), dateStr);

		dateStr = dho.getDateFromFilename("2013/08/10/17/whatever/more_sub_dirs/1376150150270-2575622166778-1");
		cal = GregorianCalendar.getInstance();
		cal.set(2013, 07, 10, 17, 00, 00);
		Assert.assertEquals(df.format(cal.getTime()), dateStr);
	}

	@Test
	public void checkForSecondsPatternTest() throws IOException, SQLException {

		FileBase.PATH_STRUCTURE = "yyyy/MM/dd/HH/mm/ss/";
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");

		DatabaseJdbcOperations dho = (DatabaseJdbcOperations) dbsetup.getWeldContainer().instance()
				.select(DatabaseOperations.class).get();

		Config config = dbsetup.getWeldContainer().instance().select(Config.class).get();
		config.init(dbsetup.getProperties());

		long d = dho.getArchivedToTime();

		Calendar cal = GregorianCalendar.getInstance();
		cal.add(Calendar.SECOND, -config.getTimeMoveToArchive());

		DateFormat df = new SimpleDateFormat(FileBase.PATH_STRUCTURE_AS_NUMBER);

		Assert.assertEquals(Long.parseLong(df.format(cal.getTime())), d);
	}

	@Test
	public void checkForMinutePatternTest() throws IOException, SQLException {

		FileBase.PATH_STRUCTURE = "yyyy/MM/dd/HH/mm/";
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");

		DatabaseJdbcOperations dho = (DatabaseJdbcOperations) dbsetup.getWeldContainer().instance()
				.select(DatabaseOperations.class).get();

		Config config = dbsetup.getWeldContainer().instance().select(Config.class).get();
		config.init(dbsetup.getProperties());

		long d = dho.getArchivedToTime();

		Calendar cal = GregorianCalendar.getInstance();
		cal.add(Calendar.MINUTE, -config.getTimeMoveToArchive());
		cal.set(Calendar.SECOND, 00);

		DateFormat df = new SimpleDateFormat(FileBase.PATH_STRUCTURE_AS_NUMBER);

		Assert.assertEquals(Long.parseLong(df.format(cal.getTime())), d);

	}

	@Test
	public void checkForHourPatternTest() throws IOException, SQLException {

		FileBase.PATH_STRUCTURE = "yyyy/MM/dd/HH/";
		FileBase.PATH_STRUCTURE_AS_NUMBER = FileBase.PATH_STRUCTURE.replace("/", "");

		DatabaseJdbcOperations dho = (DatabaseJdbcOperations) dbsetup.getWeldContainer().instance()
				.select(DatabaseOperations.class).get();

		Config config = dbsetup.getWeldContainer().instance().select(Config.class).get();
		config.init(dbsetup.getProperties());

		long d = dho.getArchivedToTime();

		Calendar cal = GregorianCalendar.getInstance();
		cal.add(Calendar.HOUR, -config.getTimeMoveToArchive());
		cal.set(Calendar.SECOND, 00);
		cal.set(Calendar.MINUTE, 00);

		DateFormat df = new SimpleDateFormat(FileBase.PATH_STRUCTURE_AS_NUMBER);

		Assert.assertEquals(Long.parseLong(df.format(cal.getTime())), d);

	}

}
