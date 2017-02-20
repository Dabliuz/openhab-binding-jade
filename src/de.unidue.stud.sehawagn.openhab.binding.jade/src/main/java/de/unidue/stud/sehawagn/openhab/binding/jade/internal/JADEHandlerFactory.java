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
import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirror;

public class JADEHandlerFactory extends BaseThingHandlerFactory {

	private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets
			.union(JADEBridgeHandler.SUPPORTED_THING_TYPES, SmartHomeAgentHandler.SUPPORTED_THING_TYPES);
	private ChannelMirror channelMirror = null;

	@Override
	public boolean supportsThingType(ThingTypeUID thingTypeUID) {
		return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
	}

	@Override
	protected ThingHandler createHandler(Thing thing) {
		ThingTypeUID thingTypeUID = thing.getThingTypeUID();

		if (channelMirror == null) {
			System.err.println("No ChannelMirror available, refraining to create Things or Bridges");
			return null;
		}

		if (thingTypeUID.equals(JADEBindingConstants.THING_TYPE_JADE_CONTAINER)) {
			if (thing instanceof Bridge) {

				JADEBridgeHandler handler = new JADEBridgeHandler((Bridge) thing, channelMirror);

				return handler;
			} else {
				return null;
			}
		} else if (thingTypeUID.equals(JADEBindingConstants.THING_TYPE_JADE_SMARTHOMEAGENT)) {
			return new SmartHomeAgentHandler(thing, channelMirror);
		} else {
			return null;
		}
	}

	/*
	 * this is called automagically on activation by the OSGi framework (see OSGI-INF/JADEHandlerFactory)
	 */
	protected void setChannelMirror(ChannelMirror channelMirror) {
		this.channelMirror = channelMirror;
	}

	/*
	 * this is called automagically on deactivation by the OSGi framework (see OSGI-INF/JADEHandlerFactory)
	 */
	protected void unsetChannelMirror(ChannelMirror channelMirror) {
		this.channelMirror = null;
	}

}
