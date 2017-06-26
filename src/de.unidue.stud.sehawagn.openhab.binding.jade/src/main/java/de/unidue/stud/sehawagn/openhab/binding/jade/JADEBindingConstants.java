package de.unidue.stud.sehawagn.openhab.binding.jade;

import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.ImmutableSet;

public class JADEBindingConstants {

    public static final String BINDING_ID = "jade";

    // List all Thing Type UIDs, related to the JADE Binding
    public final static ThingTypeUID THING_TYPE_JADE_CONTAINER = new ThingTypeUID(BINDING_ID, "jadecontainer");
    public final static ThingTypeUID THING_TYPE_JADE_SMARTHOMEAGENT = new ThingTypeUID(BINDING_ID, "smarthomeagent");

    // List all channels
    public static final String CHANNEL_ALIVE = "alive"; // whether the agent is alive
    public static final String CHANNEL_CONNECTED = "connected"; // Whether the agent is connected to a network
                                                                // simulation/the simulation connector is running
    public static final String CHANNEL_DEVICE_STATE = "deviceState"; // the eom state
    public static final String CHANNEL_MANAGED_FROM_OUTSIDE = "managedFromOutside"; // Whether the local agent is free
                                                                                    // to interact with the
    // simulation (send data/receive commands/act on behalf of
    // the commands)
    public static final String CHANNEL_END_TIME = "endTime"; // The time, when the device should be finished with the
                                                             // washing
    public static final String CHANNEL_END_TIME_TOLERANCE = "endTimeTolerance"; // The amount of time, the device is
                                                                                // allowed to finish early.
    public static final String CHANNEL_WASHING_PROGRAM = "washingProgram"; // The currently selected washing program

    public static final String CHANNEL_LOCKED_N_LOADED = "lockedNLoaded"; // Whether the washing machine is loaded with
                                                                          // laundry and detergent and the door is
                                                                          // locked.

    public static final String CHANNEL_DEVICE_POWER_CONSUMPTION = "powerConsumption";
    public static final String CHANNEL_DEVICE_LOWLEVEL_ON = "poweredOn"; // whether the mirrored device is switched on

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_JADE_CONTAINER, THING_TYPE_JADE_SMARTHOMEAGENT);

    // Bridge config properties
    public static final String CONFKEY_LOCAL_MTP_ADDRESS = "localMTPAddress";
    public static final String CONFKEY_REMOTE_MTP_ADDRESS = "remoteMTPAddress";
    public static final String CONFKEY_REMOTE_MTP_PORT = "remoteMTPPort";
    public static final String CONFKEY_REMOTE_MTP_PROTOCOL = "remoteMTPProtocol";
    public static final String CONFKEY_REMOTE_PLATFORM_NAME = "remotePlatformName";
    public static final String CONFKEY_REMOTE_GROUP_ADMIN_NAME = "remoteGroupAdminName";
    public static final String CONFKEY_REMOTE_GROUP_COORDINATOR_NAME = "remoteGroupCoordinatorName";

    // public static final String CONFKEY_POLLING_INTERVAL = "pollingInterval";

    // agent config properties
    public static final String PROPERTY_AGENT_ID = "agentId";
    public static final String PROPERTY_ACTUATE_CHANNEL_UID = "actuateChannelUID";
    public static final String PROPERTY_MEASUREMENT_CHANNEL_UID = "measurementChannelUID";

    public static final String JADE_MTP_CLASSNAME_HTTP = "jade.mtp.http.MessageTransportProtocol";
    public static final String JADE_MTP_PREFIX_HTTP = "http://";
    public static final String JADE_MTP_PORT = "7778";
    public static final String JADE_MTP_SUFFIX = "/acc";
    public static final String JADE_MTP_STRING_HTTP = JADE_MTP_CLASSNAME_HTTP + "(" + JADE_MTP_PREFIX_HTTP + "%s" + ":" + JADE_MTP_PORT + JADE_MTP_SUFFIX + ")";

}
