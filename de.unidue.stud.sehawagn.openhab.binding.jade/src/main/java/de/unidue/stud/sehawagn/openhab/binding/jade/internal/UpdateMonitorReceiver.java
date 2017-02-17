package de.unidue.stud.sehawagn.openhab.binding.jade.internal;

import org.eclipse.smarthome.core.types.State;

public interface UpdateMonitorReceiver {

	void receiveMonitorOutput(State newState);

}
