package de.unidue.stud.sehawagn.openhab.binding.jade.handler;

import static de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirror;
import hygrid.env.agentConfig.dataModel.AgentConfig;
import hygrid.env.agentConfig.dataModel.AgentOperatingMode;
import hygrid.env.agentConfig.dataModel.CentralAgentAID;
import jade.core.Profile;
import jade.osgi.service.runtime.JadeRuntimeService;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class JADEBridgeHandler extends ConfigStatusBridgeHandler {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_JADE_CONTAINER);

    private final static String BUNDLE_SYMBOLIC_NAME = "de.unidue.stud.sehawagn.openhab.binding.jade";

    private Logger logger = LoggerFactory.getLogger(JADEBridgeHandler.class);

    @SuppressWarnings("unused")
    private ChannelMirror channelMirror; // for later use

//    private HashMap<Integer, AgentController> myAgents = new HashMap<Integer, AgentController>();

    private JadeRuntimeService jrs;

    public JADEBridgeHandler(Bridge bridge, ChannelMirror channelMirror, JadeRuntimeService jrs) {
        super(bridge);
        this.channelMirror = channelMirror;
        this.jrs = jrs;
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
        logger.info("Initializing JADE bridge handler.");
        if (getConfig().containsKey(CONFKEY_LOCAL_MTP_ADDRESS) && getConfig().containsKey(CONFKEY_REMOTE_MTP_ADDRESS)) {
            // Initialize jade container profile and start it

            Properties jadeProperties = new Properties();

            jadeProperties.put(Profile.MTPS, String.format(JADE_MTP_STRING_HTTP, getConfig().get(CONFKEY_LOCAL_MTP_ADDRESS)));

            startJadeContainer(jadeProperties);
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Cannot start JADE bridge container. Some parameter not given.");
        }
    }

    @Override
    public void dispose() {
        logger.info("JADE bridge handler disposed, kill JADE container/runtime.");

        try {
            if (jrs != null) {
                jrs.kill();
                logger.info("jrs.kill()");
            } else {
                logger.info("jrs should be killed, but not available");
            }
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                    "container dispose failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startJadeContainer(Properties props) {
        logger.info("Starting new JADE container.");
        if (jrs != null) {
            jrs.startPlatform(props);
            try {
                AgentController keepOpenAgent = jrs.createNewAgent("KeepOpen", de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent.KeepOpenAgent.class.getName(), null, BUNDLE_SYMBOLIC_NAME); // jade.tools.rma.rma
                keepOpenAgent.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        logger.info("JADE container should be started.");
    }

    public AgentController startAgent(String agentName, String agentClassName,
            SmartHomeAgentESHHandler smartHomeAgentHandler) throws Exception {
        AgentController agent = smartHomeAgentHandler.getAgent();

        if (agent == null) {
            logger.info("Hiermit kreiere ich einen neuen Agenten.");
            if (jrs != null) {
                agent = jrs.createNewAgent(agentName, agentClassName, new Object[] { getGeneralAgentConfig(agentName), smartHomeAgentHandler }, BUNDLE_SYMBOLIC_NAME);
                logger.info("Agent state:" + agent.getState());
                agent.start();
                logger.info("Agent sollte gestartet worden sein.");
            } else {
                logger.error("JADE runtime service == null, agent can't be created, please try again later");
            }
        } else {
            logger.info("Agent nicht neu gestartet, sondern war schon da: " + agent.getName() + " - " + agent.getState());
        }
        return agent;
    }

    private AgentConfig getGeneralAgentConfig(String agentName) {

        CentralAgentAID centralAgentAID = new CentralAgentAID();
        centralAgentAID.setAgentName((String) getConfig().get(CONFKEY_REMOTE_GROUP_COORDINATOR_NAME));
        centralAgentAID.setPlatformName((String) getConfig().get(CONFKEY_REMOTE_PLATFORM_NAME));
        centralAgentAID.setUrlOrIp((String) getConfig().get(CONFKEY_REMOTE_MTP_ADDRESS));
        centralAgentAID.setPort(1099);
        centralAgentAID.setMtpPort(Integer.parseInt((String) getConfig().get(CONFKEY_REMOTE_MTP_PORT)));
        centralAgentAID.setHttp4Mtp((String) getConfig().get(CONFKEY_REMOTE_MTP_PROTOCOL));

        AgentConfig agentConfig = new AgentConfig();
        agentConfig.setAgentID(agentName);
        agentConfig.setCentralAgentAID(centralAgentAID);
        agentConfig.setAgentOperatingMode(AgentOperatingMode.TestBedReal); // RealSystem
        agentConfig.setKeyStore(null);
        agentConfig.setTrustStore(null);

        return agentConfig;
    }

    public boolean stopAgent(SmartHomeAgentESHHandler smartHomeAgentHandler) throws StaleProxyException {
        logger.info("stopping agent");

        if (jrs == null) {
            logger.error("Container not yet ready, please try again later");
        }
        AgentController agent = smartHomeAgentHandler.getAgent();
        if (agent != null) {
            logger.info("killing agent");
            agent.kill();
            logger.info("agent killed and removed");
            return true;
        }
        return false;
    }
}