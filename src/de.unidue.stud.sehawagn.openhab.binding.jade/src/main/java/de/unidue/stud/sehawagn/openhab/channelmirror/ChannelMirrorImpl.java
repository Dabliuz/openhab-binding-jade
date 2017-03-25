package de.unidue.stud.sehawagn.openhab.channelmirror;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.AbstractItemEventSubscriber;
import org.eclipse.smarthome.core.items.events.ItemStateEvent;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ChannelMirrorImpl listens on the event bus and passes received status updates
 * to the extra registered items.
 *
 *
 */
public class ChannelMirrorImpl extends AbstractItemEventSubscriber implements ChannelMirror {
    private final Logger logger = LoggerFactory.getLogger(ChannelMirrorImpl.class);

    private ItemRegistry itemRegistry;

    private HashMap<String, ArrayList<ChannelMirrorReceiver>> mirrorRoutes = new HashMap<String, ArrayList<ChannelMirrorReceiver>>();

    protected void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    protected void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    @Override
    protected void receiveUpdate(ItemStateEvent updateEvent) {
        State stateUpdate = updateEvent.getItemState();
        String sourceChannel = updateEvent.getSource();

        if (itemRegistry != null) {
            ArrayList<ChannelMirrorReceiver> mirrorReceivers = mirrorRoutes.get(sourceChannel);
            if (mirrorReceivers != null) {
                for (ChannelMirrorReceiver mirrorReceiver : mirrorReceivers) {
                    mirrorReceiver.receiveFromMirroredChannel(sourceChannel, stateUpdate);
                }
            }
        }
    }

    @Override
    public void mirrorChannel(ChannelUID sourceChannel, ChannelMirrorReceiver updateMonitorRecevier) {
        ArrayList<ChannelMirrorReceiver> receivers = getMirrorReceivers(sourceChannel);
        if (receivers == null) {
            receivers = new ArrayList<ChannelMirrorReceiver>();
        }
        receivers.add(updateMonitorRecevier);
        mirrorRoutes.put(sourceChannel.getAsString(), receivers);
    }

    private ArrayList<ChannelMirrorReceiver> getMirrorReceivers(ChannelUID sourceChannel) {
        return mirrorRoutes.get(sourceChannel.getAsString());
    }

}
