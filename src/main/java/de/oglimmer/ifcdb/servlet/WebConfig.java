package de.oglimmer.ifcdb.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles configuration management for the web application
 * 
 * @author Oli Zimpasser
 * 
 */
public enum WebConfig {
	INSTANCE;

	private Logger log = LoggerFactory.getLogger(RestServlet.class);

	public Properties loadConfigFile() {
		Properties prop = new Properties();
		String configFile = getConfigFilename();
		try (InputStream is = getInputStream(configFile)) {
			if (null != is) {
				prop.load(is);
			} else {
				log.debug("Configfile not found, using defaults");
			}
		} catch (IOException e) {
			log.error("Failed to load config file", e);
		}
		return prop;
	}

	private InputStream getInputStream(String configFile) throws FileNotFoundException {
		InputStream is = getClass().getResourceAsStream(configFile);
		if (null == is) {
			File f = new File(configFile);
			if (f.exists()) {
				is = new FileInputStream(f);
				log.debug("Reading configfile via FileInputStream");
			}
		} else {
			log.debug("Reading configfile via getResourceAsStream");
		}
		return is;
	}

	private String getConfigFilename() {
		String configFile = "/etc/ifcdb.config";
		if (null != System.getProperty("ifcdb.config")) {
			configFile = System.getProperty("ifcdb.config");
		}
		log.debug("Using configfile:" + configFile);
		return configFile;
	}

}
