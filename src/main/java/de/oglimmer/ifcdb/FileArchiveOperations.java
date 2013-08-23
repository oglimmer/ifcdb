package de.oglimmer.ifcdb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.sql.SQLException;
import java.util.Collection;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements write operations for archives.
 * 
 * @author Oli Zimpasser
 * 
 */
public class FileArchiveOperations extends FileBase {

	private final static String PREFIX_FILENAME = "data";
	private Logger log = LoggerFactory.getLogger(FileArchiveOperations.class);

	private long counter;

	@Inject
	public FileArchiveOperations(Driver driver) {
		super(driver);
		this.append = true;

	}

	public void init() {
		try {
			try {
				readCounterFile();
			} catch (NoSuchFileException e) {
				updateCounterFile(true);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		log.debug("Using counter {}", counter);
	}

	private void updateCounterFile(boolean create) throws IOException {
		if (create) {
			if (new File(driver.getConfig().getDataDir()).list() != null) {
				throw new RuntimeException(
						"Failed to start, could not find counter file but data directory is not empty");
			}
			new File(driver.getConfig().getDataDir()).mkdirs();
		}
		File dataRoot = new File(driver.getConfig().getDataDir() + "counter");
		try (FileOutputStream fos = new FileOutputStream(dataRoot)) {
			fos.write(Long.toString(counter).getBytes());
		}
	}

	private void readCounterFile() throws IOException {
		File dataRoot = new File(driver.getConfig().getDataDir() + "counter");
		Collection<String> lines = Files.readAllLines(dataRoot.toPath(), Charset.defaultCharset());
		counter = Long.parseLong(lines.iterator().next());
	}

	String getCurrentFile() {
		return getRelativePath() + PREFIX_FILENAME + counter;
	}

	@Override
	protected String getRelativeFilename(String key) throws SQLException, IOException {
		String absPath = getAbsolutePath();

		new File(absPath).mkdirs();

		String relFilename = getRelativePath() + PREFIX_FILENAME + counter;

		ensureFileExistens(relFilename);

		return ensureFileNotTooBig(relFilename);
	}

	private String ensureFileNotTooBig(String filename) throws SQLException, IOException {
		int currentFilesize = driver.getDatabaseOperations().getSizeFromFileRecord(filename);
		if (currentFilesize > driver.getConfig().getMaxFilesize()) {
			counter++;
			updateCounterFile(false);
			filename = getRelativePath() + PREFIX_FILENAME + counter;
			ensureFileExistens(filename);
			log.debug("Updated counter to {}", counter);
		}
		return filename;
	}

	private void ensureFileExistens(String relFilename) throws SQLException {
		if (!new File(driver.getConfig().getDataDir() + relFilename).exists()) {
			driver.getDatabaseOperations().createFileRecord(relFilename);
		}
	}
}
