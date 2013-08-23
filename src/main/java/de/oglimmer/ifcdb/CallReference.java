package de.oglimmer.ifcdb;

/**
 * ThreadLocal storage used to hold a "http-call-reference"
 * 
 * @author Oli Zimpasser
 * 
 */
public class CallReference {

	private static ThreadLocal<String> id = new ThreadLocal<>();

	public static void setId(String db) {
		id.set(db);
	}

	public static String getId() {
		String val = id.get();
		return null == val ? Thread.currentThread().getName() : val;
	}
}
