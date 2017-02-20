package de.unidue.stud.sehawagn.openhab.binding.jade.handler;

import static de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.items.Item;
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

import de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent.SmartHomeAgent;
import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirror;
import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirrorReceiver;
import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class SmartHomeAgentHandler extends BaseThingHandler implements ChannelMirrorReceiver {
	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_JADE_SMARTHOMEAGENT);

	private static final Class<? extends Agent> AGENT_CLASS = SmartHomeAgent.class;
	private static final String DISPLAY_CHANNEL = CHANNEL_POWER;  // the channel where the value from the mirrored channel is displayed in OpenHAB

	private final Logger logger = LoggerFactory.getLogger(SmartHomeAgentHandler.class);

	private JADEBridgeHandler bridgeHandler;
	private ChannelMirror channelMirror;

	private ChannelUID measurementChannelUID;
	private ChannelUID displayChannelUID;

	private double measurementChannelValue = Double.NEGATIVE_INFINITY; // initialize
	private AgentController myAgent;

	public SmartHomeAgentHandler(Thing thing, ChannelMirror channelMirror) {
		super(thing);
		this.channelMirror = channelMirror;
	}

	@Override
	public void initialize() {
		logger.debug("Initializing SmartHomeAgentHandler.");
		Configuration config = getConfig();

		measurementChannelUID = new ChannelUID((String) config.getProperties().get(PROPERTY_MEASUREMENT_CHANNEL_UID));
		displayChannelUID = new ChannelUID(this.getThing().getUID(), DISPLAY_CHANNEL);

		State currentState = null;
		Set<Item> allLinks = this.linkRegistry.getLinkedItems(measurementChannelUID);
		for (Item item : allLinks) {
			currentState = item.getState(); // only use the state of the last, usually there is only one
		}
		receiveFromMirroredChannel(currentState); // init channel with current value before waiting for the next change

		channelMirror.mirrorChannel(measurementChannelUID, this);
		String errorCause = null;

		try {
			myAgent = getBridgeHandler().startAgent((String) config.getProperties().get(PROPERTY_AGENT_ID), AGENT_CLASS, this);
		} catch (StaleProxyException e) {
			errorCause = e.getMessage();
		}
		if (myAgent != null) {
			updateStatus(ThingStatus.ONLINE);
		} else {
			updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
					"agent start failed: " + errorCause);

		}
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
			if (measurementChannelValue != Double.NEGATIVE_INFINITY) {
				switch (channelUID.getId()) {
				case DISPLAY_CHANNEL: {
					newState = new DecimalType(measurementChannelValue);
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

	@Override
	public void receiveFromMirroredChannel(State newState) {
		if (newState instanceof DecimalType) {
			DecimalType decimalItem = (DecimalType) newState;
			measurementChannelValue = decimalItem.doubleValue();
			handleCommand(displayChannelUID, RefreshType.REFRESH);
		}
	}

	public double getCurrentMeasurement() {
		return measurementChannelValue;
	}
}