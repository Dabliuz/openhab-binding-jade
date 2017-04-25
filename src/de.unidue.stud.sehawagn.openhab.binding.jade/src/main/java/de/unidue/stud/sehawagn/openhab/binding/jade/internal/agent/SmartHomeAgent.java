package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartHomeAgentHandler;
import hygrid.agent.AbstractEnergyAgent;
import hygrid.agent.AbstractIOReal;
import hygrid.agent.AbstractIOSimulated;
import hygrid.agent.EnergyAgentIO;
import hygrid.agent.SimulationConnectorRemote;
import hygrid.agent.monitoring.MonitoringBehaviourRT;
import hygrid.agent.monitoring.MonitoringListenerForLogging;
import hygrid.agent.monitoring.MonitoringListenerForProxy;
import hygrid.env.agentConfig.dataModel.AgentConfig;
import hygrid.env.agentConfig.dataModel.AgentOperatingMode;
import hygrid.ontology.HyGridOntology;
import jade.content.ContentManager;
import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.MessageTemplate;

/**
 * An energy agent, representing a "smart home", using the configured EOM.
 */
public class SmartHomeAgent extends AbstractEnergyAgent {

    private static final long serialVersionUID = 1730951019391324601L;

    private InternalDataModel internalDataModel;

    private MonitoringBehaviourRT monitoringBehaviourRT;
    private SimulationConnectorRemote simulationConnector;

    private EnergyAgentIO agentIOBehaviour;

    private SmartHomeAgentHandler myAgentHandler;

    /*
     * @see jade.core.Agent#setup()
     */
    @Override
    protected void setup() {
        Object[] args = this.getArguments();
        if (args != null) { // if started by the simulation environment
            if (args.length >= 1 && args[0] instanceof AgentConfig) {
                getAgentConfigController().setAgentConfig((AgentConfig) args[0]);
            }
            if (args.length >= 2 && args[1] instanceof SmartHomeAgentHandler) {
                myAgentHandler = (SmartHomeAgentHandler) args[1];
            }
        }

        ContentManager contentManager = this.getContentManager();

        // prepare communication with other platforms
        contentManager.registerLanguage(new SLCodec());
        contentManager.registerOntology(HyGridOntology.getInstance());

        // If in testbed messages from the central agent are handled by a different behaviour
        AgentOperatingMode operatingMode = this.getAgentOperatingMode();
        if (operatingMode == AgentOperatingMode.TestBedSimulation || operatingMode == AgentOperatingMode.TestBedReal
                || operatingMode == AgentOperatingMode.RealSystem) {
            AID centralAgent = this.getAgentConfigController().getCentralAgentAID();
            this.messageTemplate = MessageTemplate.not(MessageTemplate.MatchSender(centralAgent));
        }

        // initialize SimulationConnector
        if (operatingMode == AgentOperatingMode.TestBedReal || operatingMode == AgentOperatingMode.RealSystem) {
            this.simulationConnector = new SafeSimulationConnectorRemoteForIOReal(this);
        }

        Behaviour ioBehaviour = (Behaviour) this.getEnergyAgentIO();
        if (ioBehaviour != null) {
            this.addBehaviour(ioBehaviour);
        }

        this.addBehaviour(new OperationCommandReceiveBehaviour(this));

        System.out.println("SmartHomeAgent started");
        if (myAgentHandler != null) {
            myAgentHandler.onAgentStart();
        }
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
        System.out.println("SmartHomeAgent stopped");
        if (myAgentHandler != null) {
            myAgentHandler.onAgentStop();
        }
    }

    /**
     * @return the internal data model of this agent
     */
    @Override
    public InternalDataModel getInternalDataModel() {
        if (this.internalDataModel == null) {
            this.internalDataModel = new InternalDataModel(this);
            // necessary to initialize the datamodel's controlledSystemType
            this.internalDataModel.getOptionModelController();
            this.internalDataModel.addObserver(this);
        }
        return this.internalDataModel;
    }

    /**
     * Start real time control behaviour MonitoringBehaviourRT, if not already done.
     */
    @Override
    protected void startMonitoringBehaviourRT() {
        if (monitoringBehaviourRT == null) {
            monitoringBehaviourRT = new MonitoringBehaviourRT(this.getInternalDataModel(), this.getEnergyAgentIO());
            monitoringBehaviourRT.addMonitoringListener(new MonitoringListenerForLogging());
            monitoringBehaviourRT.addMonitoringListener(new MonitoringListenerForProxy(this.simulationConnector));
            this.getInternalDataModel().addObserver(monitoringBehaviourRT);
            this.addBehaviour(monitoringBehaviourRT);
            monitoringBehaviourRT.getMonitoringStrategyRT().setOptionModelCalculationClass(SmartHomeCalculation.class);
        }
    }

    @Override
    public AbstractIOSimulated getIOSimulated() {
        return new SimulatedIOBehaviour(this, this.getInternalDataModel());
    }

    @Override
    public AbstractIOReal getIOReal() {
        return new RealIOBehaviour(this);
    }

    // called by IO
    public double getPowerConsumption() {
        return myAgentHandler.getMeasurementChannelValue();
    }

    // called by IO, OperationCommand
    public boolean getPoweredOn() {
        return myAgentHandler.getActuateChannelValue();
    }

    // called by IO, OperationCommand
    public void setPoweredOn(Boolean poweredOn) {
        myAgentHandler.setActuateChannelValue(poweredOn);
    }

    // called by IO, OperationCommand
    public boolean getLockedNLoaded() {
        return myAgentHandler.getLockedNLoadedValue();
    }

}
