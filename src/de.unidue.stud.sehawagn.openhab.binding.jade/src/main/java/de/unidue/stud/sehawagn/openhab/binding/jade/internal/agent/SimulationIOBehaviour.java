package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import agentgui.envModel.graph.networkModel.NetworkModel;
import agentgui.simulationService.transaction.EnvironmentNotification;
import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartifiedHomeESHHandler;
import energy.optionModel.TechnicalSystemStateEvaluation;
import hygrid.agent.AbstractIOSimulated;
import hygrid.agent.AbstractInternalDataModel;
import hygrid.agent.monitoring.MonitoringEvent;

/**
 * The class IOSimulated is used if the current project setup is run as a simulation
 * It simulates measurements of an energy conversion process.
 *
 */
public class SimulationIOBehaviour extends AbstractIOSimulated implements WashingMachineIO {

    private static final long serialVersionUID = -6149499361123282249L;

    public SimulationIOBehaviour(SmartifiedHomeAgent agent, AbstractInternalDataModel internalDataModel) {
        super(agent);
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
    public void setESHHandler(SmartifiedHomeESHHandler myAgentHandler) {
        // not necessary / possible in simulation
    }

    @Override
    public void onAgentStart() {
    }

    @Override
    public void onAgentStop() {
    }

    @Override
    public Integer getWashingProgram() {
        return 0;
    }

    @Override
    public boolean getLockedNLoaded() {
        return false;
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
    public void setUnlocked() {
    }

    @Override
    public void onMonitoringEvent(MonitoringEvent monitoringEvent) {
    }

    @Override
    public void updateEOMState() {

    }

    @Override
    public void setEOMState(TechnicalSystemStateEvaluation eomState) {
    }

    @Override
    public TechnicalSystemStateEvaluation getEOMState() {
        return null;
    }
}