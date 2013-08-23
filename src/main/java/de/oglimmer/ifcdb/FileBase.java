package de.oglimmer.ifcdb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File base class.
 * 
 * @author Oli Zimpasser
 * 
 */
public abstract class FileBase {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public static String PATH_STRUCTURE = "yyyy/MM/dd/HH/";
	public static String PATH_STRUCTURE_AS_NUMBER = PATH_STRUCTURE.replace("/", "");

	protected Driver driver;

	protected boolean append;

	public FileBase(Driver driver) {
		this.driver = driver;
		this.append = false;
	}

	abstract protected String getRelativeFilename(String key) throws SQLException, IOException;

	public String write(String key, byte[] data) throws SQLException, IOException {
		long time = System.currentTimeMillis();

		String relFilename = getRelativeFilename(key);
		File file = new File(driver.getConfig().getDataDir() + relFilename);
		File path = file.getParentFile();
		if (!path.exists()) {
			path.mkdirs();
		}
		try (RandomAccessFile fos = new RandomAccessFile(file, "rw")) {
			fos.seek(file.length());
			fos.write(data);
		}

		log.debug("[{}] Wrote {} to disk at {} in {}ms", CallReference.getId(), key, file.getAbsolutePath(),
				(System.currentTimeMillis() - time));
		return relFilename;

	}

	protected String getAbsolutePath() {
		DateFormat df = new SimpleDateFormat(PATH_STRUCTURE);
		return driver.getConfig().getDataDir() + df.format(new Date());
	}

	protected String getRelativePath() {
		DateFormat df = new SimpleDateFormat(PATH_STRUCTURE);
		return df.format(new Date());
	}

}
