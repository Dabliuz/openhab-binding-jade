package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import energy.FixedVariableList;
import energy.optionModel.FixedBoolean;
import energy.optionModel.FixedDouble;
import energy.optionModel.FixedInteger;
import energy.optionModel.FixedVariable;
import hygrid.agent.AbstractIOReal;
import hygrid.agent.EnergyAgentIO;

/**
 * This class is used if the agent is run inside an openHAB instance
 * It reads data from it's agent handler
 */
public class RealIOBehaviour extends AbstractIOReal implements EnergyAgentIO {

    private static final long serialVersionUID = 5143063807591183507L;

    private static final long MEASURING_INTERVAL = 1000;

    private SmartHomeAgent myAgent;
    private FixedVariableList measurements;
    private FixedVariableList setPoints = produceDefaultSetPointList();

    /**
     * @param agent the agent
     * @param myAgentHandler
     */
    public RealIOBehaviour(SmartHomeAgent agent) {
        super(agent);
        this.myAgent = agent;
    }

    @Override
    public void action() {
        syncWithSystem();

        updateInternalDataModel();
        block(MEASURING_INTERVAL);
    }

    private void syncWithSystem() {
        // access the data in openHAB's AgentHandler
        double mPowerConsumption = myAgent.getPowerConsumption();

        if (mPowerConsumption == Double.NEGATIVE_INFINITY) {
            mPowerConsumption = InternalDataModel.VAR_POWER_CONSUMPTION_DEFAULT;
        }
//        System.out.println("SmartHomeAgent-RealIOBehaviour-measurement:" + mPowerConsumption);
        measurements = produceVariableList(mPowerConsumption, InternalDataModel.VAR_POWER_CONSUMPTION);
        setPoints = produceVariableList(myAgent.getWashingProgram(), InternalDataModel.VAR_WASHING_PROGRAM);
        setPoints.add(produceVariable(myAgent.getLockedNLoaded(), InternalDataModel.VAR_LOCKED_N_LOADED));
        setPoints.add(produceVariable(myAgent.getPoweredOn(), InternalDataModel.VAR_POWERED_ON));
        myAgent.updateEOMState();
    }

    private void updateInternalDataModel() {
        // probably important because of observable pattern?
        myAgent.getInternalDataModel().setMeasurementsFromSystem(measurements);
        // myAgent.getInternalDataModel().setSetPointsToSystem(setPoints); //TODO why is this not designed?
    }

    // unused
    @Override
    public FixedVariableList getMeasurementsFromSystem() {
        return measurements;
    }

    // called by MonitoringBehaviour
    @Override
    public FixedVariableList getSetPointsToSystem() {
        return setPoints;
    }

    // unused
    @Override
    public void setMeasurementsFromSystem(FixedVariableList newMeasurements) {
        measurements = newMeasurements;
    }

    // called by ControlBehaviourRT
    @Override
    public void setSetPointsToSystem(FixedVariableList newSetPoints) {
        setPoints = newSetPoints;
        // TODO what would this be needed for?
//        myAgent.setLockedNLoaded(deriveVariable(setPoints, InternalDataModel.VAR_LOCKED_N_LOADED));
        myAgent.setPoweredOn(deriveVariable(setPoints, InternalDataModel.VAR_POWERED_ON));
        updateInternalDataModel();
    }

    public static FixedVariable produceVariable(Object newValue, String variableID) {
        FixedVariable var = null;
        if (newValue instanceof Boolean) {
            FixedBoolean var1 = new FixedBoolean();
            var1.setValue((Boolean) newValue);
            var = var1;
        } else if (newValue instanceof Double) {
            FixedDouble var2 = new FixedDouble();
            var2.setValue((Double) newValue);
            var = var2;
        } else if (newValue instanceof Integer) {
            FixedInteger var3 = new FixedInteger();
            var3.setValue((Integer) newValue);
            var = var3;
        } else {
            System.err.println("CONVERSION ERROR IN RealIOBehaviour");
        }
        if (var != null) {
            var.setVariableID(variableID);
        }
        return var;
    }

    public static FixedVariableList produceVariableList(Object newValue, String variableID) {
        FixedVariableList variableList = new FixedVariableList();
        variableList.add(produceVariable(newValue, variableID));
        return variableList;
    }

    public static FixedVariableList produceDefaultSetPointList() {
        FixedVariableList variableList = new FixedVariableList();
        variableList.add(produceVariable(InternalDataModel.SP_WASHING_PROGRAM_DEFAULT, InternalDataModel.VAR_WASHING_PROGRAM));
        variableList.add(produceVariable(InternalDataModel.SP_LOCKED_N_LOADED_DEFAULT, InternalDataModel.VAR_LOCKED_N_LOADED));
        variableList.add(produceVariable(InternalDataModel.SP_POWERED_ON_DEFAULT, InternalDataModel.VAR_POWERED_ON));
        return variableList;
    }

    public static boolean deriveVariable(FixedVariableList variableList, String variableID) {
        FixedVariable sP1 = variableList.getVariable(variableID);
        if (sP1 instanceof FixedBoolean) {
            return ((FixedBoolean) sP1).isValue();
        } else if (sP1 == null) {
            System.out.println("deriveVariable() " + variableID + " not found! :-(");
        }
        return false;
    }
}