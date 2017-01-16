package de.unidue.stud.sehawagn.openhab.binding.jade.handler;

import static de.unidue.stud.sehawagn.openhab.binding.jade.JADEBindingConstants.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.leap.Properties;
import jade.wrapper.ContainerController;

public class JADEBridgeHandler extends ConfigStatusBridgeHandler {
    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_JADE_CONTAINER);

    private Logger logger = LoggerFactory.getLogger(JADEBridgeHandler.class);

    private ContainerController container = null;

    public JADEBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        return Collections.emptyList(); // all good, otherwise add some messages
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // judging from the hue bridge, this seems to be not needed...?
    }

    @Override
    public void initialize() {
        logger.debug("Initializing JADE bridge handler.");
        if (getConfig().get(CONFKEY_SOME_PARAMETER) != null) {
            if (container == null) {
                // Initialize jade container profile and start it
                Properties jadeProperties = new Properties();
                startJadeContainer(jadeProperties);

                // String someParameter = (String) getConfig().get(CONFKEY_SOME_PARAMETER);

                updateStatus(ThingStatus.ONLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.CONFIGURATION_ERROR,
                    "Cannot start JADE bridge container. Some parameter not given.");
        }
    }

    @Override
    public void dispose() {
        logger.debug("JADE bridge Handler disposed.");

        if (container != null) {
            container = null;
        }
    }

    private void startJadeContainer(Properties props) {
        Profile profile = new ProfileImpl(props);
        Runtime.instance().setCloseVM(false);
        if (profile.getBooleanProperty(Profile.MAIN, true)) {
            container = Runtime.instance().createMainContainer(profile);
        } else {
            container = Runtime.instance().createAgentContainer(profile);
        }
        System.out.println("JADE container started. (?)");
        // Runtime.instance().invokeOnTermination(new Terminator());
    }

}