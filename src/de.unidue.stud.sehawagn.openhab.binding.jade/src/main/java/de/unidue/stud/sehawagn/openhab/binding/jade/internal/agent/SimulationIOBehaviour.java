package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import agentgui.envModel.graph.networkModel.NetworkModel;
import agentgui.simulationService.transaction.EnvironmentNotification;
import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartifiedHomeESHHandler;
import energy.FixedVariableList;
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
	private Boolean poweredOn = true;
	private Integer washingProgram = 0;
	private Boolean lockedNLoaded = false;
	private Double powerConsumption = 0.0;

	protected SmartifiedHomeAgent myAgent;

	protected TechnicalSystemStateEvaluation eomState = null;

	public SimulationIOBehaviour(SmartifiedHomeAgent agent, AbstractInternalDataModel internalDataModel) {
		super(agent);
		myAgent = agent;
	}

	@Override
	public void action() {
		// access the data in openHAB's AgentHandler
		FixedVariableList measurements;
		measurements = InternalDataModel.produceVariableList(getPowerConsumption(), InternalDataModel.VAR_POWER_CONSUMPTION);
		setMeasurementsFromSystem(measurements);

		updateEOMState();

		myAgent.startMonitoring(myEnvironmentModel);

		// System.out.println(myEnvironmentModel);

		block(ESHIOBehaviour.MEASURING_INTERVAL);
	}

	// called by MonitoringBehaviour
	@Override
	public FixedVariableList getMeasurementsFromSystem() {
		return internalDataModel.getMeasurementsFromSystem();
	}

	// only used internally
	@Override
	public void setMeasurementsFromSystem(FixedVariableList newMeasurements) {
		// important because of observable pattern: every time the measurements are set, the ControlBehaviour is triggered
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
	}

	@Override
	public void setESHHandler(SmartifiedHomeESHHandler myAgentHandler) {
		System.err.println("SimulationIOBehaviour: Trying to set ESH handler " + myAgentHandler + " in simulation. Never mind.");
		// not necessary / possible in simulation
	}

	@Override
	public void onAgentStart() {
//		System.out.println("SimulationIOBehaviour: Agent started.");
	}

	@Override
	public void onAgentStop() {
//		System.out.println("SimulationIOBehaviour: Agent stopped.");
	}

	@Override
	public Integer getWashingProgram() {
//		System.out.println("SimulationIOBehaviour: washingProgram==" + washingProgram);
		return washingProgram;
	}

	@Override
	public boolean getLockedNLoaded() {
//		System.out.println("SimulationIOBehaviour: lockedNLoaded==" + lockedNLoaded);
		return lockedNLoaded;
	}

	@Override
	public double getPowerConsumption() {
//		System.out.println("SimulationIOBehaviour: powerConsumption==" + powerConsumption);
		return powerConsumption;
	}

	@Override
	public boolean getPoweredOn() {
//		System.out.println("SimulationIOBehaviour: poweredOn==" + poweredOn);
		return poweredOn;
	}

	@Override
	public void setPoweredOn(Boolean poweredOn) {
		this.poweredOn = poweredOn;
	}

	@Override
	public void setUnlocked() {
		lockedNLoaded = false;
	}

	@Override
	public void onMonitoringEvent(MonitoringEvent monitoringEvent) {
		setEOMState(monitoringEvent.getTSSE());
	}

	@Override
	public void updateEOMState() {
		String eomStateString = InternalDataModel.EOM_STATE_UNSET;
		if (getEOMState() != null) {
			eomStateString = getEOMState().getStateID();
		}
//		System.out.println("SimulationIOBehaviour: updateEOMState==" + eomStateString);
		if (eomStateString.equals("Off") || eomStateString.equals("Standby")) {
			nextSetting();
		}
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
	protected EnvironmentNotification onEnvironmentNotification(EnvironmentNotification notification) {
		return super.onEnvironmentNotification(notification);
	}

	@Override
	protected boolean commitMeasurementsToAgentsManually() {
		return false;
	}

	@Override
	protected void prepareForSimulation(NetworkModel networkModel) {
	}

	@Override
	public Long getTime() {
		return System.currentTimeMillis();
	}

	@Override
	public void onEnvironmentStimulus() {
		System.out.println("SimulationIOBehaviour: onEnvironmentStimulus");
		getEnergyAgent().addBehaviour(getEnergyAgent().getMonitoringBehaviourRT());
	}

	public void nextSetting() {
//		System.out.println("SimulationIOBehaviour: nextSetting()");

		if (poweredOn == true && washingProgram == 0 && lockedNLoaded == false) { // Off
			poweredOn = true;
			washingProgram = 4;
			lockedNLoaded = false; // Standby
		} else if (poweredOn == true && washingProgram == 4 && lockedNLoaded == false) {
			poweredOn = true;
			washingProgram = 4;
			lockedNLoaded = true; // Ready
		} else {
			System.err.println("SimulationIOBehaviour: nextSetting - not applicable");

			/*
			if (poweredOn == true && washingProgram == 4 && lockedNLoaded == true) { // Active
				poweredOn = false;
				washingProgram = 4;
				lockedNLoaded = true; // Waiting
			}
			*/
		}
	}

	@Override
	public double getEndTimeTolerance() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getEndTime() {
		// TODO Auto-generated method stub
		return 0;
	}

}