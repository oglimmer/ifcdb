package de.oglimmer.ifcdb.integration.tester;

import java.io.Closeable;
import java.io.IOException;

public interface Data {

	byte[] createBinary(String key) throws IOException;

	byte[] getBinary(String key) throws IOException;

	Closeable getLock(String key);
}
