package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import de.unidue.stud.sehawagn.openhab.binding.jade.handler.SmartifiedHomeESHHandler;
import hygrid.agent.AbstractEnergyAgent;
import hygrid.agent.AbstractIOReal;
import hygrid.agent.AbstractIOSimulated;
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
public class SmartifiedHomeAgent extends AbstractEnergyAgent {

    private static final long serialVersionUID = 1730951019391324601L;

    protected InternalDataModel internalDataModel;

    protected MonitoringBehaviourRT monitoringBehaviourRT;
    protected SimulationConnectorRemote simulationConnector;

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

        ContentManager contentManager = getContentManager();

        // prepare communication with other platforms
        contentManager.registerLanguage(new SLCodec());
        contentManager.registerOntology(HyGridOntology.getInstance());

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

        Behaviour ioBehaviour = (Behaviour) getEnergyAgentIO();
        if (ioBehaviour != null) {
            addBehaviour(ioBehaviour);
        }

        addBehaviour(new OperationCommandReceiveBehaviour(this));

//        System.out.println("SmartHomeAgent started");

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
    protected void startMonitoringBehaviourRT() {
        if (monitoringBehaviourRT == null) {
            monitoringBehaviourRT = new MonitoringBehaviourRT(getInternalDataModel(), getEnergyAgentIO());
            monitoringBehaviourRT.addMonitoringListener(new MonitoringListenerForLogging());
            monitoringBehaviourRT.addMonitoringListener(new MonitoringListenerForProxy(simulationConnector).overrideMeasurementVariable(InternalDataModel.VAR_POWER_CONSUMPTION));
            getInternalDataModel().addObserver(monitoringBehaviourRT);
            addBehaviour(monitoringBehaviourRT);
            monitoringBehaviourRT.getMonitoringStrategyRT().setOptionModelCalculationClass(SmartifiedHomeCalculation.class);
        }
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
}