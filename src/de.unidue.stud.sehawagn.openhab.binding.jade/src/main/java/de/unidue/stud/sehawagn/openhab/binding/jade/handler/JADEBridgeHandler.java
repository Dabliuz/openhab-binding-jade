package de.unidue.stud.sehawagn.openhab.binding.jade.handler;

import static de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirror;
import hygrid.env.agentConfig.dataModel.AgentConfig;
import hygrid.env.agentConfig.dataModel.AgentOperatingMode;
import hygrid.env.agentConfig.dataModel.CentralAgentAID;
import jade.core.Agent;
import jade.core.Profile;
import jade.osgi.service.runtime.JadeRuntimeService;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class JADEBridgeHandler extends ConfigStatusBridgeHandler {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_JADE_CONTAINER);

    private Logger logger = LoggerFactory.getLogger(JADEBridgeHandler.class);

    private ContainerController container = null;

    @SuppressWarnings("unused")
    private ChannelMirror channelMirror; // for later use

    private HashMap<Integer, AgentController> myAgents = new HashMap<Integer, AgentController>();

    private BundleContext context;

    private JadeRuntimeService jrs;

    public JADEBridgeHandler(Bridge bridge, ChannelMirror channelMirror, JadeRuntimeService jrs) {
        super(bridge);
        this.channelMirror = channelMirror;
        this.jrs = jrs;
        context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        return Collections.emptyList(); // all good, otherwise add some messages
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // not needed...?
    }

    @Override
    public void initialize() {
        logger.debug("Initializing JADE bridge handler.");
        if (getConfig().containsKey(CONFKEY_LOCAL_MTP_ADDRESS) && getConfig().containsKey(CONFKEY_REMOTE_MTP_ADDRESS)) {
            if (container == null) {
                // Initialize jade container profile and start it

                Properties jadeProperties = new Properties();

                jadeProperties.put(Profile.MTPS, String.format(JADE_MTP_STRING_HTTP, getConfig().get(CONFKEY_LOCAL_MTP_ADDRESS)));

                startJadeContainer(jadeProperties);

                updateStatus(ThingStatus.ONLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Cannot start JADE bridge container. Some parameter not given.");
        }
    }

    @Override
    public void dispose() {
        logger.debug("JADE bridge Handler disposed.");

        if (container != null) {
            try {
                container.kill();
            } catch (StaleProxyException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                        "container dispose failed: " + e.getMessage());
                e.printStackTrace();
            }
            container = null;
        }
    }

    private void startJadeContainer(Properties props) {
        System.out.println("Hiermit starte ich einen neuen Container.");

        if (jrs != null) {
            try {
                jrs.startPlatform(props);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        System.out.println("Container sollte gestartet sein.");

/*
Profile profile = new ProfileImpl(props);
Runtime.instance().setCloseVM(false);
if (profile.getBooleanProperty(Profile.MAIN, true)) {


//            container = Runtime.instance().createMainContainer(profile);
} else {
//            container = Runtime.instance().createAgentContainer(profile);
}

// Runtime.instance().invokeOnTermination(new Terminator());
 */
    }

    public AgentController startAgent(String agentName, String agentClassName,
            SmartHomeAgentHandler smartHomeAgentHandler) throws StaleProxyException {

        AgentController ac = null;
        try {
            System.out.println("Hiermit kreiere ich einen neuen Agenten.");
            if (jrs != null) {
                ac = jrs.createNewAgent(agentName, agentClassName, new Object[] { getGeneralAgentConfig(agentName), smartHomeAgentHandler }, "de.unidue.stud.sehawagn.openhab.binding.jade");
                ac.start();
                System.out.println("Agent sollte gestartet worden sein.");
            } else {
                logger.error("JADE runtime service == null, agent can't be created");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ac;
    }

    public AgentController startAgent(String agentName, Class<? extends Agent> agentClass,
            SmartHomeAgentHandler smartHomeAgentHandler) throws StaleProxyException {
        if (container == null) {
            logger.error("Container not yet ready, please try again later");
            return null;
        }

        AgentController agent = myAgents.get(smartHomeAgentHandler.hashCode());

        if (agent == null) {
            agent = container.createNewAgent(agentName, agentClass.getName(),
                    new Object[] { getGeneralAgentConfig(agentName), smartHomeAgentHandler }); //
            myAgents.put(smartHomeAgentHandler.hashCode(), agent);
            agent.start();
        }

        return agent;
    }

    private AgentConfig getGeneralAgentConfig(String agentName) {

        CentralAgentAID centralAgentAID = new CentralAgentAID();
        centralAgentAID.setAgentName((String) getConfig().get(CONFKEY_REMOTE_GROUP_COORDINATOR_NAME));
        centralAgentAID.setPlatformName((String) getConfig().get(CONFKEY_REMOTE_PLATFORM_NAME));
        centralAgentAID.setPort(Integer.parseInt((String) getConfig().get(CONFKEY_REMOTE_MTP_PORT)));
        centralAgentAID.setUrlOrIp((String) getConfig().get(CONFKEY_REMOTE_MTP_ADDRESS));
        centralAgentAID.setHttp4Mtp((String) getConfig().get(CONFKEY_REMOTE_MTP_PROTOCOL));

        AgentConfig agentConfig = new AgentConfig();
        agentConfig.setAgentID(agentName);
        agentConfig.setCentralAgentAID(centralAgentAID);
        agentConfig.setAgentOperatingMode(AgentOperatingMode.TestBedReal); // RealSystem
        agentConfig.setKeyStore(null);
        agentConfig.setTrustStore(null);

        return agentConfig;
    }

    public void stopAgent(SmartHomeAgentHandler smartHomeAgentHandler) throws StaleProxyException {
        if (container == null) {
            logger.error("Container not yet ready, please try again later");
        }
        AgentController agent = myAgents.get(smartHomeAgentHandler.hashCode());

        if (agent != null) {
            agent.kill();
            myAgents.remove(smartHomeAgentHandler.hashCode());
        }
    }
}