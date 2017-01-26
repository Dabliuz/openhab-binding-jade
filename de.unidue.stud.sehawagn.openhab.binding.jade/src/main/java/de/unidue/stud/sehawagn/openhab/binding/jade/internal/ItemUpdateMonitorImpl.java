package de.unidue.stud.sehawagn.openhab.binding.jade.internal;

import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemNotFoundException;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.AbstractItemEventSubscriber;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemStateEvent;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ItemUpdateMonitor listens on the event bus and passes received status updates which matches the filter criteria
 * to the extra registered items.
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Stefan Bußweiler - Migration to new ESH event concept
 * @author Hanno Felix Wagner - Copy {@link ItemUpdater} as a template
 */
public class ItemUpdateMonitorImpl extends AbstractItemEventSubscriber implements ItemUpdateMonitor {

	private final Logger logger = LoggerFactory.getLogger(ItemUpdateMonitorImpl.class);

	private ItemRegistry itemRegistry;

	protected void setItemRegistry(ItemRegistry itemRegistry) {
		logger.error("MONITOR: Should never happen: setItemRegistry()");

		this.itemRegistry = itemRegistry;
	}

	protected void unsetItemRegistry(ItemRegistry itemRegistry) {
		logger.error("MONITOR: Should never happen: unsetItemRegistry()");

		this.itemRegistry = null;
	}

	@Override
	protected void receiveUpdate(ItemStateEvent updateEvent) {
		String itemName = updateEvent.getItemName();
		State newState = updateEvent.getItemState();
		logger.error("MONITOR: Received update for item: {} to new state: {}", itemName, newState);
		if (itemRegistry != null) {
			// String itemName = updateEvent.getItemName();
			// State newState = updateEvent.getItemState();
			try {
				GenericItem item = (GenericItem) itemRegistry.getItem(itemName);
				boolean isAccepted = false;
				if (item.getAcceptedDataTypes().contains(newState.getClass())) {
					isAccepted = true;
				} else {
					// Look for class hierarchy
					for (Class<? extends State> state : item.getAcceptedDataTypes()) {
						try {
							if (!state.isEnum()
									&& state.newInstance().getClass().isAssignableFrom(newState.getClass())) {
								isAccepted = true;
								break;
							}
						} catch (InstantiationException e) {
							logger.warn("InstantiationException on {}", e.getMessage()); // Should never happen
						} catch (IllegalAccessException e) {
							logger.warn("IllegalAccessException on {}", e.getMessage()); // Should never happen
						}
					}
				}
				if (isAccepted) {
					item.setState(newState);
				} else {
					logger.debug("Received update of a not accepted type (" + newState.getClass().getSimpleName()
							+ ") for item " + itemName);
				}
			} catch (ItemNotFoundException e) {
				logger.debug("Received update for non-existing item: {}", e.getMessage());
			}
		}
	}

	@Override
	protected void receiveCommand(ItemCommandEvent commandEvent) {
		// if the item is a group, we have to pass the command to it as it needs to pass the command to its members
		if (itemRegistry != null) {
			try {
				Item item = itemRegistry.getItem(commandEvent.getItemName());
				if (item instanceof GroupItem) {
					GroupItem groupItem = (GroupItem) item;
					groupItem.send(commandEvent.getItemCommand());
				}
			} catch (ItemNotFoundException e) {
				logger.debug("Received command for non-existing item: {}", e.getMessage());
			}
		}
	}

	public void monitorChannel(ChannelUID inputChannel, ChannelUID displayChannel) {

		logger.error("MONITOR: Now I want to Mirror the Channel {} to {}", inputChannel, displayChannel);

	}

}
