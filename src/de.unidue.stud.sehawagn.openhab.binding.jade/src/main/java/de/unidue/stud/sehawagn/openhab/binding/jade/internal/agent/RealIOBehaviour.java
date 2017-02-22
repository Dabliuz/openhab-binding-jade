package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartHomeAgentHandler;
import energy.FixedVariableList;
import energy.optionModel.FixedDouble;
import hygrid.agent.EnergyAgentIO;
import jade.core.behaviours.CyclicBehaviour;

/**
 * This class is used if the agent is run inside an openHAB instance
 * It reads data from it's agent handler
 */
public class RealIOBehaviour extends CyclicBehaviour implements EnergyAgentIO {

    private static final long serialVersionUID = 5143063807591183507L;

    private static final long MEASURING_INTERVAL = 1000;
    private long simulationTimeOffset = 0;

    private SmartHomeAgentHandler myAgentHandler;
    private SmartHomeAgent myAgent;
    private FixedVariableList varList;

    /**
     * Instantiates this behaviour.
     *
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
    private FixedVariableList getCurrentMeasurement() {

        // access the data in openHAB's AgentHandler
        double mVoltage = myAgentHandler.getCurrentMeasurement();
        System.out.println("SmartHomeAgent-RealIOBehaviour-measurement from openHAB:" + mVoltage);

        // add measurement to the list
        FixedVariableList newMeasurements = new FixedVariableList();
        FixedDouble m1 = new FixedDouble();
        m1.setVariableID(InternalDataModel.VAR_VOLTAGE);
        m1.setValue(mVoltage);
        newMeasurements.add(m1);

        return newMeasurements;
    }

    /*
     * @see jade.core.behaviours.Behaviour#action()
     */
    @Override
    public void action() {
        this.setMeasurementsFromSystem(this.getCurrentMeasurement()); // set measurements to agent
        block(MEASURING_INTERVAL);
    }

    /**
     * Sets the simulation start time when the simulation starts to set the correct time offset.
     *
     * @param simulationStartTime the simulation start time
     */
    public void setSimulationStartTime(long simulationStartTime) {
        this.simulationTimeOffset = System.currentTimeMillis() - simulationStartTime;
    }

    /*
     * @see hygrid.measurements.AgentIO#getTime()
     */
    @Override
    public Long getTime() {
        return System.currentTimeMillis() - this.simulationTimeOffset;
    }

    /*
     * @see hygrid.agent.internalDataModel.AgentIO#getInputMeasurements()
     */
    @Override
    public FixedVariableList getMeasurementsFromSystem() {
        if (varList == null) {
            varList = this.getCurrentMeasurement();
        }
        return varList;
    }

    /*
     * @see hygrid.agent.EnergyAgentIO#setMeasurementsFromSystem(hygrid.agent.FixedVariableList)
     */
    @Override
    public void setMeasurementsFromSystem(FixedVariableList newMeasurements) {
        varList = newMeasurements;
        this.myAgent.getInternalDataModel().setMeasurementsFromSystem(newMeasurements);
    }

    /*
     * @see
     * hygrid.agent.internalDataModel.AgentIO#setOutputMeasurements(smartHouse.agent.internalDataModel.Measurements)
     */
    @Override
    public void setSetPointsToSystem(FixedVariableList newOutputMeasurements) {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     *
     * @see hygrid.agent.EnergyAgentIO#getSetPointsToSystem()
     */
    @Override
    public FixedVariableList getSetPointsToSystem() {
        // TODO Auto-generated method stub
        return null;
    }
}
