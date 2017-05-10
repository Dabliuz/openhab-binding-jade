package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartHomeAgentESHHandler;
import hygrid.agent.EnergyAgentIO;

public interface WashingMashineIO extends EnergyAgentIO {

    double getPowerConsumption();

    boolean getPoweredOn();

    void setPoweredOn(Boolean poweredOn);

    boolean getLockedNLoaded();

    Integer getWashingProgram();

    void onAgentStart();

    void setESHHandler(SmartHomeAgentESHHandler myAgentHandler);

    void onAgentStop();

}
