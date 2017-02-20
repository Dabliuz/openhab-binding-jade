package de.unidue.stud.sehawagn.openhab.channelmirror;

import org.eclipse.smarthome.core.types.State;

public interface ChannelMirrorReceiver {

	void receiveFromMirroredChannel(State newState);

}
