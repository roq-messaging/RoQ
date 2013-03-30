
package org.roq.messaging.bundle.gcm;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.roqmessaging.management.GlobalConfigurationManager;

public class GCMActivator implements BundleActivator {
	//handle on the GCM
	private GlobalConfigurationManager configurationManager = null;
	//the logger
	private Logger log = Logger.getLogger(GlobalConfigurationManager.class);

    /**
     * Start the bundle that will start the GCM process.
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
	public void start(BundleContext context) {
		log.info("Starting the GCM Bundle...");
		startingGCM(context);
	}

	/**
	 * Start the GCM process exactly as the GCM loader.
     * @param context the bundle context of the GCM bundle
	 */
	private void startingGCM(BundleContext context) {
		log.info("Starting the  global configuration manager");
		String gcmprops = "etc/GCM.properties";
		log.info("Starting with property location @"+ gcmprops);
		configurationManager=  new GlobalConfigurationManager(gcmprops);
		Thread configThread = new Thread(configurationManager);
		configThread.start();
	}

	/**
	 * Stops the GCM bundle as the GCM launcher shutdown hook does.
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) {
		log.info("Stopping the Global configuration management Bunle.");
        configurationManager.getShutDownMonitor().shutDown();
    }

}