package de.unidue.stud.sehawagn.openhab.binding.jade.handler;

import static de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
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

import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirror;
import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirrorReceiver;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class SmartHomeAgentHandler extends BaseThingHandler implements ChannelMirrorReceiver {
    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets.newHashSet(THING_TYPE_JADE_SMARTHOMEAGENT);

//    private static final Class<? extends Agent> AGENT_CLASS = SmartHomeAgent.class;
    private static final String AGENT_CLASS_NAME = "de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent.SmartHomeAgent";

    private static final String MEASUREMENT_MIRROR_CHANNEL = CHANNEL_POWER_CONSUMMPTION; // the channel where the value
                                                                                         // from the
    // mirrored
    // channel is displayed in OpenHAB
    private static final String ACTUATE_MIRROR_CHANNEL = CHANNEL_ON; // the channel where the value from the mirrored
                                                                     // channel is displayed in OpenHAB

    private final Logger logger = LoggerFactory.getLogger(SmartHomeAgentHandler.class);

    private JADEBridgeHandler bridgeHandler;
    private ChannelMirror channelMirror;

    private ChannelUID measurementOriginalChannelUID;
    private ChannelUID actuateOriginalChannelUID;
    private ChannelUID measurementMirrorChannelUID;
    private ChannelUID actuateMirrorChannelUID;
    private ChannelUID agentAliveChannelUID;

    private boolean actuateChannelValue = false; // initialize
    private double measurementChannelValue = Double.NEGATIVE_INFINITY; // initialize

    private AgentController myAgent;
    private boolean agentAlive = false;

    public SmartHomeAgentHandler(Thing thing, ChannelMirror channelMirror) {
        super(thing);
        this.channelMirror = channelMirror;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing SmartHomeAgentHandler.");

        try {
            measurementOriginalChannelUID = new ChannelUID((String) getConfig().getProperties().get(PROPERTY_MEASUREMENT_CHANNEL_UID));
            actuateOriginalChannelUID = new ChannelUID((String) getConfig().getProperties().get(PROPERTY_ACTUATE_CHANNEL_UID));
        } catch (IllegalArgumentException e) {
            fail("start", e.getMessage());

            return;
        }
        measurementMirrorChannelUID = new ChannelUID(this.getThing().getUID(), MEASUREMENT_MIRROR_CHANNEL);
        actuateMirrorChannelUID = new ChannelUID(this.getThing().getUID(), ACTUATE_MIRROR_CHANNEL);
        agentAliveChannelUID = new ChannelUID(this.getThing().getUID(), CHANNEL_ALIVE);

        startMirroing(measurementOriginalChannelUID);
        startMirroing(actuateOriginalChannelUID);

        startAgent();
        if (myAgent != null) {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    private void startMirroing(ChannelUID originalChannel) {
        // init channel with current value
        State currentState = null;
        Set<Item> allLinks = this.linkRegistry.getLinkedItems(originalChannel);
        for (Item item : allLinks) {
            currentState = item.getState(); // only use the state of the last, usually there is only one
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
        agentAlive = true;
        handleCommand(agentAliveChannelUID, RefreshType.REFRESH);
    }

    public void onAgentStop() {
        agentAlive = false;
        handleCommand(agentAliveChannelUID, RefreshType.REFRESH);
        myAgent = null;
    }

    public String getAgentName() {
        if (myAgent != null) {
            try {
                return myAgent.getName();
            } catch (StaleProxyException e) {
                // TODO Auto-generated catch block
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
        logger.info("bladiblah agent " + when + " failed because: " + cause);

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                "agent " + when + " failed: " + cause);
    }

    @Override
    public void dispose() {
        logger.info("Disposing SmartHomeAgentHandler.");
        stopAgent();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command == RefreshType.REFRESH) {
            State newState = null;
            switch (channelUID.getId()) {
                case MEASUREMENT_MIRROR_CHANNEL: {
                    if (measurementChannelValue != Double.NEGATIVE_INFINITY) {
                        newState = new DecimalType(measurementChannelValue);
                    }
                    break;
                }
                case ACTUATE_MIRROR_CHANNEL: {
                    if (actuateChannelValue) {
                        newState = OnOffType.ON;
                    } else {
                        newState = OnOffType.OFF;
                    }
                    break;
                }
                case CHANNEL_ALIVE: {
                    if (agentAlive) {
                        newState = OnOffType.ON;
                    } else {
                        newState = OnOffType.OFF;
                    }
                    break;
                }
                default: {
                    logger.info("REFRESH command for unknown channel " + channelUID);
                    break;
                }
            }
            if (newState != null) {
                logger.info("REFRESH command for " + channelUID + ": " + newState + " (should have been handeled OK)");

                updateState(channelUID.getId(), newState);
            }
        } else if (channelUID.getId().equals(CHANNEL_ALIVE)) {
            if (command == OnOffType.ON) {
                startAgent();
//                logger.info("command: " + command + " for channel " + channelUID + " starting agent");
            } else {
                stopAgent();
//                logger.info("command: " + command + " for channel " + channelUID + " stopping agent");
            }
        } else {
            switch (channelUID.getId()) {
                case ACTUATE_MIRROR_CHANNEL: {
                    if (command == OnOffType.ON) {
                        updateState(actuateOriginalChannelUID, OnOffType.ON);
                    } else {
                        updateState(actuateOriginalChannelUID, OnOffType.OFF);
                    }
                    break;
                }
                default: {
                    logger.info("unrecognized command: " + command + " for channel " + channelUID);
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
            if (newState instanceof OnOffType) {
                if (newState == OnOffType.ON) {
                    actuateChannelValue = true;
                } else {
                    actuateChannelValue = false;
                }
                handleCommand(actuateMirrorChannelUID, RefreshType.REFRESH);
            }
        } else {
            System.err.println("Unknown source channel " + sourceChannel + " has mirrored state " + newState + " while measurementMirrorChannelUID=" + measurementMirrorChannelUID + " and actuateMirrorChannelUID=" + actuateMirrorChannelUID);
        }

    }

    public double getCurrentMeasurement() {
        return measurementChannelValue;
    }

    public void setOperationalState(boolean operationalState) {
        actuateChannelValue = operationalState;
        handleCommand(actuateMirrorChannelUID, RefreshType.REFRESH);
    }
}