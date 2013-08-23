package de.oglimmer.ifcdb;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple ID generator. Format is: Current time - Random - Counter
 * 
 * @author Oli Zimpasser
 * 
 */
public class IDGenerator {

	private Logger log = LoggerFactory.getLogger(IDGenerator.class);

	private AtomicLong counter = new AtomicLong();

	public String getId() {
		long c = counter.incrementAndGet();
		DecimalFormat df = new DecimalFormat("###############");
		String id = Long.toString(System.currentTimeMillis()) + "-" + df.format(Math.random() * 1_000_000_000_000_000L) + "-" + c;
		log.debug("[{}] Generated new id {}", CallReference.getId(), id);
		return id;
	}

}
