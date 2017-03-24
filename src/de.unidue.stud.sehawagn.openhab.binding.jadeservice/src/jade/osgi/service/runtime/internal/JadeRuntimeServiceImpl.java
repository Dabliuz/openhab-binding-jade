package jade.osgi.service.runtime.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

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
    public AgentController createNewAgent(String name, String className, Object[] args) throws Exception {
        return container.createNewAgent(name, className, args);
    }

    @Override
    public AgentController createNewAgent(String name, String className, Object[] args, String bundleSymbolicName)
            throws Exception {
        if (logger.isLoggable(Logger.FINE)) {
            logger.log(Logger.FINE, "createAgent(name = " + name + " bundle = " + bundleSymbolicName + ") via JRS");
        }
        String classNameMod = className + "[" + AgentFactoryService.BUNDLE_NAME + "=" + bundleSymbolicName + "]";
        return container.createNewAgent(name, classNameMod, args);
    }

    @Override
    public AgentController createNewAgent(String name, String className, Object[] args, String bundleSymbolicName,
            String bundleVersion) throws Exception {
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
    public String getContainerName() throws Exception {
        return container.getContainerName();
    }

    @Override
    public String getPlatformName() {
        return container.getPlatformName();
    }

    @Override
    public void kill() throws Exception {
        container.kill();
    }

    @Override
    public void startPlatform(Properties jadeProperties) throws Exception {
        // Initialize jade container profile and start it
        if (jadeProperties == null) {
            jadeProperties = new Properties();
        }
        addJadeSystemProperties(jadeProperties);
        addJadeFileProperties(jadeProperties);
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

    private void addJadeSystemProperties(Properties props) {
        Set<Entry<Object, Object>> entrySet = System.getProperties().entrySet();
        for (Entry<Object, Object> entry : entrySet) {
            String key = (String) entry.getKey();
            if (key.startsWith(PROFILE_PARAMETER_PREFIX)) {
                props.setProperty(key.substring(PROFILE_PARAMETER_PREFIX.length()), (String) entry.getValue());
            }
        }
    }

    private void addJadeFileProperties(Properties props) throws Exception {
        String profileConf = System.getProperty(JADE_CONF);
        if (profileConf != null) {
            // find profile configuration in classpath
            InputStream input = ClassLoader.getSystemResourceAsStream(profileConf);
            if (input == null) {
                File f = new File(profileConf);
                if (f.exists()) {
                    input = new FileInputStream(f);
                }
            }
            if (input != null) {
                Properties pp = new Properties();
                pp.load(input);
                Iterator<Object> it = pp.keySet().iterator();
                while (it.hasNext()) {
                    String key = (String) it.next();
                    if (!props.containsKey(key)) {
                        props.setProperty(key, pp.getProperty(key));
                    }
                }

            }
        }

    }

    private void startJadeContainer(Properties props) {
        Profile profile = new ProfileImpl(props);
        Runtime.instance().setCloseVM(false);
        if (profile.getBooleanProperty(Profile.MAIN, true)) {
            container = Runtime.instance().createMainContainer(profile);
        } else {
            container = Runtime.instance().createAgentContainer(profile);
        }
        Runtime.instance().invokeOnTermination(new Terminator());
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
                } catch (Exception e) {
                    logger.log(Logger.SEVERE, "Error stopping bundle", e);
                }
            }
        }
    }

}
