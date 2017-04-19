package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import energy.FixedVariableList;
import energy.optionModel.FixedBoolean;
import energy.optionModel.FixedDouble;
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
    private FixedVariableList setPoints = produceVariableList(InternalDataModel.SP_POWERED_ON_DEFAULT, InternalDataModel.VAR_POWERED_ON);

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
        setPoints = produceVariableList(myAgent.getPoweredOn(), InternalDataModel.VAR_POWERED_ON);
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
        myAgent.setPoweredOn(deriveSetPointPoweredOn(setPoints));
        updateInternalDataModel();
    }

    public static FixedVariableList produceVariableList(Object newValue, String variableID) {
        FixedVariableList variableList = new FixedVariableList();
        if (newValue instanceof Boolean) {
            FixedBoolean var1 = new FixedBoolean();
            var1.setVariableID(variableID);
            var1.setValue((Boolean) newValue);
            variableList.add(var1);
        } else if (newValue instanceof Double) {
            FixedDouble var2 = new FixedDouble();
            var2.setVariableID(variableID);
            var2.setValue((Double) newValue);
            variableList.add(var2);
        } else {
            System.err.println("CONVERSION ERROR IN RealIOBehaviour");
        }
        return variableList;
    }

    public static boolean deriveSetPointPoweredOn(FixedVariableList setPoints) {
        FixedVariable sP1 = setPoints.getVariable(InternalDataModel.VAR_POWERED_ON);
        if (sP1 instanceof FixedBoolean) {
            return ((FixedBoolean) sP1).isValue();
        }
        return false;
    }
}