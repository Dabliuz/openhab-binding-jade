package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartHomeAgentHandler;
import energy.FixedVariableList;
import energy.optionModel.FixedDouble;
import hygrid.agent.EnergyAgentIO;
import jade.core.behaviours.CyclicBehaviour;

/**
 * This class is used if the current project setup is run on physical hardware
 * It takes real measurements of an energy conversion process.
 *
 */
public class IOReal extends CyclicBehaviour implements EnergyAgentIO {

    private static final long serialVersionUID = 5143063807591183507L;

    private SmartHomeAgent myAgent;

    private static final long MEASURING_INTERVAL = 1000;
    private FixedVariableList varList;

    private SmartHomeAgentHandler myAgentHandler;

    /**
     * Instantiates this behaviour.
     *
     * @param agent the agent
     * @param myAgentHandler
     */
    public IOReal(SmartHomeAgent agent, SmartHomeAgentHandler myAgentHandler) {
        super(agent);
        this.myAgent = agent;
        this.myAgentHandler = myAgentHandler;
    }

    /**
     * @return the current measurement
     */
    private FixedVariableList getCurrentMeasurement() {

        // access the actual IO interface
        double mVoltage = myAgentHandler.getCurrentMeasurement();

        // add measurement to the list
        FixedVariableList newMeasurements = new FixedVariableList();
        FixedDouble m1 = new FixedDouble();
        m1.setVariableID(InternalDataModel.VAR_VOLTAGE);
        m1.setValue(mVoltage);
        newMeasurements.add(m1);

        System.out.println("SmartHomeAgent-RealIOBehaviour-measurement from openHAB:" + mVoltage);

        return newMeasurements;
    }

    /*
     * @see jade.core.behaviours.Behaviour#action()
     */
    @Override
    public void action() {
        long startTime = System.currentTimeMillis();
        this.setMeasurementsFromSystem(this.getCurrentMeasurement());
        long waitTime = MEASURING_INTERVAL - (System.currentTimeMillis() - startTime);
        block(waitTime);
    }

    /*
     * @see hygrid.measurements.AgentIO#getTime()
     */
    @Override
    public Long getTime() {
        return System.currentTimeMillis();
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
