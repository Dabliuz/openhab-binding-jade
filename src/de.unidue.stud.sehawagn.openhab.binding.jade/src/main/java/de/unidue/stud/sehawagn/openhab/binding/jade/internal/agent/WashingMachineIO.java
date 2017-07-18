package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartifiedHomeESHHandler;
import energy.optionModel.TechnicalSystemStateEvaluation;
import hygrid.agent.EnergyAgentIO;
import hygrid.agent.monitoring.MonitoringListener;

public interface WashingMachineIO extends EnergyAgentIO, MonitoringListener {

    void setESHHandler(SmartifiedHomeESHHandler myAgentHandler);

    void onAgentStart();

    void onAgentStop();

    Integer getWashingProgram();

    boolean getLockedNLoaded();

    double getPowerConsumption();

    boolean getPoweredOn();

    void setPoweredOn(Boolean poweredOn);

    void setUnlocked();

    void updateEOMState();

    void setEOMState(TechnicalSystemStateEvaluation eomState);

    TechnicalSystemStateEvaluation getEOMState();

}
