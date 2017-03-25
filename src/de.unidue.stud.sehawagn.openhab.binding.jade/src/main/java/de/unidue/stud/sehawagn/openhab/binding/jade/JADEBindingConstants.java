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
    public static final String CHANNEL_POWER_CONSUMMPTION = "powerConsumption";
    public static final String CHANNEL_ALIVE = "alive"; // whether the agent is alive
    public static final String CHANNEL_ON = "on"; // whether the mirrored device is switched on

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_JADE_CONTAINER, THING_TYPE_JADE_SMARTHOMEAGENT);

    // Bridge config properties
    public static final String CONFKEY_LOCAL_MTP_ADDRESS = "localMTPAddress";

    public static final String CONFKEY_REMOTE_MTP_ADDRESS = "remoteMTPAddress";
    public static final String CONFKEY_REMOTE_MTP_PORT = "remoteMTPPort";
    public static final String CONFKEY_REMOTE_MTP_PROTOCOL = "remoteMTPProtocol";
    public static final String CONFKEY_REMOTE_PLATFORM_NAME = "remotePlatformName";
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
