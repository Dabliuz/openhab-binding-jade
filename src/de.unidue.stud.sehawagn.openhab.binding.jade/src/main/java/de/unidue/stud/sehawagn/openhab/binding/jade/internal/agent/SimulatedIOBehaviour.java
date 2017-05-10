package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import agentgui.envModel.graph.networkModel.NetworkModel;
import agentgui.simulationService.transaction.EnvironmentNotification;
import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartHomeAgentESHHandler;
import hygrid.agent.AbstractIOSimulated;
import hygrid.agent.AbstractInternalDataModel;

/**
 * The class IOSimulated is used if the current project setup is run as a simulation
 * It simulates measurements of an energy conversion process.
 *
 */
public class SimulatedIOBehaviour extends AbstractIOSimulated implements WashingMashineIO {

    private static final long serialVersionUID = -6149499361123282249L;

    public SimulatedIOBehaviour(SmartHomeAgent agent, AbstractInternalDataModel internalDataModel) {
        super(agent, internalDataModel);
    }

    /*
     * @see agentgui.simulationService.behaviour.SimulationServiceBehaviour#onEnvironmentNotification(agentgui.
     * simulationService.transaction.EnvironmentNotification)
     */
    @Override
    protected EnvironmentNotification onEnvironmentNotification(EnvironmentNotification notification) {
        return super.onEnvironmentNotification(notification);
    }

    /*
     * @see hygrid.agent.AbstractIOSimulated#commitMeasurementsToAgentsManually()
     */
    @Override
    protected boolean commitMeasurementsToAgentsManually() {
        return false;
    }

    /*
     * @see hygrid.agent.AbstractIOSimulated#prepareForSimulation(agentgui.envModel.graph.networkModel.NetworkModel)
     */
    @Override
    protected void prepareForSimulation(NetworkModel networkModel) {

    }

    @Override
    public double getPowerConsumption() {
        return 0;
    }

    @Override
    public boolean getPoweredOn() {
        return false;
    }

    @Override
    public void setPoweredOn(Boolean poweredOn) {
    }

    @Override
    public boolean getLockedNLoaded() {
        return false;
    }

    @Override
    public Integer getWashingProgram() {
        return null;
    }

    @Override
    public void onAgentStart() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setESHHandler(SmartHomeAgentESHHandler myAgentHandler) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAgentStop() {
        // TODO Auto-generated method stub

    }

}