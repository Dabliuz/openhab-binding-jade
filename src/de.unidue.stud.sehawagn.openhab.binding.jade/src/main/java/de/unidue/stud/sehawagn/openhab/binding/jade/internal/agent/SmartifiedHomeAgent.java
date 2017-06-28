package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import de.unidue.stud.sehawagn.energy.Flexibility;
import de.unidue.stud.sehawagn.energy.Helper;
import de.unidue.stud.sehawagn.energy.RequestRunSchedule;
import de.unidue.stud.sehawagn.energy.ScheduleRequester;
import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartifiedHomeESHHandler;
import energy.optionModel.TechnicalSystem;
import hygrid.agent.AbstractEnergyAgent;
import hygrid.agent.AbstractIOReal;
import hygrid.agent.AbstractIOSimulated;
import hygrid.agent.SimulationConnectorRemote;
import hygrid.agent.monitoring.MonitoringBehaviourRT;
import hygrid.agent.monitoring.MonitoringListenerForLogging;
import hygrid.agent.monitoring.MonitoringListenerForProxy;
import hygrid.env.agentConfig.dataModel.AgentConfig;
import hygrid.env.agentConfig.dataModel.AgentOperatingMode;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

/**
 * An energy agent, representing a "smart home", using the configured EOM.
 */
public class SmartifiedHomeAgent extends AbstractEnergyAgent implements ScheduleRequester {

    private static final long serialVersionUID = 1730951019391324601L;

    protected InternalDataModel internalDataModel;

    protected MonitoringBehaviourRT monitoringBehaviourRT;
    protected SimulationConnectorRemote simulationConnector;

    protected AID coordinatorAgentAID;

    /*
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {
        Object[] args = getArguments();
        SmartifiedHomeESHHandler myESHHandler = null;
        if (args != null) { // if started by the simulation environment
            if (args.length >= 1 && args[0] instanceof AgentConfig) {
                getAgentConfigController().setAgentConfig((AgentConfig) args[0]);
            }
            if (args.length >= 2 && args[1] instanceof SmartifiedHomeESHHandler) {
                myESHHandler = (SmartifiedHomeESHHandler) args[1];
            }
        }

        Helper.enableForCommunication(this);

//        ContentManager contentManager = getContentManager();

        // prepare communication with other platforms
//        contentManager.registerLanguage(new SLCodec());
//        contentManager.registerOntology(HyGridOntology.getInstance());

        // If in testbed messages from the central agent are handled by a different behaviour
        AgentOperatingMode operatingMode = getAgentOperatingMode();
        if (operatingMode == AgentOperatingMode.TestBedSimulation || operatingMode == AgentOperatingMode.TestBedReal
                || operatingMode == AgentOperatingMode.RealSystem) {
            AID centralAgent = getAgentConfigController().getCentralAgentAID();
            messageTemplate = MessageTemplate.not(MessageTemplate.MatchSender(centralAgent));
        }

        // initialize SimulationConnector
        if (operatingMode == AgentOperatingMode.TestBedReal || operatingMode == AgentOperatingMode.RealSystem) {
            simulationConnector = new PerpetualSmartGridConnector(this);
        }

        getEnergyAgentIO().setESHHandler(myESHHandler);
        if (myESHHandler != null) { // don't do it in case of the simulation, where the OH/ESH classes are not available
            coordinatorAgentAID = myESHHandler.getBridgeHandler().getCoordinatorAgentAID();
        }
        Behaviour ioBehaviour = (Behaviour) getEnergyAgentIO();
        if (ioBehaviour != null) {
            addBehaviour(ioBehaviour);
        }

        addBehaviour(new OperationCommandReceiveBehaviour(this));

        System.out.println("SmartHomeAgent started");

        getEnergyAgentIO().onAgentStart();
    }

    /*
     * @see jade.core.Agent#takeDown()
     */
    @Override
    protected void takeDown() {
        if (getAgentOperatingMode() != null) {
            switch (getAgentOperatingMode()) {
                case Simulation:
                    if (agentIOBehaviour != null) {
                        ((SimulationIOBehaviour) agentIOBehaviour).stopTimeTriggerForSystemInput();
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
//        System.out.println("SmartHomeAgent stopped");
        getEnergyAgentIO().onAgentStop();
    }

    /**
     * @return the internal data model of this agent
     */
    @Override
    public InternalDataModel getInternalDataModel() {
        if (internalDataModel == null) {
            internalDataModel = new InternalDataModel(this);
            // necessary to initialize the datamodel's controlledSystemType
            internalDataModel.getOptionModelController();
            internalDataModel.addObserver(this);
        }
        return internalDataModel;
    }

    /**
     * Start real time control behaviour MonitoringBehaviourRT, if not already done.
     */
    @Override
    public MonitoringBehaviourRT getMonitoringBehaviourRT() {
        if (monitoringBehaviourRT == null) {
            monitoringBehaviourRT = new MonitoringBehaviourRT(getInternalDataModel(), getEnergyAgentIO());
            monitoringBehaviourRT.addMonitoringListener(new MonitoringListenerForLogging());
            monitoringBehaviourRT.addMonitoringListener(new MonitoringListenerForProxy(simulationConnector).overrideMeasurementVariable(InternalDataModel.VAR_POWER_CONSUMPTION));
            getInternalDataModel().addObserver(monitoringBehaviourRT);
            monitoringBehaviourRT.addMonitoringListener(getInternalDataModel());
            monitoringBehaviourRT.getMonitoringStrategyRT().setOptionModelCalculationClass(SmartifiedHomeCalculation.class);
        }
        return monitoringBehaviourRT;
    }

    @Override
    public AbstractIOSimulated getIOSimulated() {
        return new SimulationIOBehaviour(this, getInternalDataModel());
    }

    @Override
    public AbstractIOReal getIOReal() {
        return new ESHIOBehaviour(this);
    }

    @Override
    public WashingMachineIO getEnergyAgentIO() {
        return (WashingMachineIO) super.getEnergyAgentIO();
    }

    public void requestRunScheduleFromGrid(TechnicalSystem technicalSystem) {
        int ticIndex = getInternalDataModel().getOptionModelController().getIndexOfTechnicalInterfaceConfiguration(getInternalDataModel().getTechnicalSystemStateEvaluation().getConfigID());
        Flexibility flexibility = new Flexibility(technicalSystem);
        flexibility.selectInterface(ticIndex);
        flexibility.pruneForControllableStates(InternalDataModel.VAR_LOCKED_N_LOADED, InternalDataModel.VAR_POWERED_ON);
        ACLMessage requestMessage = RequestRunSchedule.prepareRequestMessage(this, flexibility);
        requestMessage.addReceiver(coordinatorAgentAID);
        addBehaviour(new RequestRunSchedule(this, requestMessage, this));
    }

    @Override
    public void processReceivedSchedule(Flexibility schedule) {
        int ticIndex = getInternalDataModel().getOptionModelController().getIndexOfTechnicalInterfaceConfiguration(getInternalDataModel().getTechnicalSystemStateEvaluation().getConfigID());

        schedule.applyTo(getInternalDataModel().getOptionModelController().getTechnicalSystem(), ticIndex);
        internalDataModel.waitForCoordination = false;
    }

    public void coordinateWithGrid() {
        TechnicalSystem technicalSystem = getInternalDataModel().getOptionModelController().getTechnicalSystem();
        requestRunScheduleFromGrid(technicalSystem);
//        TechnicalSystem technicalSystem=(TechnicalSystem) getInternalDataModel().getNetworkComponent().getDataModel();

        // this contains the graph as states and transitions, it should be cleaned from x/y attributes, unneeded states,
        // descriptions, semantic ids, (IO)VariableStates, FormatTimeUnits, detailed EnergyFlowInWatt (calculate
        // average) from measurements (turn EnergyFlowMeasured into EnergyFlowGeneral), optionally introduce
        // StatisticalEnergyFLow in the type of a Stock Chart/Kursdiagramm with Fehlerindikatoren (error bars, standard
        // deviation/variance)
//        getInternalDataModel().getOptionModelController().getTechnicalInterfaceConfiguration("Washing Program from openHAB handler");
//        technicalSystem.get

    }
}