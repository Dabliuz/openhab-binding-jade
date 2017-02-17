package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import java.util.Observable;
import java.util.Observer;

import agentgui.simulationService.SimulationService;
import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartHomeAgentHandler;
import energy.optionModel.ScheduleList;
import energy.optionModel.TechnicalSystem;
import energy.optionModel.TechnicalSystemGroup;
import hygrid.agent.AbstractInternalDataModel;
import hygrid.agent.ControlBehaviourRT;
import hygrid.agent.EnergyAgentIO;
import hygrid.env.agentConfig.controller.AgentConfigController;
import hygrid.env.agentConfig.dataModel.AgentConfig;
import hygrid.env.agentConfig.dataModel.AgentOperatingMode;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

/**
 * An energy agent, representing a "smart home", using the configured EOM.
 */
public class SmartHomeAgent extends Agent implements Observer {

	private static final long serialVersionUID = 1730951019391324601L;
	private AgentOperatingMode operatingMode;
	private AgentConfigController agentConfigController;

	private EnergyAgentIO agentIOBehaviour;
	private InternalDataModel internalDataModel;
	private ControlBehaviourRT controlBehaviourRT;
	private SmartHomeAgentHandler myAgentHandler;

	/*
	 * @see jade.core.Agent#setup()
	 */
	@Override
	protected void setup() {
		Object[] args = this.getArguments();
		if (args.length >= 1 && args[0] instanceof AgentConfig) {
			getAgentConfigController().setAgentConfig((AgentConfig) args[0]);
		}
		if (args.length >= 2 && args[1] instanceof SmartHomeAgentHandler) {
			myAgentHandler = (SmartHomeAgentHandler) args[1];
		}

		Behaviour ioBehaviour = (Behaviour) this.getEnergyAgentIO();
		if (ioBehaviour != null) {
			this.addBehaviour(ioBehaviour); // the needed IO behaviour
		}
		this.addBehaviour(new MessageReceiveBehaviour()); // additional behaviours
		System.out.println("SmartHomeAgent started");
	}

	/*
	 * @see jade.core.Agent#takeDown()
	 */
	@Override
	protected void takeDown() {
		if (getAgentOperatingMode() != null) {
			switch (this.getAgentOperatingMode()) {
			case Simulation:
				if (this.agentIOBehaviour != null) {
					((SimulatedIOBehaviour) this.agentIOBehaviour).stopTimeTriggerForSystemInput();
				}
				break;

			case TestBedSimulation:
				break;

			case TestBedReal:
				break;

			case RealSystem:
				break;
			default:
				break;
			}
		}

	}

	/**
	 * @return the agent config controller
	 */
	public AgentConfigController getAgentConfigController() {
		if (agentConfigController == null) {
			agentConfigController = new AgentConfigController();
		}
		return agentConfigController;
	}

	/**
	 * @return the current {@link AgentOperatingMode}.
	 */
	public AgentOperatingMode getAgentOperatingMode() {
		if (operatingMode == null) {
			if (this.isSimulation() == true) {
				operatingMode = AgentOperatingMode.Simulation;
			} else {
				// --- Possible other cases ---------------
				// operatingMode = AgentOperatingMode.TestBedSimulation;
				// operatingMode = AgentOperatingMode.TestBedReal;
				// operatingMode = AgentOperatingMode.RealSystem;

				AgentConfig agentConfig = this.getAgentConfigController().getAgentConfig();
				operatingMode = agentConfig.getAgentOperatingMode();
			}
		}
		return operatingMode;
	}

	/**
	 * @return true, if the execution is simulation
	 */
	private boolean isSimulation() {
		try {
			this.getHelper(SimulationService.NAME);
			return true;
		} catch (ServiceException se) {
			// mute exception because it only determines, that no simulation is running
		}
		return false;
	}

	/**
	 * @return the internal data model of this agent
	 */
	public InternalDataModel getInternalDataModel() {
		if (this.internalDataModel == null) {
			this.internalDataModel = new InternalDataModel(this);
			this.internalDataModel.addObserver(this);
		}
		return this.internalDataModel;
	}

	/**
	 * @return the current IO behaviour
	 */
	public EnergyAgentIO getEnergyAgentIO() {
		if (agentIOBehaviour == null) {
			if (getAgentOperatingMode() != null) {

				switch (this.getAgentOperatingMode()) {
				case Simulation: {
					agentIOBehaviour = new SimulatedIOBehaviour(this, this.getInternalDataModel());
					break;
				}
				case TestBedSimulation: {
					agentIOBehaviour = new SimulatedIOBehaviour(this, this.getInternalDataModel());
					break;
				}
				case TestBedReal: {
					// TODO
					break;
				}
				case RealSystem: {
					agentIOBehaviour = new IOReal(this, myAgentHandler);
					break;
				}
				default: {
					break;
				}
				}
			}
		}
		return agentIOBehaviour;
	}

	/**
	 * Start real time control behaviour ControlBehaviourRT, if not already done.
	 */
	private void startControlBehaviourRT() {
		if (controlBehaviourRT == null) {
			controlBehaviourRT = new ControlBehaviourRT(this.getInternalDataModel(), this.getEnergyAgentIO());
			this.addBehaviour(controlBehaviourRT);
		}
	}

	/*
	 * Will be invoked if the internal data model has changed
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(Observable observable, Object updateObject) {
		if (observable instanceof InternalDataModel) {
			if (updateObject == AbstractInternalDataModel.CHANGED.NetworModel) {

			} else if (updateObject == AbstractInternalDataModel.CHANGED.NetworkComponent) {
				// Get the actual data model of the NetworkComponent
				Object dm = this.getInternalDataModel().getNetworkComponent().getDataModel();
				if (dm instanceof ScheduleList) {
					this.internalDataModel.getScheduleController().setScheduleList((ScheduleList) dm);

				} else if (dm instanceof TechnicalSystem) {
					this.internalDataModel.getOptionModelController().setTechnicalSystem((TechnicalSystem) dm);
					if (this.internalDataModel.getOptionModelController().getEvaluationStrategyRT() != null) {
						this.startControlBehaviourRT(); // Add real time control, if configured

					}

				} else if (dm instanceof TechnicalSystemGroup) {
					this.internalDataModel.getGroupController().setTechnicalSystemGroup((TechnicalSystemGroup) dm);
					if (this.internalDataModel.getOptionModelController().getEvaluationStrategyRT() != null) {
						this.startControlBehaviourRT(); // add real time control, if configured
					}

				}

			} else if (updateObject == AbstractInternalDataModel.CHANGED.MeasurementsFromSystem) {
			}
		}
	}

	/**
	 * Internal class for message handling
	 */
	private class MessageReceiveBehaviour extends CyclicBehaviour {

		private static final long serialVersionUID = -6383794735800175272L;

		@Override
		public void action() {
			ACLMessage msg = this.myAgent.receive();
			if (msg != null) {
				// work on the message

			} else {
				block(); // until next message received
			}
		}
	}
}
