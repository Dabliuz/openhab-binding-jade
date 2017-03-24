package jade.osgi.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import jade.osgi.service.runtime.JadeRuntimeService;
import jade.osgi.service.runtime.internal.JadeRuntimeServiceFactory;
import jade.osgi.service.runtime.internal.OsgiEventHandler;
import jade.osgi.service.runtime.internal.OsgiEventHandlerFactory;
import jade.util.Logger;
import jade.util.ObjectManager;
import jade.wrapper.ContainerController;

public class JadeActivator implements BundleActivator, BundleListener {

    private static final String RESTART_AGENTS_ON_UPDATE_KEY = "restart-agents-on-update";
    private static final String RESTART_AGENTS_TIMEOUT_KEY = "restart-agents-timeout";
    public static final String SPLIT_CONTAINER_KEY = "split-container";

    private static final boolean RESTART_AGENTS_ON_UPDATE_DEFAULT = true;
    private static final long RESTART_AGENTS_TIMEOUT_DEFAULT = 10000;

    private BundleContext context;
    private static JadeActivator instance;
    private ContainerController container;
    private AgentManager agentManager;
    private OsgiEventHandlerFactory handlerFactory;

    private static Logger logger = Logger.getMyLogger(JadeActivator.class.getName());

    private Object terminationLock = new Object();
    private ServiceRegistration<?> jrs;
    private OSGIAgentLoader agentLoader;

    @Override
    public void start(BundleContext context) throws Exception {
        try {
            instance = this;
            this.context = context;
            this.agentManager = new AgentManager(context);

            // Create OsgiEventHandlerFactory
            boolean restartAgents = RESTART_AGENTS_ON_UPDATE_DEFAULT;
            String restartAgentsOnUpdateS = System.getProperty(RESTART_AGENTS_ON_UPDATE_KEY);
            if (restartAgentsOnUpdateS != null) {
                restartAgents = Boolean.parseBoolean(restartAgentsOnUpdateS);
            }
            long restartTimeout = RESTART_AGENTS_TIMEOUT_DEFAULT;
            String restartAgentsTimeoutS = System.getProperty(RESTART_AGENTS_TIMEOUT_KEY);
            if (restartAgentsTimeoutS != null) {
                try {
                    restartTimeout = Long.parseLong(restartAgentsTimeoutS);
                } catch (NumberFormatException e) {
                }
            }
            logger.log(Logger.CONFIG, RESTART_AGENTS_ON_UPDATE_KEY + " " + restartAgents);
            logger.log(Logger.CONFIG, RESTART_AGENTS_TIMEOUT_KEY + " " + restartTimeout);
            this.handlerFactory = new OsgiEventHandlerFactory(agentManager, restartAgents, restartTimeout);

            // Register an osgi agent loader (do that before starting JADE so that bootstrap agents can be loaded from
            // separated bundles too)
            agentLoader = new OSGIAgentLoader(context, agentManager);
            ObjectManager.addLoader(ObjectManager.AGENT_TYPE, agentLoader);

            // Register JRS service
            registerJadeRuntimeService();

            // Listen to bundle events
            context.addBundleListener(this);

        } catch (Exception e) {
            logger.log(Logger.SEVERE, "Error during bundle startup", e);
            throw e;
        }
        System.out.println("jadeservice: The JadeActivator has been started!");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        synchronized (terminationLock) {
//            if (isSplitContainer()) {
//                MicroRuntime.stopJADE();
//            } else {
            try {
                // If JADE has already terminated we get an exception and simply ignore it
                container.kill();
            } catch (Exception e) {
            }
//            }
        }
        handlerFactory.stop();
        if (jrs != null) {
            jrs.unregister();
        }
        context.removeBundleListener(this);
        if (agentLoader != null && !ObjectManager.removeLoader(ObjectManager.AGENT_TYPE, agentLoader)) {
            logger.log(Logger.SEVERE, "Error removing osgi agent loader");
        }
        logger.log(Logger.INFO, context.getBundle().getSymbolicName() + " stopped!");
    }

    public static JadeActivator getInstance() {
        return instance;
    }

    public AgentManager getAgentManager() {
        return agentManager;
    }

    public BundleContext getBundleContext() {
        return context;
    }

    @Override
    public synchronized void bundleChanged(BundleEvent event) {
        Bundle b = event.getBundle();
        OsgiEventHandler handler = handlerFactory.getOsgiEventHandler(b.getSymbolicName(),
                b.getHeaders().get(Constants.BUNDLE_VERSION));
        handler.handleEvent(event);
    }

    private void registerJadeRuntimeService() {
        ServiceFactory<?> factory;
        if (isSplitContainer()) {
            factory = new JadeRuntimeServiceFactory(agentManager);
        } else {
            factory = new JadeRuntimeServiceFactory(container, agentManager);
        }
        jrs = context.registerService(JadeRuntimeService.class.getName(), factory, null);
    }

    private boolean isSplitContainer() {
        return "true".equalsIgnoreCase(System.getProperty(SPLIT_CONTAINER_KEY));
    }

}
