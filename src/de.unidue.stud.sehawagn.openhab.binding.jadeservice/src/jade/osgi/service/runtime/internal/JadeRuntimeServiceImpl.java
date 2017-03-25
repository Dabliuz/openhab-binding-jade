package jade.osgi.service.runtime.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.osgi.internal.AgentManager;
import jade.osgi.internal.OSGIBridgeService;
import jade.osgi.service.agentFactory.AgentFactoryService;
import jade.osgi.service.runtime.JadeRuntimeService;
import jade.util.Logger;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

public class JadeRuntimeServiceImpl implements JadeRuntimeService {

    private static final String PROFILE_PARAMETER_PREFIX = "jade.";
    private static final String JADE_CONF = PROFILE_PARAMETER_PREFIX + "conf";

    private ContainerController container;
    private AgentManager agentManager;
    private Bundle bundle;

    private Object terminationLock = new Object();

    private static Logger logger = Logger.getMyLogger(JadeRuntimeServiceImpl.class.getName());

    /*
     * called automagically by OSGi
     */
    public JadeRuntimeServiceImpl() {
    }

    @Override
    public AgentController createNewAgent(String name, String className, Object[] args) throws StaleProxyException {
        return container.createNewAgent(name, className, args);
    }

    @Override
    public AgentController createNewAgent(String name, String className, Object[] args, String bundleSymbolicName) throws StaleProxyException {
        if (logger.isLoggable(Logger.FINE)) {
            logger.log(Logger.FINE, "createAgent(name = " + name + " bundle = " + bundleSymbolicName + ") via JRS");
        }
        String classNameMod = className + "[" + AgentFactoryService.BUNDLE_NAME + "=" + bundleSymbolicName + "]";
        return container.createNewAgent(name, classNameMod, args);
    }

    @Override
    public AgentController createNewAgent(String name, String className, Object[] args, String bundleSymbolicName,
            String bundleVersion) throws StaleProxyException {
        if (logger.isLoggable(Logger.FINE)) {
            logger.log(Logger.FINE, "createAgent(name = " + name + " bundle = " + bundleSymbolicName + ") via JRS");
        }
        String classNameMod = className + "[" + AgentFactoryService.BUNDLE_NAME + "=" + bundleSymbolicName + ";"
                + AgentFactoryService.BUNDLE_VERSION + "=" + bundleVersion + "]";
        return container.createNewAgent(name, classNameMod, args);
    }

    @Override
    public AgentController getAgent(String localAgentName) throws Exception {
        if (logger.isLoggable(Logger.FINE)) {
            logger.log(Logger.FINE, "Agent Controller requested by " + bundle.getSymbolicName());
        }
        return container.getAgent(localAgentName);
    }

    @Override
    public AgentController acceptNewAgent(String name, Agent agent) throws Exception {
        if (logger.isLoggable(Logger.FINE)) {
            logger.log(Logger.FINE, "acceptAgent(name = " + name + " bundle = " + bundle.getSymbolicName() + ")");
        }
        AgentController myAgent = container.acceptNewAgent(name, agent);
        agentManager.addAgent(bundle, agent, false);
        return myAgent;
    }

    @Override
    public String getContainerName() throws ControllerException {
        return container.getContainerName();
    }

    @Override
    public String getPlatformName() {
        return container.getPlatformName();
    }

    @Override
    public void kill() throws StaleProxyException {
        if (isRunning()) {
            container.kill();
            container = null;
        }
    }

    @Override
    public void startPlatform(Properties jadeProperties) {
        // Initialize jade container profile and start it
        if (jadeProperties == null) {
            jadeProperties = new Properties();
        }
        addOSGIBridgeService(jadeProperties);

        startJadeContainer(jadeProperties);

    }

    private void addOSGIBridgeService(Properties pp) {
        String services = pp.getProperty(Profile.SERVICES);
        String defaultServices = ";" + jade.core.mobility.AgentMobilityService.class.getName() + ";"
                + jade.core.event.NotificationService.class.getName();
        String serviceName = OSGIBridgeService.class.getName();
        if (services == null) {
            pp.setProperty(Profile.SERVICES, serviceName + defaultServices);
        } else if (services.indexOf(serviceName) == -1) {
            pp.setProperty(Profile.SERVICES, services + ";" + serviceName);
        }
    }

    private void startJadeContainer(Properties props) {
        Profile profile = new ProfileImpl(props);
        Runtime.instance().setCloseVM(false);
        if (!isRunning()) {
            if (profile.getBooleanProperty(Profile.MAIN, true)) {
                container = Runtime.instance().createMainContainer(profile);
            } else {
                container = Runtime.instance().createAgentContainer(profile);
            }
//            Runtime.instance().invokeOnTermination(new Terminator());
        }
    }

    public boolean isRunning() {
        if (container != null) {
            try {
                getContainerName();
                return true;
            } catch (ControllerException e) {
                return false;
            }
        }
        return false;
    }

    private class Terminator implements Runnable {
        @Override
        public void run() {
            synchronized (terminationLock) {
                Runtime.instance().resetTerminators();
                System.out.println("JADE termination invoked!");
                try {

                    Bundle myBundle = FrameworkUtil.getBundle(this.getClass());
                    if (myBundle.getState() == Bundle.ACTIVE) {
                        myBundle.stop(Bundle.STOP_TRANSIENT);
                    }
                } catch (IllegalStateException ise) {
                    // This exception is thrown when jadeOsgi bundle is invalid. This case happens
                    // when user stop the bundle from the osgi ui. Depends on the execution time of the
                    // thread listening jade termination, jadeOsgi bundle can be already stopped.
                    logger.log(Logger.WARNING, "jadeOsgi bundle is invalid");
                } catch (Exception e) {
                    logger.log(Logger.SEVERE, "Error stopping bundle", e);
                }
            }
        }
    }

}
