package de.unidue.stud.sehawagn.openhab.binding.jade.internal;

import java.util.Set;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;

import de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants;
import de.unidue.stud.sehawagn.openhab.binding.jade.handler.JADEBridgeHandler;

public class JADEHandlerFactory extends BaseThingHandlerFactory {

    // private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets
    // .union(JADEBridgeHandler.SUPPORTED_THING_TYPES, WMBusTechemHKVHandler.SUPPORTED_THING_TYPES);

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = JADEBridgeHandler.SUPPORTED_THING_TYPES;

    // private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(JADEBindingConstants.THING_TYPE_JADE_BRIDGE)) {
            if (thing instanceof Bridge) {
                JADEBridgeHandler handler = new JADEBridgeHandler((Bridge) thing);
                registerDiscoveryService(handler);
                return handler;
            } else {
                return null;
            }
        } else if (thingTypeUID.equals(JADEBindingConstants.THING_TYPE_JADE_TESTAGENT)) {
            // return new WMBusTechemHKVHandler(thing);
            return null;
        } else {
            return null;
        }
    }

    private synchronized void registerDiscoveryService(JADEBridgeHandler bridgeHandler) {
        /*
         * WMBusHKVDiscoveryService discoveryService = new WMBusHKVDiscoveryService(bridgeHandler);
         * discoveryService.activate();
         * this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
         * .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
         */
    }

}
