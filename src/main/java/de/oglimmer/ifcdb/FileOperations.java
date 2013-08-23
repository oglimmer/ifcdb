package de.oglimmer.ifcdb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.SQLException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.records.Blobdata;
import de.oglimmer.ifcdb.records.ByteResult;

/**
 * Implements write operations for all not archived files and read operations for all files.
 * 
 * @author Oli Zimpasser
 * 
 */
public class FileOperations extends FileBase {

	private Logger log = LoggerFactory.getLogger(FileOperations.class);

	@Inject
	public FileOperations(Driver driver) {
		super(driver);
	}

	@Override
	protected String getRelativeFilename(String key) throws SQLException {
		String absPath = getAbsolutePath();

		new File(absPath).mkdirs();

		return getRelativePath() + key;
	}

	public ByteResult read(String key) throws SQLException, IOException {
		ByteResult ret = null;
		Blobdata blobdata = driver.getDatabaseOperations().getBlobdata(key);
		if (null != blobdata) {
			ret = readFileOp(blobdata);
		} else {
			log.info("[{}] {} not found", CallReference.getId(), key);
		}
		return ret;
	}

	ByteResult readFileOp(Blobdata blobdata) throws IOException {
		long time = System.currentTimeMillis();
		ByteResult ret;
		File file = new File(driver.getConfig().getDataDir() + blobdata.getRelativeFilename());
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			raf.seek(blobdata.getOffset());
			byte[] buff = new byte[blobdata.getSize()];
			raf.read(buff, 0, blobdata.getSize());
			ret = new ByteResult(buff, blobdata.getMimetype());
		}
		log.debug("[{}] File read for {} of {} took {}ms", CallReference.getId(), blobdata.getBlobkey(), file.getAbsolutePath(),
				(System.currentTimeMillis() - time));
		return ret;
	}

	public void remove(String relativeFilename) {
		File file = new File(driver.getConfig().getDataDir() + relativeFilename);
		log.debug("[{}] Delete file {}", CallReference.getId(), file.getAbsolutePath());
		if (!file.delete()) {
			log.error("[{}] Failed to delete {}", CallReference.getId(), driver.getConfig().getDataDir() + relativeFilename);
		}
	}

}
