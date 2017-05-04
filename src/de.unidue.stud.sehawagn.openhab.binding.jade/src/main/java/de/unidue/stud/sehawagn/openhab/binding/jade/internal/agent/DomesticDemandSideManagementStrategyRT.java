package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import java.util.List;
import java.util.Vector;

import energy.OptionModelController;
import energy.evaluation.AbstractEvaluationStrategyRT;
import energy.evaluation.TechnicalSystemStateDeltaEvaluation;
import energy.evaluation.TechnicalSystemStateDeltaHelper;
import energy.evaluation.TechnicalSystemStateDeltaHelper.DeltaSelectionBy;
import energy.optionModel.FixedBoolean;
import energy.optionModel.FixedVariable;
import energy.optionModel.TechnicalSystemStateEvaluation;

public class DomesticDemandSideManagementStrategyRT extends AbstractEvaluationStrategyRT {

    public DomesticDemandSideManagementStrategyRT(OptionModelController optionModelController) {
        super(optionModelController);
    }

    @Override
    public InitialStateAdaption getInitialStateAdaption() {
        return InitialStateAdaption.TemporalMoveToStateDurationsEnd;
    }

    @Override
    public void runEvaluation() {
//        System.out.println("DomesticDemandSideManagementStrategyRT - runEvaluation");

        // Initialize search
        TechnicalSystemStateEvaluation tsse = this.getInitialTechnicalSystemStateEvaluation();

        // Search by walking through time
        while (tsse.getGlobalTime() < this.timeUntilEvaluationInterrupt) {

            // Get the possible subsequent steps and states
            long duration = this.timeUntilEvaluationInterrupt - tsse.getGlobalTime();
            Vector<TechnicalSystemStateDeltaEvaluation> deltaSteps = this.getAllDeltaEvaluationsStartingFromTechnicalSystemState(tsse, duration);
            if (deltaSteps.isEmpty()) {
                System.err.println("DomesticDSMStategy ERROR: No further delta steps possible => interrupt search!");
                this.setTechnicalSystemStateEvaluation(null);
                break;
            }

            // search for a state which could be transitioned into by switch the poweredOn
            FixedBoolean intendedPoweredOn = (FixedBoolean) extractVariableByID(tsse.getIOlist(), InternalDataModel.VAR_POWERED_ON);
            intendedPoweredOn.setValue(!intendedPoweredOn.isValue()); // invert poweredOn setting
            deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps, DeltaSelectionBy.IO_List, InternalDataModel.VAR_POWERED_ON, intendedPoweredOn);
            deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps, DeltaSelectionBy.IO_List, InternalDataModel.VAR_LOCKED_N_LOADED, extractVariableByID(tsse.getIOlist(), InternalDataModel.VAR_LOCKED_N_LOADED));
            deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps, DeltaSelectionBy.IO_List, InternalDataModel.VAR_WASHING_PROGRAM, extractVariableByID(tsse.getIOlist(), InternalDataModel.VAR_WASHING_PROGRAM));

            TechnicalSystemStateDeltaEvaluation tssDeltaDecision = null;

            if (deltaSteps == null || deltaSteps.isEmpty()) {
                System.err.println("DomesticDSMStategy ERROR: No delta steps left after filtering for setpoints => interrupt search!");
                this.setTechnicalSystemStateEvaluation(null);
                break;
            } else if (deltaSteps.size() == 1) {// it should be only one left
                System.out.println("DomesticDSMStategy: Found a single new delta step.");
                tssDeltaDecision = deltaSteps.get(0);
            } else {
                System.err.println("DomesticDSMStategy ERROR: Too many (" + deltaSteps.size() + ") delta steps left after filtering for setpoints => interrupt search!");
                this.setTechnicalSystemStateEvaluation(null);
                break;
            }

            if (tssDeltaDecision == null) {
                System.err.println("DomesticDSMStategy ERROR: No valid subsequent state found!");
                this.setTechnicalSystemStateEvaluation(null);
                break;
            }

            // Set new current TechnicalSystemStateEvaluation
            TechnicalSystemStateEvaluation tsseNext = this.getNextTechnicalSystemStateEvaluation(tsse, tssDeltaDecision);
            if (tsseNext == null) {
                System.err.println("DomesticDSMStategy ERROR: Error while using selected delta => interrupt search!");
                this.setTechnicalSystemStateEvaluation(null);
                break;
            } else {
                // Set next state as new current state
                tsse = tsseNext;
            }
            if (isStopEvaluation() == true) {
                break; // if interrupted from outside
            }

            System.out.println("DomesticDSMStategy: setTechnicalSystemStateEvaluation(tsse)");
            this.setTechnicalSystemStateEvaluation(tsse); // TODO work on this, because
                                                          // getTechnicalSystemStateEvaluation() determines a TSSE
                                                          // itself if it is null
//            this.setIntermediateStateToResult(tsse);
        } // end while

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
