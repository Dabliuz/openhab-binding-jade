package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import energy.OptionModelController;
import energy.calculations.AbstractOptionModelCalculation;
import energy.evaluation.AbstractEvaluationStrategy;

public class CustomOptionModelController extends OptionModelController {

    private DomesticDemandSideManagementStrategyRT evaluationStrategy;

    public CustomOptionModelController() {
    }

//    @Override
//    public AbstractEvaluationStrategy getEvaluationStrategyRT() {
//        return
//    }

    @Override
    public AbstractEvaluationStrategy getEvaluationStrategy() {
        if (evaluationStrategy == null) {
            evaluationStrategy = new DomesticDemandSideManagementStrategyRT(this);
        }
        return evaluationStrategy;
    }

    @Override
    public AbstractOptionModelCalculation createOptionModelCalculation() {
        return new SmartHomeCalculation(this);
    }

}
