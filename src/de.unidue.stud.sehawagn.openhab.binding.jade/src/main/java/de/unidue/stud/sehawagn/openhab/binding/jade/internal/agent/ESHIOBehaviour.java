package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartifiedHomeESHHandler;
import energy.FixedVariableList;
import energy.optionModel.TechnicalSystemStateEvaluation;
import hygrid.agent.AbstractIOReal;
import hygrid.agent.monitoring.MonitoringEvent;

/**
 * This class is used if the agent is run inside an openHAB instance
 * It reads data from it's agent handler
 */
public class ESHIOBehaviour extends AbstractIOReal implements WashingMachineIO {

    private static final long serialVersionUID = 5143063807591183507L;

    private static final long MEASURING_INTERVAL = 1000;

    protected SmartifiedHomeAgent myAgent;
    protected InternalDataModel internalDataModel = null;

    protected SmartifiedHomeESHHandler myESHHandler;

    protected TechnicalSystemStateEvaluation eomState = null;

    /**
     * @param agent the agent
     * @param myESHHandler
     */
    public ESHIOBehaviour(SmartifiedHomeAgent agent) {
        super(agent);
        myAgent = agent;
        internalDataModel = myAgent.getInternalDataModel();
    }

    @Override
    public void setESHHandler(SmartifiedHomeESHHandler myESHHandler) {
        this.myESHHandler = myESHHandler;
    }

    @Override
    public void action() {
        // access the data in openHAB's AgentHandler
        FixedVariableList measurements;
        measurements = InternalDataModel.produceVariableList(getPowerConsumption(),
                InternalDataModel.VAR_POWER_CONSUMPTION);
        setMeasurementsFromSystem(measurements);

        updateEOMState();

        block(MEASURING_INTERVAL);
    }

    // called by MonitoringBehaviour
    @Override
    public FixedVariableList getMeasurementsFromSystem() {
        return internalDataModel.getMeasurementsFromSystem();
    }

    // only used internally
    @Override
    public void setMeasurementsFromSystem(FixedVariableList newMeasurements) {
        // important because of observable pattern: every time the measurements are set, the ControlBehaviour is
        // triggered
        internalDataModel.setMeasurementsFromSystem(newMeasurements);
    }

    // called by MonitoringBehaviour
    @Override
    public FixedVariableList getSetPointsToSystem() {
        FixedVariableList setPoints = null;
        setPoints = InternalDataModel.produceVariableList(getWashingProgram(), InternalDataModel.VAR_WASHING_PROGRAM);
        setPoints.add(InternalDataModel.produceVariable(getLockedNLoaded(), InternalDataModel.VAR_LOCKED_N_LOADED));
        setPoints.add(InternalDataModel.produceVariable(getPoweredOn(), InternalDataModel.VAR_POWERED_ON));
        return setPoints;
    }

    // called by ControlBehaviourRT
    @Override
    public void setSetPointsToSystem(FixedVariableList newSetPoints) {
        boolean poweredOn = InternalDataModel.deriveVariable(newSetPoints, InternalDataModel.VAR_POWERED_ON);
        boolean lockedNLoaded = InternalDataModel.deriveVariable(newSetPoints, InternalDataModel.VAR_LOCKED_N_LOADED);
        Integer washingProgram = InternalDataModel.deriveVariable(newSetPoints, InternalDataModel.VAR_WASHING_PROGRAM);
        setPoweredOn(poweredOn);
        // TODO implement automatic switchoff at the end
        /*
         * if (poweredOn && !lockedNLoaded && washingProgram == 4 && getEOMState() ==
         * InternalDataModel.EOM_STATE_AUTOMATIC_END) {
         * setUnlocked();
         * }
         */
        }

    @Override
    public void onAgentStart() {
        if (myESHHandler != null) {
            myESHHandler.onAgentStart();
        }
    }

    @Override
    public void onAgentStop() {
        if (myESHHandler != null) {
            myESHHandler.onAgentStop();
        }
    }

    @Override
    public Integer getWashingProgram() {
        return myESHHandler.getWashingProgramValue();
    }

    @Override
    public boolean getLockedNLoaded() {
        return myESHHandler.getLockedNLoadedValue();
    }

    @Override
    public double getPowerConsumption() {
        double mPowerConsumption = myESHHandler.getMeasurementChannelValue();
        if (mPowerConsumption == Double.NEGATIVE_INFINITY) {
            mPowerConsumption = InternalDataModel.VAR_POWER_CONSUMPTION_DEFAULT;
        }
        // System.out.println("SmartHomeAgent-RealIOBehaviour-measurement:" + mPowerConsumption);
        return mPowerConsumption;
    }

    @Override
    public boolean getPoweredOn() {
        return myESHHandler.getActuateChannelValue();
    }

    @Override
    public void setPoweredOn(Boolean poweredOn) {
        myESHHandler.setActuateChannelValue(poweredOn, true); // 2nd parameter=calledFromOutside
    }

    @Override
    public void setUnlocked() {
        myESHHandler.setLockedNLoadedValue(false, true); // 2nd parameter=calledFromOutside
    }

    @Override
    public void updateEOMState() {
        String eomStateString = InternalDataModel.EOM_STATE_UNSET;
        if (getEOMState() != null) {
            eomStateString = getEOMState().getStateID();
        }
        myESHHandler.setDeviceState(eomStateString);

    }

    @Override
    public void setEOMState(TechnicalSystemStateEvaluation eomState) {
        this.eomState = eomState;
    }

    @Override
    public TechnicalSystemStateEvaluation getEOMState() {
        return eomState;
        }

    @Override
    public void onMonitoringEvent(MonitoringEvent monitoringEvent) {
        setEOMState(monitoringEvent.getTSSE());
    }
}