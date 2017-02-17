package de.unidue.stud.sehawagn.openhab.binding.jade.handler;

import static de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import de.unidue.stud.sehawagn.openhab.binding.jade.internal.ItemUpdateMonitorImpl;
import de.unidue.stud.sehawagn.openhab.binding.jade.internal.UpdateMonitorReceiver;
import de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent.SmartHomeAgent;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class SmartHomeAgentHandler extends BaseThingHandler implements UpdateMonitorReceiver {

	private final Logger logger = LoggerFactory.getLogger(SmartHomeAgentHandler.class);
	private JADEBridgeHandler bridgeHandler;

	private ItemUpdateMonitorImpl itemUpdateMonitor;

	public SmartHomeAgentHandler(Thing thing, ItemUpdateMonitorImpl itemUpdateMonitor) {
		super(thing);
		this.itemUpdateMonitor = itemUpdateMonitor;

	}

	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_JADE_SMARTHOMEAGENT);
	private ChannelUID measurementChannelUID;
	private double readOutMeasurementValue = Double.NEGATIVE_INFINITY; // initialize
	private AgentController agent;

	@Override
	public void initialize() {
		logger.debug("Initializing SmartHomeAgentHandler.");
		Configuration config = getConfig();
		config.getProperties().get(PROPERTY_MEASUREMENT_CHANNEL_UID);

		measurementChannelUID = new ChannelUID((String) config.getProperties().get(PROPERTY_MEASUREMENT_CHANNEL_UID));

		mirrorChannelByAgent();
	}

	@Override
	public void dispose() {
		logger.debug("Disposing SmartHomeAgentHandler.");
		try {
			getBridgeHandler().stopAgent(this);
		} catch (StaleProxyException e) {
			updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, "agent dispose failed: " + e.getCause());
			e.printStackTrace();
		}
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		if (command == RefreshType.REFRESH) {
			State newState = null;
			if (readOutMeasurementValue != Double.NEGATIVE_INFINITY) {
				switch (channelUID.getId()) {
				case CHANNEL_POWER: {
					newState = new DecimalType(readOutMeasurementValue);
					break;
				}
				}
				updateState(channelUID.getId(), newState);
			}
		}
	}

	private synchronized JADEBridgeHandler getBridgeHandler() {
		if (this.bridgeHandler == null) {
			Bridge bridge = getBridge();
			if (bridge == null) {
				return null;
			}
			ThingHandler handler = bridge.getHandler();
			if (handler instanceof JADEBridgeHandler) {
				this.bridgeHandler = (JADEBridgeHandler) handler;
			} else {
				return null;
			}
		}
		return this.bridgeHandler;
	}

	private void mirrorChannelByAgent() {
//		System.out.println("tryChannelSupervisionByAgent(ChannelUID currentMeasurementChannelUID) : " + currentMeasurementChannelUID);

		itemUpdateMonitor.mirrorChannel(measurementChannelUID, this);

		String errorCause = null;

		try {
			agent = getBridgeHandler().startNewAgent(SmartHomeAgent.class, this);
		} catch (StaleProxyException e) {
			errorCause = e.getMessage();
		}
		if (agent != null) {
			updateStatus(ThingStatus.ONLINE);
		} else {
			updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR, "agent start failed: " + errorCause);

		}
	}

	@Override
	public void receiveMonitorOutput(State newState) {
		ChannelUID displayChannel = new ChannelUID(this.getThing().getUID(), CHANNEL_POWER);

		if (newState instanceof DecimalType) {
//			System.err.println("HANNO: received newState=" + newState + ", want to output to displayChannel=" + displayChannel);

			DecimalType decimalItem = (DecimalType) newState;
			readOutMeasurementValue = decimalItem.doubleValue();

			handleCommand(displayChannel, RefreshType.REFRESH);
			mirrorChannelByAgent();
		}

	}

	public double getCurrentMeasurement() {
		return readOutMeasurementValue;
	}
}
