package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import energy.OptionModelController;
import energy.calculations.AbstractOptionModelCalculation;
import energy.evaluation.AbstractEvaluationStrategy;

public class SmartifiedHomeOptionModelController extends OptionModelController {

    private DomesticDemandSideManagementStrategyRT evaluationStrategy;

    public SmartifiedHomeOptionModelController() {
    }

    @Override
    public DomesticDemandSideManagementStrategyRT getEvaluationStrategy() {
        if (evaluationStrategy == null) {
            evaluationStrategy = new DomesticDemandSideManagementStrategyRT(this);
        }
        return evaluationStrategy;
    }

    @Override
    public AbstractOptionModelCalculation createOptionModelCalculation() {
        return new SmartifiedHomeCalculation(this);
    }
}