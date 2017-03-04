package de.unidue.stud.sehawagn.openhab.binding.jade.handler;

import static de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants.*;

import java.util.Set;

import org.eclipse.smarthome.config.core.Configuration;
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
    private static final String MEASUREMENT_MIRROR_CHANNEL = CHANNEL_POWER; // the channel where the value from the
                                                                            // mirrored
    // channel is displayed in OpenHAB
    private static final String ACTUATE_MIRROR_CHANNEL = CHANNEL_ON; // the channel where the value from the mirrored
                                                                     // channel is displayed in OpenHAB

    private final Logger logger = LoggerFactory.getLogger(SmartHomeAgentHandler.class);

    private JADEBridgeHandler bridgeHandler;
    private ChannelMirror channelMirror;

    private ChannelUID measurementChannelUID;
    private ChannelUID measurementMirrorChannelUID;
    private ChannelUID actuateMirrorChannelUID;

    private boolean actuateChannelValue = false; // initialize
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

        try {
            measurementChannelUID = new ChannelUID((String) config.getProperties().get(PROPERTY_MEASUREMENT_CHANNEL_UID));
        } catch (IllegalArgumentException e) {
            fail("start", e.getMessage());

            return;
        }
        measurementMirrorChannelUID = new ChannelUID(this.getThing().getUID(), MEASUREMENT_MIRROR_CHANNEL);

        actuateMirrorChannelUID = new ChannelUID(this.getThing().getUID(), ACTUATE_MIRROR_CHANNEL);

        State currentState = null;
        Set<Item> allLinks = this.linkRegistry.getLinkedItems(measurementChannelUID);
        for (Item item : allLinks) {
            currentState = item.getState(); // only use the state of the last, usually there is only one
        }
        receiveFromMirroredChannel(currentState); // init channel with current value before waiting for the next change

//        channelMirror.mirrorChannel(, this);
        channelMirror.mirrorChannel(measurementChannelUID, this);
        String errorCause = null;

        try {
            if (getBridgeHandler() == null) {
                errorCause = "no bridge given";
            } else {
//                myAgent = getBridgeHandler().startAgent((String) config.getProperties().get(PROPERTY_AGENT_ID), AGENT_CLASS,
//                        this);
                myAgent = getBridgeHandler().startAgent((String) config.getProperties().get(PROPERTY_AGENT_ID), AGENT_CLASS_NAME,
                        this);
            }
        } catch (StaleProxyException e) {
            errorCause = e.getMessage();
        }
        if (myAgent != null) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            fail("start", errorCause);
        }
    }

    private void fail(String when, String cause) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                "agent " + when + " failed: " + cause);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing SmartHomeAgentHandler.");
        try {
            if (getBridgeHandler() != null) {
                getBridgeHandler().stopAgent(this);
            }
        } catch (StaleProxyException e) {
            fail("dispose", e.getMessage());
            e.printStackTrace();
        }
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
                default: {
                    break;
                }
            }
            if (newState != null) {
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
            handleCommand(measurementMirrorChannelUID, RefreshType.REFRESH);
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