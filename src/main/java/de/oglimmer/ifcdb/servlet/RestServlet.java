package de.oglimmer.ifcdb.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.Config;
import de.oglimmer.ifcdb.DatabaseException;
import de.oglimmer.ifcdb.CallReference;
import de.oglimmer.ifcdb.Driver;
import de.oglimmer.ifcdb.api.Session;
import de.oglimmer.ifcdb.records.ByteResult;

/**
 * REST API servlet
 * 
 * @author Oli Zimpasser
 * 
 */
@SuppressWarnings("serial")
@WebServlet(name = "RestServlet", urlPatterns = { "/*" })
public class RestServlet extends HttpServlet {

	private Logger log = LoggerFactory.getLogger(RestServlet.class);

	@Inject
	private Driver driver;

	@PostConstruct
	public void contextInitialized() {
		Properties prop = WebConfig.INSTANCE.loadConfigFile();
		Config.setDatabaseOperations(prop.getProperty("databaseOperations"));
		driver.init(prop);
	}

	@PreDestroy
	public void contextUninitialized() {
		driver.stopServer();
	}

	private void setDebugCount(HttpServletRequest req) {
		Object o = req.getAttribute("cc");
		if (null != o) {
			CallReference.setId(o.toString());
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		setDebugCount(req);
		try {
			String key = req.getRequestURI().isEmpty() ? "" : req.getRequestURI().substring(1);
			ByteResult br = getDBSession().get(key);

			if (br != null) {
				String mimeType = br.getMimetype() != null ? br.getMimetype() : "application/octet-stream";
				resp.setContentType(mimeType);
				resp.setContentLength(br.getData().length);
				resp.getOutputStream().write(br.getData());
				log.debug("[{}] get returned {} with size of {}", CallReference.getId(), mimeType, br.getData().length);
			} else {
				resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
				resp.getWriter().print("Binary not found");
				log.debug("[{}] get returned 'Binary not found'", CallReference.getId());
			}
		} catch (DatabaseException e) {
			log.error("[{}] doGet failed", CallReference.getId(), e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		setDebugCount(req);
		try {
			String key = getRequestURI(req);

			byte[] binaryPayload = readInputStream(req);

			String mimetype = req.getHeader("Content-Type");
			String prefix = req.getHeader("X-Id-Prefix");
			String postfix = req.getHeader("X-Id-Postfix");

			key = getDBSession().put(key, mimetype, prefix, postfix, binaryPayload);

			resp.setContentType("text/plain");
			resp.setContentLength(key.length());
			resp.getWriter().write(key);

			log.debug("[{}] Wrote {} as {} ({}/{}) size of {}", CallReference.getId(), key, mimetype, prefix, postfix,
					binaryPayload.length);
		} catch (DatabaseException e) {
			log.error("[{}] doPost failed", CallReference.getId(), e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		setDebugCount(req);
		try {
			String key = req.getRequestURI().isEmpty() ? "" : req.getRequestURI().substring(1);

			Boolean res = getDBSession().remove(key);

			String answer = res.toString();

			resp.setContentType("text/plain");
			resp.setContentLength(answer.length());
			resp.getWriter().write(answer);

			log.debug("[{}] Deleted {}, answer {}", CallReference.getId(), key, answer);
		} catch (DatabaseException e) {
			log.error("[{}] doDelete failed", CallReference.getId(), e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// called by LB healthcheck
		if (!driver.getConfig().isServerActive()) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			log.info("Server disabled! Health-check returned 404.");
		}
	}

	private Session getDBSession() {
		return driver.getSession();
	}

	private String getRequestURI(HttpServletRequest req) {
		String requestURI = req.getRequestURI();

		if (requestURI.contains("../")) {
			// possible security problem
			throw new RuntimeException();
		}
		String key = requestURI.startsWith("/") ? requestURI.substring(1) : requestURI;
		if (key.isEmpty() || "null".equals(key)) {
			key = null;
		}
		return key;
	}

	private byte[] readInputStream(HttpServletRequest req) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] data = new byte[1024 * 10];
		int len = 0;
		InputStream is = req.getInputStream();

		while ((len = is.read(data)) > 0) {
			baos.write(data, 0, len);
		}
		return baos.toByteArray();
	}

}
