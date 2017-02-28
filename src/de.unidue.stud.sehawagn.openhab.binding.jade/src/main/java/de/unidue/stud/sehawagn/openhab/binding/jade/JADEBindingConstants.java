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
    public static final String CHANNEL_POWER = "power";
    public static final String CHANNEL_ALIVE = "alive"; // whether the agent is alive
    public static final String CHANNEL_ON = "on"; // whether the mirrored device is switched on

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_JADE_CONTAINER, THING_TYPE_JADE_SMARTHOMEAGENT);

    // Bridge config properties
    public static final String CONFKEY_LOCAL_HOST_ADDRESS = "localHostAddress";

    public static final String CONFKEY_MTP_ADDRESS = "mtpAddress";
    public static final String CONFKEY_MTP_PORT = "mtpPort";
    public static final String CONFKEY_MTP_PROTOCOL = "mtpProtocol";
    public static final String CONFKEY_PLATFORM_NAME = "platformName";
    public static final String CONFKEY_CENTRAL_AGENT_NAME = "centralAgentName";

    // public static final String CONFKEY_POLLING_INTERVAL = "pollingInterval";

    // agent config properties
    public static final String PROPERTY_AGENT_ID = "agentId";
    public static final String PROPERTY_ACTUATE_CHANNEL_UID = "actuateChannelUID";
    public static final String PROPERTY_MEASUREMENT_CHANNEL_UID = "measurementChannelUID";

}
