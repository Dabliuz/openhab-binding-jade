package de.unidue.stud.sehawagn.openhab.binding.jade.internal;

import java.util.Set;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;

import com.google.common.collect.Sets;

import de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants;
import de.unidue.stud.sehawagn.openhab.binding.jade.handler.JADEBridgeHandler;
import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartHomeAgentHandler;

public class JADEHandlerFactory extends BaseThingHandlerFactory {

	private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets
			.union(JADEBridgeHandler.SUPPORTED_THING_TYPES, SmartHomeAgentHandler.SUPPORTED_THING_TYPES);
	private ItemUpdateMonitorImpl itemUpdateMonitor = null;

	// private Map<ThingUID, ServiceRegistration<?>> discoveryServiceRegs = new HashMap<>();

	@Override
	public boolean supportsThingType(ThingTypeUID thingTypeUID) {
		return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
	}

	@Override
	protected ThingHandler createHandler(Thing thing) {
		ThingTypeUID thingTypeUID = thing.getThingTypeUID();

		if (itemUpdateMonitor == null) {
			System.err.println("No ItemUpdateMonitor available, refraining to create Things or Bridges");
			return null;
		}

		if (thingTypeUID.equals(JADEBindingConstants.THING_TYPE_JADE_CONTAINER)) {
			if (thing instanceof Bridge) {

				JADEBridgeHandler handler = new JADEBridgeHandler((Bridge) thing, itemUpdateMonitor);

				registerDiscoveryService(handler);
				return handler;
			} else {
				return null;
			}
		} else if (thingTypeUID.equals(JADEBindingConstants.THING_TYPE_JADE_SMARTHOMEAGENT)) {
			return new SmartHomeAgentHandler(thing, itemUpdateMonitor);
		} else {
			return null;
		}
	}

	private synchronized void registerDiscoveryService(JADEBridgeHandler bridgeHandler) {
		/*
		 * JADEAgentDiscoveryService discoveryService = new JADEAgentDiscoveryService(bridgeHandler);
		 * discoveryService.activate();
		 * this.discoveryServiceRegs.put(bridgeHandler.getThing().getUID(), bundleContext
		 * .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
		 */
	}

	/*
	 * this is called automagically on activation by the OSGi framework (see OSGI-INF/JADEHandlerFactory)
	 */
	protected void setItemUpdateMonitor(ItemUpdateMonitorImpl itemUpdateMonitor) {
		this.itemUpdateMonitor = itemUpdateMonitor;
	}

	/*
	 * this is called automagically on deactivation by the OSGi framework (see OSGI-INF/JADEHandlerFactory)
	 */
	protected void unsetItemUpdateMonitor(ItemUpdateMonitorImpl itemUpdateMonitor) {
		this.itemUpdateMonitor = null;
	}

}
