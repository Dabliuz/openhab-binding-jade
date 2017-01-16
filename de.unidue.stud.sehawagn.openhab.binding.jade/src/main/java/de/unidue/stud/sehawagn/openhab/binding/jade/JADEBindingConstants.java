package de.unidue.stud.sehawagn.openhab.binding.jade;

import java.util.Set;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import com.google.common.collect.ImmutableSet;

public class JADEBindingConstants {

    public static final String BINDING_ID = "jade";

    // List all Thing Type UIDs, related to the WMBus Binding
    public final static ThingTypeUID THING_TYPE_JADE_BRIDGE = new ThingTypeUID(BINDING_ID, "jadebridge");
    public final static ThingTypeUID THING_TYPE_JADE_TESTAGENT = new ThingTypeUID(BINDING_ID, "testagent");

    // List all channels
    public static final String CHANNEL_SOMECHANNEL = "some_channel";

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = ImmutableSet.of(THING_TYPE_JADE_BRIDGE,
            THING_TYPE_JADE_TESTAGENT);

    // Bridge config properties
    public static final String CONFKEY_SOME_PARAMETER = "someParameter";
    // public static final String CONFKEY_POLLING_INTERVAL = "pollingInterval";

    // agent config properties
    public static final String PROPERTY_AGENT_ID = "agentId";

}
