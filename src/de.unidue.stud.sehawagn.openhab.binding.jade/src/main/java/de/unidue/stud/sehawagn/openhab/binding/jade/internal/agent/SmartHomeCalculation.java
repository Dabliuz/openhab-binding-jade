package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import energy.OptionModelController;
import energy.calculations.AbstractOptionModelCalculation;
import energy.domain.DefaultDomainModelElectricity;
import energy.domain.DefaultDomainModelElectricity.PowerType;
import energy.optionModel.AbstractInterfaceFlow;
import energy.optionModel.Duration;
import energy.optionModel.EnergyFlowInWatt;
import energy.optionModel.EnergyUnitFactorPrefixSI;
import energy.optionModel.FixedDouble;
import energy.optionModel.TechnicalInterface;
import energy.optionModel.TechnicalSystemStateEvaluation;

/**
 * This class calculates the energy flows of a smart house.
 */
public class SmartHomeCalculation extends AbstractOptionModelCalculation {
    private static final String VAR_INPUT = "input";

    private static final double POWER_FACTOR = 0.925; // Wirkfaktor P/S = cos(phi)
    private static final double PHI = Math.acos(POWER_FACTOR);
    private static final double APPARENT_FACTOR = Math.tan(PHI);

    public SmartHomeCalculation(OptionModelController optionModelController) {
        super(optionModelController);
    }

    @Override
    public AbstractInterfaceFlow getEnergyOrGoodFlow(TechnicalSystemStateEvaluation techSysStaEva,
            TechnicalInterface techInt, boolean isManualConfiguration) {

        if (techInt.getDomainModel() instanceof DefaultDomainModelElectricity) {
            FixedDouble inputFD = (FixedDouble) this.getVariable(techSysStaEva.getIOlist(), VAR_INPUT);

            double activePower = 500.0 * inputFD.getValue(); // Wirkleistung P
            double apparentPower = activePower / POWER_FACTOR; // Scheinleistung S
            double reactivePower = APPARENT_FACTOR * activePower; // Blindleistung Q

            // double reactivePower = Math.sqrt(Math.pow(apparentPower, 2) - Math.pow(activePower, 2)); // Blindleistung
            // Q

            EnergyFlowInWatt currentEnergyFlow = new EnergyFlowInWatt();
            currentEnergyFlow.setSIPrefix(EnergyUnitFactorPrefixSI.NONE_0);

            PowerType powerType = ((DefaultDomainModelElectricity) techInt.getDomainModel()).getPowerType();
            if (powerType == PowerType.ActivePower) { // Wirkleistung P
                currentEnergyFlow.setValue(activePower);
            } else if (powerType == PowerType.ApparentPower) { // Scheinleistung S
                currentEnergyFlow.setValue(apparentPower);
            } else if (powerType == PowerType.ReactivePower) { // Blindleistung Q
                currentEnergyFlow.setValue(reactivePower);
            }

            return currentEnergyFlow;
        }
        return null;
    }

    @Override
    public Duration getDuration(DurationType arg0, TechnicalSystemStateEvaluation arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EnergyFlowInWatt getEnergyFlowForLosses(TechnicalSystemStateEvaluation arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}
