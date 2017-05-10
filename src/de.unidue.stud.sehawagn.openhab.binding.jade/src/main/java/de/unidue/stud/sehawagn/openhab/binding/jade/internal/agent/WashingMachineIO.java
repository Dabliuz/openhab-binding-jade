package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartifiedHomeESHHandler;
import hygrid.agent.EnergyAgentIO;

public interface WashingMachineIO extends EnergyAgentIO {

    void setESHHandler(SmartifiedHomeESHHandler myAgentHandler);

    void onAgentStart();

    void onAgentStop();

    Integer getWashingProgram();

    boolean getLockedNLoaded();

    double getPowerConsumption();

    boolean getPoweredOn();

    void setPoweredOn(Boolean poweredOn);

}
