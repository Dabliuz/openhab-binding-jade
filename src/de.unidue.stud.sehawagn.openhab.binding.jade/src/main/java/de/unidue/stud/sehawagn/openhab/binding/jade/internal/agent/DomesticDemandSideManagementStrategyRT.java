package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import java.util.Vector;

import energy.OptionModelController;
import energy.evaluation.AbstractEvaluationStrategyRT;
import energy.evaluation.TechnicalSystemStateDeltaEvaluation;
import energy.evaluation.TechnicalSystemStateDeltaHelper;
import energy.evaluation.TechnicalSystemStateDeltaHelper.DeltaSelectionBy;
import energy.optionModel.FixedBoolean;
import energy.optionModel.FixedInteger;
import energy.optionModel.TechnicalSystemStateEvaluation;
import hygrid.agent.AbstractEnergyAgent;

public class DomesticDemandSideManagementStrategyRT extends AbstractEvaluationStrategyRT {

    protected boolean switchingNecessary = false;
    protected InternalDataModel agentDataModel = null;
    private TechnicalSystemStateEvaluation nextTSSE = null;

    public DomesticDemandSideManagementStrategyRT(OptionModelController optionModelController) {
        super(optionModelController);
        if (optionModelController.getControllingAgent() instanceof AbstractEnergyAgent) {
            AbstractEnergyAgent energyAgent = (AbstractEnergyAgent) optionModelController.getControllingAgent();
            agentDataModel = (InternalDataModel) energyAgent.getInternalDataModel();
        }
    }

    @Override
    protected boolean isAvailableOptionModelCalculation() {
        return true; // always available, since not instantiated via classloader
    }

    @Override
    public InitialStateAdaption getInitialStateAdaption() {
        return InitialStateAdaption.TemporalMoveToStateDurationsEnd;
    }

    @Override
    public void runEvaluation() {

        // String echoString = "DomesticDSMStrategy: runEvaluation(), ";
        if (isStopEvaluation() == true) {
            return; // if interrupted from outside (by simulation UI)
        }

        switchingNecessary = false;

        if (agentDataModel.waitForCoordination) {
            System.err.println("waitingForCoordination");
            return;
        }
        // Initialize search
        TechnicalSystemStateEvaluation tsse = getSystemState();

        long remainingTimeInState = this.calculateEvaluationPause(tsse, evaluationStepEndTime);
        if (remainingTimeInState > 0) {
            // too early, state still valid, no need for new evaluation step
            // DateFormat f = new SimpleDateFormat("HH:mm:ss.SSS");
            // System.out.println("Remaining time in state: " + f.format(new Date(remainingTimeInState)));
            return;
        }

        if (tsse != null && tsse.getGlobalTime() < evaluationStepEndTime) {
            // echoString += "state=" + tsse.getStateID() + ", evaluating";

            // Get the possible subsequent steps and states
            long duration = evaluationStepEndTime - tsse.getGlobalTime();
            Vector<TechnicalSystemStateDeltaEvaluation> deltaSteps = getAllDeltaEvaluationsStartingFromTechnicalSystemState(
                    tsse, duration);

            Vector<TechnicalSystemStateDeltaEvaluation> deltaStepsFiltered = filterForPowerSwitching(deltaSteps, tsse);

            if (deltaStepsFiltered == null || deltaStepsFiltered.isEmpty()) {
                deltaStepsFiltered = filterForUnlocking(deltaSteps, tsse);
            }
            deltaSteps = deltaStepsFiltered;

            TechnicalSystemStateDeltaEvaluation tssDeltaDecision = null;

            if (deltaSteps == null || deltaSteps.isEmpty()) {
                System.err.println(
                        "DomesticDSMStrategy: No delta steps left after filtering for setpoints, don't switch, wait for external state change.");
                return;
            } else if (deltaSteps.size() > 1) {
                System.err.println("DomesticDSMStrategy: " + deltaSteps.size()
                        + " delta steps left after filtering for setpoints, undecided, don't switch, wait for external state change.");
                return;
            } else { // it should be only one left
                tssDeltaDecision = deltaSteps.get(0);
            }

            nextTSSE = getNextTechnicalSystemStateEvaluation(tsse, tssDeltaDecision);
            if (nextTSSE == null) {
                System.err.println(
                        "DomesticDSMStrategy: No next state computable, don't switch, wait for external state change.");
                return;
            }

            // Set next state as new current state
            switchingNecessary = true;
            if (!agentDataModel.programRunning) {
                agentDataModel.startProgramAndExternalCoordination();

            }
        }
        // System.out.println(echoString);
    }

    private Vector<TechnicalSystemStateDeltaEvaluation> filterForPowerSwitching(
            Vector<TechnicalSystemStateDeltaEvaluation> deltaSteps, TechnicalSystemStateEvaluation tsse) {
        deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps,
                DeltaSelectionBy.IO_List, InternalDataModel.VAR_LOCKED_N_LOADED, ((FixedBoolean) InternalDataModel
                        .extractVariableByID(tsse.getIOlist(), InternalDataModel.VAR_LOCKED_N_LOADED)).isValue());
        deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps,
                DeltaSelectionBy.IO_List, InternalDataModel.VAR_WASHING_PROGRAM, ((FixedInteger) InternalDataModel
                        .extractVariableByID(tsse.getIOlist(), InternalDataModel.VAR_WASHING_PROGRAM)).getValue());

        // search for a state which could be transitioned into by switch the poweredOn
        Boolean intendedPoweredOn = ((FixedBoolean) InternalDataModel.extractVariableByID(tsse.getIOlist(),
                InternalDataModel.VAR_POWERED_ON)).isValue();
        intendedPoweredOn = !intendedPoweredOn; // invert poweredOn setting

        deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps,
                DeltaSelectionBy.IO_List, InternalDataModel.VAR_POWERED_ON, intendedPoweredOn);
        return deltaSteps;
    }

    private Vector<TechnicalSystemStateDeltaEvaluation> filterForUnlocking(
            Vector<TechnicalSystemStateDeltaEvaluation> deltaSteps, TechnicalSystemStateEvaluation tsse) {
        deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps,
                DeltaSelectionBy.IO_List, InternalDataModel.VAR_POWERED_ON, ((FixedBoolean) InternalDataModel
                        .extractVariableByID(tsse.getIOlist(), InternalDataModel.VAR_LOCKED_N_LOADED)).isValue());
        deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps,
                DeltaSelectionBy.IO_List, InternalDataModel.VAR_WASHING_PROGRAM, ((FixedInteger) InternalDataModel
                        .extractVariableByID(tsse.getIOlist(), InternalDataModel.VAR_WASHING_PROGRAM)).getValue());
        deltaSteps = TechnicalSystemStateDeltaHelper.filterTechnicalSystemStateDeltaEvaluation(deltaSteps,
                DeltaSelectionBy.IO_List, InternalDataModel.VAR_LOCKED_N_LOADED, false);
        return deltaSteps;
    }

    @Override
    public TechnicalSystemStateEvaluation getTechnicalSystemStateEvaluation() {
        if (!switchingNecessary) {
            return null;
        }

        if (nextTSSE == null) {
            return getSystemState();
        }

        return nextTSSE;
    }

    private TechnicalSystemStateEvaluation getSystemState() {
        if (agentDataModel == null) {
            return null;
        }
        return agentDataModel.getTechnicalSystemStateEvaluation();
    }

}