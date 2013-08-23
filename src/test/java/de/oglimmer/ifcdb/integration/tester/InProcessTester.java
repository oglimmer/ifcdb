package de.oglimmer.ifcdb.integration.tester;

import de.oglimmer.ifcdb.DatabaseException;
import de.oglimmer.ifcdb.Driver;
import de.oglimmer.ifcdb.records.ByteResult;

public class InProcessTester implements InterfaceType {

	protected Driver driver;

	public InProcessTester(Driver driver) {
		this.driver = driver;
	}

	@Override
	public String put(String key, String mimetype, String prefix, String postfix, byte[] data) throws DatabaseException {
		return driver.getSession().put(key, mimetype, prefix, postfix, data);
	}

	@Override
	public byte[] get(String key) throws DatabaseException {
		ByteResult br = driver.getSession().get(key);
		return br != null ? br.getData() : null;
	}

	@Override
	public boolean remove(String key) throws DatabaseException {
		return driver.getSession().remove(key);
	}

}
