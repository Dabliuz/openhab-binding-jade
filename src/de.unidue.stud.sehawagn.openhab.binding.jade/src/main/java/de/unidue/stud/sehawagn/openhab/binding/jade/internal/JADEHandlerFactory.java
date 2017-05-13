package de.unidue.stud.sehawagn.openhab.binding.jade.internal;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeRegistry;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants;
import de.unidue.stud.sehawagn.openhab.binding.jade.handler.JADEBridgeHandler;
import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartifiedHomeESHHandler;
import de.unidue.stud.sehawagn.openhab.channelmirror.ChannelMirror;
import jade.osgi.service.runtime.JadeRuntimeService;

public class JADEHandlerFactory extends BaseThingHandlerFactory implements ChannelTypeProvider {
    private final Logger logger = LoggerFactory.getLogger(JADEHandlerFactory.class);

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets
            .union(JADEBridgeHandler.SUPPORTED_THING_TYPES, SmartifiedHomeESHHandler.SUPPORTED_THING_TYPES);
    private ChannelMirror channelMirror = null;

    private JadeRuntimeService jrs;

    private ChannelTypeRegistry channelTypeRegistry = null;

    private List<ChannelType> channelTypes = new CopyOnWriteArrayList<ChannelType>();
    private List<ChannelGroupType> channelGroupTypes = new CopyOnWriteArrayList<ChannelGroupType>();

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (channelMirror == null) {
            logger.error("No ChannelMirror available, refraining to create Things or Bridges");
            return null;
        }

        if (thingTypeUID.equals(JADEBindingConstants.THING_TYPE_JADE_CONTAINER)) {
            if (thing instanceof Bridge) {

                JADEBridgeHandler handler = new JADEBridgeHandler((Bridge) thing, channelMirror, jrs);

                return handler;
            } else {
                return null;
            }
        } else if (thingTypeUID.equals(JADEBindingConstants.THING_TYPE_JADE_SMARTHOMEAGENT)) {
            return new SmartifiedHomeESHHandler(thing, channelMirror, channelTypeRegistry, this);
        } else {
            return null;
        }
    }

    /*
     * this is called automagically on activation by the OSGi framework (see OSGI-INF/JADEHandlerFactory.xml)
     */
    protected void setChannelMirror(ChannelMirror channelMirror) {
        this.channelMirror = channelMirror;
    }

    /*
     * this is called automagically on deactivation by the OSGi framework (see OSGI-INF/JADEHandlerFactory.xml)
     */
    protected void unsetChannelMirror(ChannelMirror channelMirror) {
        this.channelMirror = null;
    }

    /*
     * this is called automagically on activation by the OSGi framework (see OSGI-INF/JADEHandlerFactory.xml)
     */
    protected void setJadeRuntimeService(JadeRuntimeService jrs) {
        this.jrs = jrs;
    }

    /*
     * this is called automagically on deactivation by the OSGi framework (see OSGI-INF/JADEHandlerFactory.xml)
     */
    protected void unsetJadeRuntimeService(JadeRuntimeService jrs) {
        this.jrs = null;
    }

    /*
     * this is called automagically on activation by the OSGi framework (see OSGI-INF/JADEHandlerFactory.xml)
     */
    public void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    /*
     * this is called automagically on deactivation by the OSGi framework (see OSGI-INF/JADEHandlerFactory.xml)
     */
    public void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }

    @Override
    public Collection<ChannelType> getChannelTypes(Locale locale) {
        return channelTypes;
    }

    @Override
    public ChannelType getChannelType(ChannelTypeUID channelTypeUID, Locale locale) {
        for (ChannelType channelType : channelTypes) {
            if (channelType.getUID().equals(channelTypeUID)) {
                return channelType;
            }
        }
        return null;
    }

    @Override
    public ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, Locale locale) {
        for (ChannelGroupType channelGroupType : channelGroupTypes) {
            if (channelGroupType.getUID().equals(channelGroupTypeUID)) {
                return channelGroupType;
            }
        }
        return null;
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(Locale locale) {
        return channelGroupTypes;

    }

    public void addChannelType(ChannelType type) {
        channelTypes.add(type);
    }

    public void removeChannelType(ChannelType type) {
        channelTypes.remove(type);
    }

}
