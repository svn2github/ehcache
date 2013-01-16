package net.dahanne.osgi.ehcache.consumer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

	private static final Logger LOG = LoggerFactory.getLogger(Activator.class.getName());

	public void start(BundleContext context) throws Exception {
		System.out.println(String.format("Start - %s", this.getClass().getName()));
		LOG.info("hello !!!");

		//Thread.currentThread().setContextClassLoader(Activator.class.getClassLoader());
		// ClassLoader contextClassLoader =
		// Thread.currentThread().getContextClassLoader();

		Demo demo = new Demo();
		try {
			demo.txEhcache();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop(BundleContext context) throws Exception {
		System.out.println(String.format("Stop - %s", this.getClass().getName()));
	}
}
