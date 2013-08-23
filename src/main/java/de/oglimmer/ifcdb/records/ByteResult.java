package de.oglimmer.ifcdb.records;

/**
 * Used as a data transfer object between methods.
 * 
 * @author Oli Zimpasser
 * 
 */
public class ByteResult {
	private byte[] data;
	private String mimetype;

	public ByteResult(byte[] data, String mimetype) {
		super();
		this.data = data;
		this.mimetype = mimetype;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public String getMimetype() {
		return mimetype;
	}

	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}
}
