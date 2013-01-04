package net.dahanne.osgi.ehcache.consumer;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public void start(BundleContext context) throws Exception {
		System.out.println(String.format("Start - %s", this.getClass().getName()));

		
		Demo demo = new Demo();
		try {
			demo.go();
		} catch (Exception e) {
      e.printStackTrace();
		}
	}

	public void stop(BundleContext context) throws Exception {
		System.out.println(String.format("Stop - %s", this.getClass().getName()));
	}
}
