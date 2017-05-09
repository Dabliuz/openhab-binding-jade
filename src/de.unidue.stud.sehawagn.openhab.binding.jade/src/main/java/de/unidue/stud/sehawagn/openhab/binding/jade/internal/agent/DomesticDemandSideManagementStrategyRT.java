package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import java.util.List;
import java.util.Vector;

import energy.OptionModelController;
import energy.evaluation.AbstractEvaluationStrategyRT;
import energy.evaluation.TechnicalSystemStateDeltaEvaluation;
import energy.evaluation.TechnicalSystemStateDeltaHelper;
import energy.evaluation.TechnicalSystemStateDeltaHelper.DeltaSelectionBy;
import energy.optionModel.FixedBoolean;
import energy.optionModel.FixedInteger;
import energy.optionModel.FixedVariable;
import energy.optionModel.TechnicalSystemStateEvaluation;
import hygrid.agent.AbstractEnergyAgent;
import hygrid.agent.AbstractInternalDataModel;

public class DomesticDemandSideManagementStrategyRT extends AbstractEvaluationStrategyRT {

    protected boolean noSwitchingNecessary = false;
    protected AbstractInternalDataModel agentDataModel;
    private TechnicalSystemStateEvaluation nextTSSE = null;

    public DomesticDemandSideManagementStrategyRT(OptionModelController optionModelController) {
        super(optionModelController);
        if (optionModelController.getControllingAgent() instanceof AbstractEnergyAgent) {
            AbstractEnergyAgent energyAgent = (AbstractEnergyAgent) optionModelController.getControllingAgent();
            agentDataModel = energyAgent.getInternalDataModel();
        }
    }

    @Override
    public InitialStateAdaption getInitialStateAdaption() {
        return InitialStateAdaption.TemporalMoveToStateDurationsEnd;
    }

    @Override
    public void runEvaluation() {
//        System.out.println("DomesticDemandSideManagementStrategyRT - runEvaluation");

        // Initialize search
        TechnicalSystemStateEvaluation myTSSE = determineTSSE();

        // Search by walking through time
        while (myTSSE != null && myTSSE.getGlobalTime() < timeUntilEvaluationInterrupt) {
            nextTSSE = null;

            if (isStopEvaluation() == true) {
                dontSwitch();
                break; // if interrupted from outside
            }

            // Get the possible subsequent steps and states
            long duration = timeUntilEvaluationInterrupt - myTSSE.getGlobalTime();
            Vector<TechnicalSystemStateDeltaEvaluation> deltaSteps = getAllDeltaEvaluationsStartingFromTechnicalSystemState(myTSSE, duration);
            if (deltaSteps.isEmpty()) {
                System.err.println("DomesticDSMStrategy ERROR: No further delta steps possible => interrupt search!");
                dontSwitch();
                break;
            }
//            dumpDeltaStepsInfo(deltaSteps, "ALL");
            deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps, DeltaSelectionBy.IO_List, InternalDataModel.VAR_LOCKED_N_LOADED, ((FixedBoolean) extractVariableByID(myTSSE.getIOlist(), InternalDataModel.VAR_LOCKED_N_LOADED)).isValue());
//            dumpDeltaStepsInfo(deltaSteps, InternalDataModel.VAR_LOCKED_N_LOADED);
            deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps, DeltaSelectionBy.IO_List, InternalDataModel.VAR_WASHING_PROGRAM, ((FixedInteger) extractVariableByID(myTSSE.getIOlist(), InternalDataModel.VAR_WASHING_PROGRAM)).getValue());
//            dumpDeltaStepsInfo(deltaSteps, InternalDataModel.VAR_WASHING_PROGRAM);

            // search for a state which could be transitioned into by switch the poweredOn
            Boolean intendedPoweredOn = ((FixedBoolean) extractVariableByID(myTSSE.getIOlist(), InternalDataModel.VAR_POWERED_ON)).isValue();
//            System.out.println("current poweredOn=" + intendedPoweredOn);
            intendedPoweredOn = !intendedPoweredOn; // invert poweredOn setting
//            System.out.println("intendedPoweredOn=" + intendedPoweredOn);

            deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps, DeltaSelectionBy.IO_List, InternalDataModel.VAR_POWERED_ON, intendedPoweredOn);
//            dumpDeltaStepsInfo(deltaSteps, InternalDataModel.VAR_POWERED_ON);

            TechnicalSystemStateDeltaEvaluation tssDeltaDecision = null;

            if (deltaSteps == null || deltaSteps.isEmpty()) {
                System.err.println("DomesticDSMStrategy ERROR: No delta steps left after filtering for setpoints");
                dontSwitch();
                break;
            } else if (deltaSteps.size() == 1) {// it should be only one left
                tssDeltaDecision = deltaSteps.get(0);
                System.out.println("DomesticDSMStrategy: Found a single new delta step: " + tssDeltaDecision.getTechnicalSystemStateEvaluation().getStateID());
                noSwitchingNecessary = false;
            } else {
                System.err.println("DomesticDSMStrategy ERROR: Too many (" + deltaSteps.size() + ") delta steps left after filtering for setpoints");
                dontSwitch();
                break;
            }

            nextTSSE = this.getNextTechnicalSystemStateEvaluation(myTSSE, tssDeltaDecision);
            if (nextTSSE == null) {
                System.err.println("DomesticDSMStrategy ERROR: Error while using selected delta");
                dontSwitch();
                break;
            } else {
                // Set next state as new current state
                myTSSE = nextTSSE;
            }

            System.out.println("DomesticDSMStrategy: setTechnicalSystemStateEvaluation(" + myTSSE.getStateID() + ")");
            setTechnicalSystemStateEvaluation(myTSSE); // TODO work on this, because
                                                       // getTechnicalSystemStateEvaluation() determines a TSSE
                                                       // itself if it is null
            noSwitchingNecessary = false;
            break;
//            this.setIntermediateStateToResult(tsse);
        } // end while

    }

//    private static void dumpDeltaStepsInfo(Vector<TechnicalSystemStateDeltaEvaluation> deltaSteps, String occasion) {
//        System.out.print("Delta steps available (" + occasion + "): ");
//        if (deltaSteps != null && !deltaSteps.isEmpty()) {
//            for (TechnicalSystemStateDeltaEvaluation tssde : deltaSteps) {
//                System.out.print(tssde.getTechnicalSystemStateEvaluation().getStateID() + ", ");
//            }
//        }
//        System.out.println();
//    }

    void dontSwitch() {
        if (noSwitchingNecessary == false) {
            System.err.println("DomesticDSMStrategy: dontSwitch() (wasn't before)");
        }
        noSwitchingNecessary = true;
//        this.setTechnicalSystemStateEvaluation(null);
    }

    private TechnicalSystemStateEvaluation determineTSSE() {
        TechnicalSystemStateEvaluation myTSSE = null;
        if (agentDataModel != null) {
            myTSSE = agentDataModel.getTechnicalSystemStateEvaluation();
        }
        if (myTSSE == null) {
            System.err.println("DomesticDSMStrategy: agentDataModel not valid, super-getting TSSE");
            myTSSE = super.getTechnicalSystemStateEvaluation();
        }
        if (myTSSE == null) {
            System.err.println("DomesticDSMStrategy: returning initial TSSE");
            myTSSE = getInitialTechnicalSystemStateEvaluation();
        }
        return myTSSE;
    }

    @Override
    public TechnicalSystemStateEvaluation getTechnicalSystemStateEvaluation() {
        TechnicalSystemStateEvaluation myTSSE = nextTSSE;
        if (myTSSE == null) {
            myTSSE = determineTSSE();
        }
        String stateName = "null";
        if (myTSSE != null) {
            stateName = myTSSE.getStateID();
        }
        System.out.println("DomesticDSMStrategy state=" + stateName);

        if (noSwitchingNecessary) {
            System.err.println("DomesticDSMStrategy: noSwitchingNecessary, returning null as TSSE (so the behaviour doesn't set the setpoints)");
//            System.err.println("DomesticDSMStrategy: noSwitchingNecessary");
            myTSSE = null;
        }

        return myTSSE;
    }

    @Override
    protected boolean isAvailableOptionModelCalculation() {
        return true; // always available, since not instantiated via classloader
    }

    /**
     * Gets a {@link FixedVariable} with a specified ID from a list of {@link FixedVariable}s.
     *
     * @param variablesList The variables list
     * @param variableID The variable ID
     * @return the variable, null if not found
     */
    private static FixedVariable extractVariableByID(List<FixedVariable> variablesList, String variableID) {
        for (FixedVariable fv : variablesList) {
            if (fv.getVariableID().equals(variableID)) {
                return fv;
            }
        }
        return null;
    }

}
