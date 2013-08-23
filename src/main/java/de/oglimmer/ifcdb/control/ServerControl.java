package de.oglimmer.ifcdb.control;

import java.io.IOException;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import de.oglimmer.ifcdb.Driver;
import de.oglimmer.ifcdb.stats.ControlMBean;

public class ServerControl {

	public static void main(String[] args) {
		try {
			String port = args[0];
			System.out.println("Shutting down ifcdb at jxm-port:" + port);
			JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:" + port + "/jmxrmi");
			JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
			MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

			ObjectName mbeanName = new ObjectName(Driver.MBEAN_NAME_CONTROL);
			ControlMBean mbeanProxy = JMX.newMBeanProxy(mbsc, mbeanName, ControlMBean.class, true);

			mbeanProxy.shutdown();
			System.exit(0);
		} catch (MalformedObjectNameException | IOException e) {
			System.exit(1);
		}
	}

}
