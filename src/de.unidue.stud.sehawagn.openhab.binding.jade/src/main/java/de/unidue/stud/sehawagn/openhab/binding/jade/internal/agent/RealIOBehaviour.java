package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartHomeAgentHandler;
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

    private SmartHomeAgentHandler myAgentHandler;
    private SmartHomeAgent myAgent;
    private FixedVariableList measurements;
    private FixedVariableList setPoints;

    /**
     * @param agent the agent
     * @param myAgentHandler
     */
    public RealIOBehaviour(SmartHomeAgent agent, SmartHomeAgentHandler myAgentHandler) {
        super(agent);
        this.myAgent = agent;
        this.myAgentHandler = myAgentHandler;
    }

    /**
     * @return the current measurement
     */
    private FixedVariableList measure() {

        // access the data in openHAB's AgentHandler
        double mPowerConsumption = myAgentHandler.getCurrentMeasurement();

        if (mPowerConsumption == Double.NEGATIVE_INFINITY) {
            mPowerConsumption = InternalDataModel.VAR_POWER_CONSUMPTION_DEFAULT;
        }

//        System.out.println("SmartHomeAgent-RealIOBehaviour-measurement:" + mPowerConsumption);

        // add measurement to the list
        FixedVariableList newMeasurements = new FixedVariableList();
        FixedDouble m1 = new FixedDouble();
        m1.setVariableID(InternalDataModel.VAR_POWER_CONSUMPTION);
        m1.setValue(mPowerConsumption);
        newMeasurements.add(m1);

        return newMeasurements;
    }

    private void actuate() {
        for (FixedVariable fixedVariable : setPoints) {
            switch (fixedVariable.getVariableID()) {
                case InternalDataModel.VAR_OCCUPIED: {
                    if (fixedVariable instanceof FixedBoolean) {
                        myAgentHandler.setOperationalState(((FixedBoolean) fixedVariable).isValue());
                    }
                }
            }
        }
    }

    private FixedVariableList initSetPoints() {
        FixedVariableList newSetPoints = new FixedVariableList();
        FixedBoolean sP1 = new FixedBoolean();
        sP1.setVariableID(InternalDataModel.VAR_OCCUPIED);
        sP1.setValue(InternalDataModel.VAR_OCCUPIED_DEFAULT);
        newSetPoints.add(sP1);

        return newSetPoints;
    }

    @Override
    public void action() {
        setMeasurementsFromSystem(getMeasurementsFromSystem()); // set measurements to agent
        block(MEASURING_INTERVAL);
    }

    @Override
    public FixedVariableList getMeasurementsFromSystem() {
        measurements = measure();
        return measurements;
    }

    @Override
    public void setMeasurementsFromSystem(FixedVariableList newMeasurements) {
        measurements = newMeasurements;
        this.myAgent.getInternalDataModel().setMeasurementsFromSystem(newMeasurements);
    }

    @Override
    public void setSetPointsToSystem(FixedVariableList newSetPoints) {
        setPoints = newSetPoints;
        actuate();
    }

    @Override
    public FixedVariableList getSetPointsToSystem() {
        if (setPoints == null) {
            setPoints = initSetPoints();
        }
        return setPoints;
    }

}