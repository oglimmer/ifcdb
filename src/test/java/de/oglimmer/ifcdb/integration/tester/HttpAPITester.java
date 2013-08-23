package de.oglimmer.ifcdb.integration.tester;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.DatabaseException;

public class HttpAPITester implements InterfaceType {

	private static Logger log = LoggerFactory.getLogger(HttpAPITester.class);

	private static final String BASE_URL = "http://localhost:8080/";

	@Override
	public String put(String key, String mimetype, String prefix, String postfix, byte[] data) throws DatabaseException {
		try {
			URL url = new URL(BASE_URL + (key != null ? key : ""));
			URLConnection con = url.openConnection();

			con.setDoInput(true);
			con.setDoOutput(true);

			con.addRequestProperty("Content-Type", mimetype != null ? mimetype : "application/octet-stream");
			if (prefix != null) {
				con.addRequestProperty("X-Id-Prefix", prefix);
			}
			if (postfix != null) {
				con.addRequestProperty("X-Id-Postfix", postfix);
			}

			OutputStream os = con.getOutputStream();
			os.write(data);

			String response = IOUtils.toString(con.getInputStream());

			log.debug("http-put, got:" + response);
			return response;
		} catch (IOException e) {
			throw new DatabaseException(e);
		}

	}

	@Override
	public byte[] get(String key) throws DatabaseException {
		try {
			URL url = new URL(BASE_URL + key);

			return IOUtils.toByteArray(url.openStream());
		} catch (IOException e) {
			throw new DatabaseException(e);
		}
	}

	@Override
	public boolean remove(String key) throws DatabaseException {
		try {
			URL url = new URL(BASE_URL + key);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();

			con.setRequestMethod("DELETE");

			return Boolean.parseBoolean(IOUtils.toString(con.getInputStream()));
		} catch (IOException e) {
			throw new DatabaseException(e);
		}
	}

}
