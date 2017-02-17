package de.unidue.stud.sehawagn.openhab.binding.jade.handler;

import static de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.RegistryChangeListener;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import de.unidue.stud.sehawagn.openhab.binding.jade.internal.ItemUpdateMonitorImpl;
import de.unidue.stud.sehawagn.openhab.binding.jade.internal.UpdateMonitorReceiver;
import hygrid.agent.smarthome.SmartHomeAgent;
import jade.core.Agent;

public class SmartHomeAgentHandler extends BaseThingHandler implements RegistryChangeListener<ItemChannelLink>, UpdateMonitorReceiver {

	private final Logger logger = LoggerFactory.getLogger(SmartHomeAgentHandler.class);
	private JADEBridgeHandler bridgeHandler;

	private String agentId;
	private ItemUpdateMonitorImpl itemUpdateMonitor;

	public SmartHomeAgentHandler(Thing thing, ItemUpdateMonitorImpl itemUpdateMonitor) {
		super(thing);
		this.itemUpdateMonitor = itemUpdateMonitor;

	}

	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_JADE_SMARTHOMEAGENT);
	private ChannelUID measurementChannelUID;
	private double readOutMeasurementValue = Double.NEGATIVE_INFINITY; // initialize

	@Override
	public void initialize() {
		logger.debug("Initializing SmartHomeAgentHandler.");
		Configuration config = getConfig();
		config.getProperties().get(PROPERTY_MEASUREMENT_CHANNEL_UID);

		measurementChannelUID = new ChannelUID((String) config.getProperties().get(PROPERTY_MEASUREMENT_CHANNEL_UID));

		linkRegistry.addRegistryChangeListener(this);

		tryChannelSupervisionByAgent(measurementChannelUID);
	}

	@Override
	public void dispose() {
		logger.debug("Disposing SmartHomeAgentHandler.");
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
				// this.bridgeHandler.registerJADEMessageListener(this);
			} else {
				return null;
			}
		}
		return this.bridgeHandler;
	}

	private void tryChannelSupervisionByAgent(ChannelUID currentMeasurementChannelUID) {
		System.out.println("tryChannelSupervisionByAgent(ChannelUID currentMeasurementChannelUID) : "
				+ currentMeasurementChannelUID);

		itemUpdateMonitor.mirrorChannel(currentMeasurementChannelUID, this);

		Set<Item> linkedMeasurementItems = this.linkRegistry.getLinkedItems(currentMeasurementChannelUID);

		if (linkedMeasurementItems.isEmpty()) {
			System.err.println("no linked items found for channel " + currentMeasurementChannelUID
					+ " :-(, which shouldn't happen");
		}

		for (Item linkedItem : linkedMeasurementItems) {
			System.out.println(linkedItem.getType() + linkedItem.getName() + linkedItem.getLabel()
					+ linkedItem.getCategory() + linkedItem.getState() + linkedItem.getStateDescription());

			DecimalType decimalItem = (DecimalType) linkedItem.getStateAs(DecimalType.class);

			System.out.println("linkedItem.getState()=" + linkedItem.getState());

			if (decimalItem != null) {
				System.out.println("decimalItem.doubleValue()=" + decimalItem.doubleValue());

				readOutMeasurementValue = decimalItem.doubleValue();

				getBridgeHandler().startNewAgent(SmartHomeAgent.class);
				updateStatus(ThingStatus.ONLINE);
			}
		}
	}

	@Override
	public void receiveMonitorOutput(State newState) {
		ChannelUID displayChannel = new ChannelUID(this.getThing().getUID(), CHANNEL_POWER);

		if (newState instanceof DecimalType) {
			System.err.println("HANNO: received newState=" + newState + ", want to output to displayChannel=" + displayChannel);

			DecimalType decimalItem = (DecimalType) newState;
			readOutMeasurementValue = decimalItem.doubleValue();

			handleCommand(displayChannel, RefreshType.REFRESH);
		}

	}

	private Agent getAgent() {
		if (getBridgeHandler() != null) {
			return getBridgeHandler().getAgentById(agentId);
		}
		return null;
	}

	@Override
	public void added(ItemChannelLink element) {
		if (element.getUID().equals(measurementChannelUID)) {
			System.out.println(
					"a new linked channel item with the UID " + measurementChannelUID + " has been added, start agent");

			tryChannelSupervisionByAgent(measurementChannelUID);
		}
	}

	@Override
	public void removed(ItemChannelLink element) {
		// TODO stop agent
	}

	@Override
	public void updated(ItemChannelLink oldElement, ItemChannelLink element) {
		// TODO same as added?
	}
}
