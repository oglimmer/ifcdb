package de.oglimmer.ifcdb.records;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Table: FileRecord. Stores the information how large a certain data file currently is (that also means what the offset
 * for the next appended file is).
 * 
 * @author Oli Zimpasser
 * 
 */
@Entity
public class FileRecord {

	@Id
	private String filename;
	private int size;

	public FileRecord() {
		super();
	}

	public FileRecord(String filename, int size) {
		super();
		this.filename = filename;
		this.size = size;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}
