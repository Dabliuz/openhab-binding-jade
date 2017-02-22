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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirror;
import hygrid.env.agentConfig.dataModel.AgentConfig;
import hygrid.env.agentConfig.dataModel.AgentOperatingMode;
import hygrid.env.agentConfig.dataModel.CentralAgentAID;
import jade.core.Agent;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
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

    public JADEBridgeHandler(Bridge bridge, ChannelMirror channelMirror) {
        super(bridge);
        this.channelMirror = channelMirror;
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
        if (getConfig().get(CONFKEY_MTP_ADDRESS) != null) {
            if (container == null) {
                // Initialize jade container profile and start it

                // String someParameter = (String) getConfig().get(CONFKEY_SOME_PARAMETER);

                Properties jadeProperties = new Properties();

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
                        "container dispose failed: " + e.getCause());
                e.printStackTrace();
            }
            container = null;
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

        // Runtime.instance().invokeOnTermination(new Terminator());
    }

    public AgentController startAgent(String agentName, Class<? extends Agent> agentClass,
            SmartHomeAgentHandler smartHomeAgentHandler) throws StaleProxyException {
        if (container == null) {
            System.err.println("Container not yet ready, please try again later");
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
        centralAgentAID.setAgentName((String) getConfig().get(CONFKEY_CENTRAL_AGENT_NAME));
        centralAgentAID.setPlatformName((String) getConfig().get(CONFKEY_PLATFORM_NAME));
        centralAgentAID.setPort(Integer.parseInt((String) getConfig().get(CONFKEY_MTP_PORT)));
        centralAgentAID.setUrlOrIp((String) getConfig().get(CONFKEY_MTP_ADDRESS));
        centralAgentAID.setHttp4Mtp((String) getConfig().get(CONFKEY_MTP_PROTOCOL));

        AgentConfig agentConfig = new AgentConfig();
        agentConfig.setAgentID(agentName);
        agentConfig.setCentralAgentAID(centralAgentAID);
        agentConfig.setAgentOperatingMode(AgentOperatingMode.RealSystem);
        agentConfig.setKeyStore(null);
        agentConfig.setTrustStore(null);

        return agentConfig;
    }

    public void stopAgent(SmartHomeAgentHandler smartHomeAgentHandler) throws StaleProxyException {
        if (container == null) {
            System.err.println("Container not yet ready, please try again later");
        }
        AgentController agent = myAgents.get(smartHomeAgentHandler.hashCode());

        if (agent != null) {
            agent.kill();
            myAgents.remove(smartHomeAgentHandler.hashCode());
        }
    }
}