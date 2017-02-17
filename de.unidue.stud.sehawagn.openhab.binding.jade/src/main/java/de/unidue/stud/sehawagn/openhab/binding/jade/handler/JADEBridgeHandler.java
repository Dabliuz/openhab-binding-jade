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

import de.unidue.stud.sehawagn.openhab.binding.jade.internal.ItemUpdateMonitorImpl;
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

	private ItemUpdateMonitorImpl itemUpdateMonitor;

	private static final String DEFAULT_MTPADDRESS = "132.252.61.116";
	private static final int DEFAULT_MTPPORT = 7778;
	private static final String DEFAULT_MTPPROTOCOL = "HTTP";
	private static final String DEFAULT_PLATFORMNAME = "132.252.61.116:1099/JADE";
	private static final String DEFAULT_CENTRALAGENTNAME = "CeExAg";

	private static final String DEFAULT_AGENTID = "n49";
	private HashMap<Integer, AgentController> myAgents = new HashMap<Integer, AgentController>();

	public JADEBridgeHandler(Bridge bridge, ItemUpdateMonitorImpl itemUpdateMonitor) {
		super(bridge);
		this.itemUpdateMonitor = itemUpdateMonitor;
	}

	public JADEBridgeHandler(Bridge bridge) {
		super(bridge);
	}

	@Override
	public Collection<ConfigStatusMessage> getConfigStatus() {
		return Collections.emptyList(); // all good, otherwise add some messages
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		// judging from the hue bridge, this seems to be not needed...?
	}

	@Override
	public void initialize() {
		logger.debug("Initializing JADE bridge handler.");
		if (getConfig().get(CONFKEY_MTP_ADDRESS) != null) {
			if (container == null) {
				// Initialize jade container profile and start it
				Properties jadeProperties = new Properties();
				startJadeContainer(jadeProperties);

				// String someParameter = (String) getConfig().get(CONFKEY_SOME_PARAMETER);

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
				// TODO Auto-generated catch block
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

		System.out.println("JADE container started.");
		// Runtime.instance().invokeOnTermination(new Terminator());
	}

	public AgentController startNewAgent(Class<? extends Agent> clazz, SmartHomeAgentHandler smartHomeAgentHandler) throws StaleProxyException {
		if (container == null) {
			System.out.println("Container not yet ready, please try again later");
			return null;
		}
		AgentController agent = myAgents.get(smartHomeAgentHandler.hashCode());

		if (agent == null) {
			agent = container.createNewAgent(clazz.getSimpleName() + "-" + smartHomeAgentHandler.getThing().getUID().getId(), clazz.getName(), new Object[] { getGeneralAgentConfig(), smartHomeAgentHandler });
			myAgents.put(smartHomeAgentHandler.hashCode(), agent);
			agent.start();
		}

		return agent;

	}

	private AgentConfig getGeneralAgentConfig() {
		CentralAgentAID centralAgentAID = new CentralAgentAID();
		centralAgentAID.setAgentName(DEFAULT_CENTRALAGENTNAME);
		centralAgentAID.setPlatformName(DEFAULT_PLATFORMNAME);
		centralAgentAID.setPort(DEFAULT_MTPPORT);
		centralAgentAID.setUrlOrIp(DEFAULT_MTPADDRESS);
		centralAgentAID.setHttp4Mtp(DEFAULT_MTPPROTOCOL);

		AgentConfig agentConfig = new AgentConfig();
		agentConfig.setAgentID(DEFAULT_AGENTID);
		agentConfig.setCentralAgentAID(centralAgentAID);
		agentConfig.setAgentOperatingMode(AgentOperatingMode.RealSystem);
		agentConfig.setKeyStore(null);
		agentConfig.setTrustStore(null);

		return agentConfig;
	}

	public void stopAgent(SmartHomeAgentHandler smartHomeAgentHandler) throws StaleProxyException {
		if (container == null) {
			System.out.println("Container not yet ready, please try again later");
		}
		AgentController agent = myAgents.get(smartHomeAgentHandler.hashCode());

		if (agent != null) {
			agent.kill();
			myAgents.remove(smartHomeAgentHandler.hashCode());
		}

	}

}