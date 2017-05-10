package de.unidue.stud.sehawagn.openhab.binding.jade.handler;

import static de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
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

import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirror;
import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirrorReceiver;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class SmartHomeAgentESHHandler extends BaseThingHandler implements ChannelMirrorReceiver {
    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_JADE_SMARTHOMEAGENT);

    private static final String AGENT_CLASS_NAME = "de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent.SmartHomeAgent";

    // the channel where the value from the mirrored channel is displayed in OpenHAB
    private static final String MEASUREMENT_MIRROR_CHANNEL = CHANNEL_DEVICE_POWER_CONSUMPTION;

    // the channel where the value from the mirrored channel is displayed in OpenHAB
    private static final String ACTUATE_MIRROR_CHANNEL = CHANNEL_DEVICE_LOWLEVEL_ON;

    private final Logger logger = LoggerFactory.getLogger(SmartHomeAgentESHHandler.class);

    private JADEBridgeHandler bridgeHandler;
    private ChannelMirror channelMirror;
    private AgentController myAgent;

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

    public SmartHomeAgentESHHandler(Thing thing, ChannelMirror channelMirror) {
        super(thing);
        this.channelMirror = channelMirror;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing SmartHomeAgentHandler.");

        aliveChannelUID = new ChannelUID(this.getThing().getUID(), CHANNEL_ALIVE);
        connectedChannelUID = new ChannelUID(this.getThing().getUID(), CHANNEL_CONNECTED);
        deviceStateChannelUID = new ChannelUID(this.getThing().getUID(), CHANNEL_DEVICE_STATE);
        managedFromOutsideChannelUID = new ChannelUID(this.getThing().getUID(), CHANNEL_MANAGED_FROM_OUTSIDE);
        endTimeChannelUID = new ChannelUID(this.getThing().getUID(), CHANNEL_END_TIME);
        endTimeToleranceChannelUID = new ChannelUID(this.getThing().getUID(), CHANNEL_END_TIME_TOLERANCE);
        washingProgramChannelUID = new ChannelUID(this.getThing().getUID(), CHANNEL_WASHING_PROGRAM);
        lockedNLoadedChannelUID = new ChannelUID(this.getThing().getUID(), CHANNEL_LOCKED_N_LOADED);

        try {
            measurementOriginalChannelUID = new ChannelUID((String) getConfig().getProperties().get(PROPERTY_MEASUREMENT_CHANNEL_UID));
            actuateOriginalChannelUID = new ChannelUID((String) getConfig().getProperties().get(PROPERTY_ACTUATE_CHANNEL_UID));
        } catch (IllegalArgumentException e) {
            fail("start", e.getMessage());
            return;
        }
        measurementMirrorChannelUID = new ChannelUID(this.getThing().getUID(), MEASUREMENT_MIRROR_CHANNEL);
        actuateMirrorChannelUID = new ChannelUID(this.getThing().getUID(), ACTUATE_MIRROR_CHANNEL);

        startMirroring(measurementOriginalChannelUID, null);
        startMirroring(actuateOriginalChannelUID, actuateMirrorChannelUID);

        startAgent();
        if (myAgent != null) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    /*
     * usually: only read
     * if mirrorChannel is given, the mirroring is two-way (read+write) and commands sent to the mirror channel will be passed to the original channel's item
     */
    private void startMirroring(ChannelUID originalChannel, ChannelUID mirrorChannel) {
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

    public void startAgent() {
        try {
            if (getBridgeHandler() == null) {
                fail("agentStart", "no bridge given");
            } else {
                myAgent = getBridgeHandler().startAgent((String) getConfig().getProperties().get(PROPERTY_AGENT_ID), AGENT_CLASS_NAME,
                        this);
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
                    myAgent = null;
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
    }

    public void onAgentStop() {
        aliveChannelValue = false;
        if (disposing == false) {
            // only call this, if the handler is not already removed
            handleCommand(aliveChannelUID, RefreshType.REFRESH);
        }
        myAgent = null;
    }

    public String getAgentName() {
        if (myAgent != null) {
            try {
                return myAgent.getName();
            } catch (StaleProxyException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public AgentController getAgent() {
        if (myAgent != null) {
            return myAgent;
        }
        return null;
    }

    private void fail(String when, String cause) {
        logger.info("Agent " + when + " failed because: " + cause);

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                "agent " + when + " failed: " + cause);
    }

    @Override
    public void dispose() {
        logger.info("Disposing SmartHomeAgentHandler.");
        disposing = true;
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
                    break;
                }
                default: {
                    logger.info("unrecognized command: " + command + " for channel " + channelUID);
                    break;
                }
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
//                System.out.println("receiveFromMirroredChannel sourceChannel==actuateOriginalChannelUID newState=" + newState + " actuateChannelValue==" + actuateChannelValue);

                handleCommand(actuateMirrorChannelUID, RefreshType.REFRESH);
            }
        } else {
            System.err.println("Unknown source channel " + sourceChannel + " has mirrored state " + newState + " while measurementMirrorChannelUID=" + measurementMirrorChannelUID + " and actuateMirrorChannelUID=" + actuateMirrorChannelUID);
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

    public void setDeviceState(String newDeviceState) {
        currentDeviceState = newDeviceState;
        handleCommand(deviceStateChannelUID, RefreshType.REFRESH);
    }

    protected Item getLastChannelItem(ChannelUID channel) {
        Set<Item> allLinks = this.linkRegistry.getLinkedItems(channel);
        Item lastItem = null;
        for (Item item : allLinks) {
            lastItem = item;  // only use the state of the last item, usually there is only one
        }
        return lastItem;
    }

    public boolean getLockedNLoadedValue() {
        Item lastChannelItem = getLastChannelItem(lockedNLoadedChannelUID);
        if (lastChannelItem != null && lastChannelItem.getState() instanceof OnOffType) {
            return stateToBool(lastChannelItem.getState());
        }
        return false;
    }

    public Integer getWashingProgramValue() {
        return currentWashingProgram;
    }

    private static boolean stateToBool(Object state) {
        if (state instanceof OnOffType) {
            if (state == OnOffType.ON) {
                return true;
            }
        }
        return false;
    }

    private static OnOffType boolToState(boolean bool) {
        if (bool) {
            return OnOffType.ON;
        }
        return OnOffType.OFF;
    }
}