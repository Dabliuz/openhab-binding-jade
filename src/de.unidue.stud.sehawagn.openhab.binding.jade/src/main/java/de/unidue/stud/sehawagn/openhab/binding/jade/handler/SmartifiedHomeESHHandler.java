package de.unidue.stud.sehawagn.openhab.binding.jade.handler;

import static de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.link.ItemChannelLink;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.StateDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import de.unidue.stud.sehawagn.openhab.binding.jade.internal.JADEHandlerFactory;
import de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent.SmartifiedHomeAgent;
import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirror;
import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirrorReceiver;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class SmartifiedHomeESHHandler extends BaseThingHandler implements ChannelMirrorReceiver {
	public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_JADE_SMARTHOMEAGENT);

	// the channel where the value from the mirrored channel is displayed in OpenHAB
	private static final String MEASUREMENT_MIRROR_CHANNEL = CHANNEL_DEVICE_POWER_CONSUMPTION;

	// the channel where the value from the mirrored channel is displayed in OpenHAB
	private static final String ACTUATE_MIRROR_CHANNEL = CHANNEL_DEVICE_LOWLEVEL_ON;

	private final Logger logger = LoggerFactory.getLogger(SmartifiedHomeESHHandler.class);

	private JADEBridgeHandler bridgeHandler;
	private ChannelMirror channelMirror;
	private ChannelTypeRegistry channelTypeRegistry;
	private JADEHandlerFactory channelTypeProvider;

	private AgentController agentController;

	private ChannelUID aliveChannelUID;
	private ChannelUID connectedChannelUID;
	private ChannelUID deviceStateChannelUID;
	private ChannelUID managedFromOutsideChannelUID;
	private ChannelUID endTimeChannelUID;
	private ChannelUID endTimeToleranceChannelUID;
	private ChannelUID washingProgramChannelUID;
	private ChannelUID lockedNLoadedChannelUID;

	private ChannelUID measurementOriginalChannelUID;
	private ChannelUID measurementMirrorChannelUID;
	private ChannelUID actuateOriginalChannelUID;
	private ChannelUID actuateMirrorChannelUID;

	private boolean aliveChannelValue = false; // initialize
	private String currentDeviceState;
	private Integer currentWashingProgram = 0; // initialize
	private boolean outsideManagementAllowed;
	private boolean actuateChannelValue = false; // initialize
	private double measurementChannelValue = Double.NEGATIVE_INFINITY; // initialize

	private boolean disposing = false;

	private static final String CHANNEL_READONLY = "RO";
	private static final String CHANNEL_READWRITE = "RW";

	public SmartifiedHomeESHHandler(Thing thing, ChannelMirror channelMirror, ChannelTypeRegistry channelTypeRegistry, JADEHandlerFactory channelTypeProvider) {
		super(thing);
		this.channelMirror = channelMirror;
		this.channelTypeRegistry = channelTypeRegistry;
		this.channelTypeProvider = channelTypeProvider;
/*
System.out.println("channelMirror =" + channelMirror);
System.out.println("channelTypeRegistry =" + channelTypeRegistry);
System.out.println("channelTypeProvider =" + channelTypeProvider);
*/
	}

	public synchronized JADEBridgeHandler getBridgeHandler() {
		if (bridgeHandler == null) {
			Bridge bridge = getBridge();
			if (bridge == null) {
				return null;
			}
			ThingHandler handler = bridge.getHandler();
			if (handler instanceof JADEBridgeHandler) {
				bridgeHandler = (JADEBridgeHandler) handler;
			} else {
				return null;
			}
		}
		return bridgeHandler;
	}

	@Override
	public void initialize() {
		logger.debug("Initializing SmartHomeAgentESHHandler.");

		aliveChannelUID = new ChannelUID(getThing().getUID(), CHANNEL_ALIVE);
		connectedChannelUID = new ChannelUID(getThing().getUID(), CHANNEL_CONNECTED);
		deviceStateChannelUID = new ChannelUID(getThing().getUID(), CHANNEL_DEVICE_STATE);
		managedFromOutsideChannelUID = new ChannelUID(getThing().getUID(), CHANNEL_MANAGED_FROM_OUTSIDE);
		endTimeChannelUID = new ChannelUID(getThing().getUID(), CHANNEL_END_TIME);
		endTimeToleranceChannelUID = new ChannelUID(getThing().getUID(), CHANNEL_END_TIME_TOLERANCE);
		washingProgramChannelUID = new ChannelUID(getThing().getUID(), CHANNEL_WASHING_PROGRAM);
		lockedNLoadedChannelUID = new ChannelUID(getThing().getUID(), CHANNEL_LOCKED_N_LOADED);

		try {
			measurementOriginalChannelUID = new ChannelUID((String) getConfig().getProperties().get(PROPERTY_MEASUREMENT_CHANNEL_UID));
			actuateOriginalChannelUID = new ChannelUID((String) getConfig().getProperties().get(PROPERTY_ACTUATE_CHANNEL_UID));
		} catch (IllegalArgumentException e) {
			fail("start", e.getMessage());
			return;
		}
		measurementMirrorChannelUID = new ChannelUID(getThing().getUID(), MEASUREMENT_MIRROR_CHANNEL);
		actuateMirrorChannelUID = new ChannelUID(getThing().getUID(), ACTUATE_MIRROR_CHANNEL);

		startMirroring(measurementOriginalChannelUID, null);
		startMirroring(actuateOriginalChannelUID, actuateMirrorChannelUID);

		startAgent();
		if (agentController != null) {
			updateStatus(ThingStatus.ONLINE);
		}
		stopAgent();
	}

	private void initChannels() {
		updateState(connectedChannelUID, OnOffType.OFF);
		setDeviceState("Pending");
		handleCommand(managedFromOutsideChannelUID, boolToState(true));
		handleCommand(managedFromOutsideChannelUID, RefreshType.REFRESH);
//        outsideManagementAllowed = true;
//        updateState(managedFromOutsideChannelUID, boolToState(outsideManagementAllowed));
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(new Date());
		updateState(endTimeChannelUID, new DateTimeType(cal));
		updateState(endTimeToleranceChannelUID, new DecimalType(0));
		currentWashingProgram = 0;
		updateState(washingProgramChannelUID, new DecimalType(currentWashingProgram));
		updateState(lockedNLoadedChannelUID, OnOffType.OFF);
		receiveFromMirroredChannel(measurementOriginalChannelUID.getAsString(), new DecimalType(0));
		setActuateChannelValue(true, true);
	}

	/*
	 * usually: only read
	 * if mirrorChannel is given, the mirroring is two-way (read+write) and commands sent to the mirror channel will be passed to the original channel's item
	 */
	private void startMirroring(ChannelUID originalChannel, ChannelUID mirrorChannel) {
//        System.out.println("startMirroring(" + originalChannel + "," + mirrorChannel + ")");

		// init channel with current value
		Item lastItem = null;
		State currentState = null;

		lastItem = getLastChannelItem(originalChannel);

		if (lastItem != null && mirrorChannel != null) { // two-way-case
			currentState = lastItem.getState();
			Set<Item> alreadyLinked = linkRegistry.getLinkedItems(mirrorChannel);
			if (!alreadyLinked.contains(lastItem)) { // only link if not already linked
				// add new item link for command passing
				linkRegistry.add(new ItemChannelLink(lastItem.getName(), mirrorChannel));
			}
		}
		receiveFromMirroredChannel(originalChannel.getAsString(), currentState);

		// subscribe for subsequent changes
		channelMirror.mirrorChannel(originalChannel, this);
	}

	private void stopMirroring(ChannelUID originalChannel) {
		channelMirror.unMirrorChannel(originalChannel, this);
	}

	@Override
	public void receiveFromMirroredChannel(String sourceChannel, State newState) {
		if (sourceChannel.equals(measurementOriginalChannelUID.getAsString())) {
			if (newState instanceof DecimalType) {
				DecimalType decimalItem = (DecimalType) newState;
				measurementChannelValue = decimalItem.doubleValue();
				handleCommand(measurementMirrorChannelUID, RefreshType.REFRESH);
			}
		} else if (sourceChannel.equals(actuateOriginalChannelUID.getAsString())) {
			// this is actually only used anymore for the initial setting of the switch value, subsequent changes will
			// be received directly via the additional item link
			if (newState instanceof OnOffType) {
				actuateChannelValue = stateToBool(newState);
				// System.out.println("receiveFromMirroredChannel sourceChannel==actuateOriginalChannelUID newState=" +
				// newState + " actuateChannelValue==" + actuateChannelValue);

//                handleCommand(actuateMirrorChannelUID, RefreshType.REFRESH);

				// seems to have been catered for by the additional link, not necessary to make it manual anymore?
			}
		} else {
			System.err.println("Unknown source channel " + sourceChannel + " has mirrored state " + newState + " while measurementMirrorChannelUID=" + measurementMirrorChannelUID + " and actuateMirrorChannelUID=" + actuateMirrorChannelUID);
		}

	}

	public void startAgent() {
		try {
			if (getBridgeHandler() == null) {
				fail("agentStart", "no bridge given");
			} else {
				agentController = getBridgeHandler().startAgent((String) getConfig().getProperties().get(PROPERTY_AGENT_ID), SmartifiedHomeAgent.class.getName(), this);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("agentStart", e.getMessage());
		}
	}

	public void stopAgent() {
		try {
			if (getBridgeHandler() == null) {
				fail("agentStop", "no bridge given");
			} else {
				if (getBridgeHandler().stopAgent(this)) {
					agentController = null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("agentStop", e.getMessage());
		}
	}

	public void onAgentStart() {
		aliveChannelValue = true;
		handleCommand(aliveChannelUID, RefreshType.REFRESH);
		initChannels();
	}

	public void onAgentStop() {
		aliveChannelValue = false;
		if (disposing == false) {
			// only call this, if the handler is not already removed
			handleCommand(aliveChannelUID, RefreshType.REFRESH);
		}
		agentController = null;
	}

	public String getAgentName() {
		if (agentController != null) {
			try {
				return agentController.getName();
			} catch (StaleProxyException e) {
				e.printStackTrace();
			}
		}
		return "";
	}

	public AgentController getAgent() {
		if (agentController != null) {
			return agentController;
		}
		return null;
	}

	@Override
	public void dispose() {
		logger.info("Disposing SmartHomeAgentESHHandler.");
		disposing = true;
		stopMirroring(measurementOriginalChannelUID);
		stopMirroring(actuateOriginalChannelUID);
		stopAgent();
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		if (command == RefreshType.REFRESH) {
			State newState = null;
			switch (channelUID.getId()) {
			case CHANNEL_ALIVE: {
				newState = boolToState(aliveChannelValue);
				break;
			}
			case MEASUREMENT_MIRROR_CHANNEL: {
				if (measurementChannelValue != Double.NEGATIVE_INFINITY) {
					newState = new DecimalType(measurementChannelValue);
				}
				break;
			}
			case ACTUATE_MIRROR_CHANNEL: {
				newState = boolToState(actuateChannelValue);
				break;
			}
			case CHANNEL_DEVICE_STATE: {
				newState = new StringType(currentDeviceState);
//                    logger.info("New state " + currentDeviceState);
				break;
			}
			case CHANNEL_MANAGED_FROM_OUTSIDE: {
				newState = boolToState(outsideManagementAllowed);
				break;
			}
			default: {
				logger.info("REFRESH command for unknown channel " + channelUID);
				break;
			}
			}
			if (newState != null) {
//                logger.info("REFRESH command for " + channelUID + ": " + newState + " (should have been handled OK)");
				updateState(channelUID.getId(), newState);
			}
		} else {
			switch (channelUID.getId()) {
			case CHANNEL_ALIVE: {
				if (command == OnOffType.ON) {
					startAgent();
				} else {
					stopAgent();
				}
				break;
			}
			case ACTUATE_MIRROR_CHANNEL: {
				if (command instanceof OnOffType) {
//                        System.out.println("handleCommand channelUID==ACTUATE_MIRROR_CHANNEL actuateChannelValue==" + actuateChannelValue + "=> posting command");
					setActuateChannelValue(stateToBool(command), false);
				}
				break;
			}
			case CHANNEL_WASHING_PROGRAM: {
				if (command instanceof DecimalType) {
					currentWashingProgram = ((DecimalType) command).intValue();
				}
				break;
			}
			case CHANNEL_MANAGED_FROM_OUTSIDE: {
				outsideManagementAllowed = stateToBool(command);
				// setChannelReadOnly(actuateMirrorChannelUID, stateToBool(command));
				break;
			}
			default: {
				logger.info("unrecognized command: " + command + " for channel " + channelUID);
				break;
			}
			}
		}
	}

	// only called by the agent (via one of it's behaviours)
	public double getMeasurementChannelValue() {
		return measurementChannelValue;
	}

	// only called by the agent (via one of it's behaviours)
	public boolean getActuateChannelValue() {
		return actuateChannelValue;
	}

	// called by the agent (via one of it's behaviours) and the handler when receiving a command from the actuateChannel
	public void setActuateChannelValue(boolean actuateValue, boolean calledFromOutside) {
//        System.out.println("setActuateChannelValue=" + actuateValue + ", calledFromOutside=" + calledFromOutside);
		if (!calledFromOutside || (calledFromOutside && outsideManagementAllowed)) {
			actuateChannelValue = actuateValue;
			postCommand(actuateMirrorChannelUID, boolToState(actuateValue));
//            System.out.println("and command actually sent!");
		}
	}

	public boolean getLockedNLoadedValue() {
		return getBooleanValue(lockedNLoadedChannelUID);
	}

	public boolean getBooleanValue(ChannelUID channel) {
		Item lastChannelItem = getLastChannelItem(channel);
		if (lastChannelItem != null && lastChannelItem.getState() instanceof OnOffType) {
			return stateToBool(lastChannelItem.getState());
		}
		return false;
	}

	public Double getDecimalValue(ChannelUID channel) {
		Item lastChannelItem = getLastChannelItem(channel);
		if (lastChannelItem != null && lastChannelItem.getState() instanceof DecimalType) {
			return stateToDecimal(lastChannelItem.getState());
		}
		return -1.0;
	}

	public void setLockedNLoadedValue(boolean lockedNLoadedValue, boolean calledFromOutside) {
		handleCommand(lockedNLoadedChannelUID, boolToState(lockedNLoadedValue));
	}

	public double getEndTimeToleranceValue() {
		return getDecimalValue(endTimeToleranceChannelUID);
	}

	public double getEndTimeValue() {
		return getDecimalValue(endTimeChannelUID);
	}

	public Integer getWashingProgramValue() {
		return currentWashingProgram;
	}

	public void setDeviceState(String newDeviceState) {
		currentDeviceState = newDeviceState;
		handleCommand(deviceStateChannelUID, RefreshType.REFRESH);
	}

	protected Item getLastChannelItem(ChannelUID channel) {
		Set<Item> allLinks = linkRegistry.getLinkedItems(channel);
		Item lastItem = null;
		for (Item item : allLinks) {
			lastItem = item;  // only use the state of the last item, usually there is only one
		}
		return lastItem;
	}

	@SuppressWarnings("unused")
	private void setChannelReadOnly(ChannelUID channelUID, Boolean newReadOnly) {
		logger.error("update Channel " + channelUID + " to ro=" + newReadOnly);

//        String channelID = channelUID.getIdWithoutGroup();
		List<Channel> newChannels = new ArrayList<Channel>();
		List<Channel> oldChannels = getThing().getChannels();
		Channel oldChannel = null;

		for (int i = 0; i < oldChannels.size(); i++) {
			Channel curChannel = oldChannels.get(i);
			if (curChannel.getUID().equals(channelUID)) {
				oldChannel = curChannel;
			} else {
				// keep the other channels
				newChannels.add(curChannel);
			}
		}

		if (oldChannel == null) {
			// channel wasn't found
			logger.error("Channel " + channelUID + " wasn't found");

			return;
		}
		ChannelTypeUID oldChannelTypeUID = oldChannel.getChannelTypeUID();
		String oldChannelTypeUIDString = oldChannelTypeUID.getAsString();

		ChannelType oldChannelType = channelTypeRegistry.getChannelType(oldChannelTypeUID);

		if (oldChannelType == null) {
			logger.error("Old ChannelType " + oldChannelTypeUID + " wasn't found.");

			// Probably after restart etc., the customized channel type is gone and replaced by the base one?

			if (oldChannelTypeUIDString.endsWith(CHANNEL_READONLY) || oldChannelTypeUIDString.endsWith(CHANNEL_READWRITE)) {
				// try the base ChannelTypeUID (without RO or RW)
				oldChannelTypeUID = new ChannelTypeUID(oldChannelTypeUIDString.substring(0, oldChannelTypeUIDString.length() - 2));
			}

			// try again
			oldChannelType = channelTypeRegistry.getChannelType(oldChannelTypeUID);
			if (oldChannelType == null) {
				// still not found
				logger.error("Old ChannelType " + oldChannelTypeUID + " wasn't found, even on the second attempt.");
				return;
			}
		}

		StateDescription oldStateDescription = oldChannelType.getState();
		StateDescription newStateDescription = new StateDescription(oldStateDescription.getMinimum(), oldStateDescription.getMinimum(), oldStateDescription.getStep(), oldStateDescription.getPattern(), newReadOnly, oldStateDescription.getOptions());

		String newChannelTypeUIDString = getThing().getUID().getAsString() + ":" + oldChannelType.getUID().getId();
		if (newReadOnly) {
			newChannelTypeUIDString += CHANNEL_READONLY;
		} else {
			newChannelTypeUIDString += CHANNEL_READWRITE;
		}
		ChannelTypeUID newChannelTypeUID = new ChannelTypeUID(newChannelTypeUIDString);

		ChannelType newChannelType = new ChannelType(new ChannelTypeUID(newChannelTypeUIDString), oldChannelType.isAdvanced(), oldChannelType.getItemType(), oldChannelType.getLabel(), oldChannelType.getDescription(),
				oldChannelType.getCategory(), oldChannelType.getTags(), newStateDescription, oldChannelType.getConfigDescriptionURI());

		channelTypeProvider.removeChannelType(oldChannelType);
		channelTypeProvider.addChannelType(newChannelType);

		Channel newChannel = ChannelBuilder
				.create(channelUID,
						oldChannel.getAcceptedItemType())
				.withType(newChannelTypeUID).build();

		newChannels.add(newChannel);
		ThingBuilder thingBuilder = editThing();
		thingBuilder.withChannels(newChannels);

//        logger.error("Channel " + channelUID + " updated to ro=" + newReadOnly);

		updateThing(thingBuilder.build());

	}

	private void fail(String when, String cause) {
		logger.info("Agent " + when + " failed because: " + cause);

		updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
				"agent " + when + " failed: " + cause);
	}

	private static boolean stateToBool(Object state) {
		if (state instanceof OnOffType) {
			if (state == OnOffType.ON) {
				return true;
			}
		}
		return false;
	}

	private static double stateToDecimal(Object state) {
		if (state instanceof DecimalType) {
			DecimalType decimalState = (DecimalType) state;
			return decimalState.doubleValue();
		}
		return -1;
	}

	private static OnOffType boolToState(boolean bool) {
		if (bool) {
			return OnOffType.ON;
		}
		return OnOffType.OFF;
	}

}