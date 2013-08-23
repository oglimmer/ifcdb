package de.oglimmer.ifcdb.records;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Table: RemovalfileRecord. Stores the information how much space is wasted in a certain file.
 * 
 * @author Oli Zimpasser
 * 
 */
@Entity
public class RemovalfileRecord {

	@Id
	private String filename;
	private int size;

	public RemovalfileRecord() {
		super();
	}

	public RemovalfileRecord(String filename, int size) {
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
