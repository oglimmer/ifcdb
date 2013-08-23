package de.oglimmer.ifcdb.servlet;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.oglimmer.ifcdb.Driver;
import de.oglimmer.ifcdb.stats.TimeStatMBean;

/**
 * Provides debug time logging
 * 
 * @author Oli Zimpasser
 * 
 */
@WebFilter(urlPatterns = { "/*" })
public class ProcessingTimeFilter implements Filter, TimeStatMBean {

	private Logger log = LoggerFactory.getLogger(ProcessingTimeFilter.class);

	private int numberOfGET;
	private int numberOfPOST;
	private int numberOfDELETE;
	private long totalTimeGET;
	private long totalTimePOST;
	private long totalTimeDELETE;

	private AtomicLong debugCounter = new AtomicLong();

	@Override
	public void init(FilterConfig fc) throws ServletException {
		createMBeanServer();
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain fc) throws IOException, ServletException {
		long cc = debugCounter.incrementAndGet();
		HttpServletRequest httpReq = (HttpServletRequest) req;
		httpReq.setAttribute("cc", cc);
		long time = System.currentTimeMillis();
		fc.doFilter(req, resp);
		if (!"OPTIONS".equals(httpReq.getMethod())) {

			log.info("[{}] {} - {} - {} - {}", cc, httpReq.getRemoteHost(), httpReq.getMethod(), httpReq.getRequestURI(),
					System.currentTimeMillis() - time);

			switch (httpReq.getMethod()) {
			case "GET":
				numberOfGET++;
				totalTimeGET += (System.currentTimeMillis() - time);
				break;
			case "POST":
				numberOfPOST++;
				totalTimePOST += (System.currentTimeMillis() - time);
				break;
			case "DELETE":
				numberOfDELETE++;
				totalTimeDELETE += (System.currentTimeMillis() - time);
				break;
			}
		}
	}

	@Override
	public void destroy() {
		destroyMBeanServer();
	}

	private void createMBeanServer() {
		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			server.registerMBean(new StandardMBean(this, TimeStatMBean.class), new ObjectName(Driver.MBEAN_NAME_TIMESTAT));
		} catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
			log.error("Failed to init MBean", e);
		}
	}

	private void destroyMBeanServer() {
		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			ObjectName on = new ObjectName(Driver.MBEAN_NAME_TIMESTAT);
			if (server.isRegistered(on)) {
				server.unregisterMBean(on);
			}
		} catch (MBeanRegistrationException | InstanceNotFoundException | MalformedObjectNameException e) {
			log.error("Failed to unregister MBean", e);
		}
	}

	@Override
	public int getAverageTimePost() {
		return (int) (totalTimePOST / numberOfPOST);
	}

	@Override
	public int getAverageTimeGet() {
		return (int) (totalTimeGET / numberOfGET);
	}

	@Override
	public int getAverageTimeDelete() {
		return (int) (totalTimeDELETE / numberOfDELETE);
	}
}
