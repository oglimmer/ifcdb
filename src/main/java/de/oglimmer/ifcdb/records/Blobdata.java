package de.oglimmer.ifcdb.records;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Table: Blobdata. Central table, stores the information for a stored binary.
 * 
 * @author Oli Zimpasser
 * 
 */
@Entity
public class Blobdata {

	@Id
	private String blobkey;
	private String relativeFilename;
	private int size;
	private int offset;
	private boolean archived;
	private String mimetype;
	private Date lastUpdate;

	public Blobdata() {
		// no code here
	}

	public Blobdata(String blobkey, String relativeFilename, int size, int offset, boolean archived, String mimetype) {
		this.blobkey = blobkey;
		this.relativeFilename = relativeFilename;
		this.size = size;
		this.offset = offset;
		this.archived = archived;
		this.mimetype = mimetype;
	}

	public String getBlobkey() {
		return blobkey;
	}

	public void setBlobkey(String key) {
		this.blobkey = key;
	}

	public String getRelativeFilename() {
		return relativeFilename;
	}

	public void setRelativeFilename(String relativeFilename) {
		this.relativeFilename = relativeFilename;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public String getMimetype() {
		return mimetype;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}

	public Date getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

}
