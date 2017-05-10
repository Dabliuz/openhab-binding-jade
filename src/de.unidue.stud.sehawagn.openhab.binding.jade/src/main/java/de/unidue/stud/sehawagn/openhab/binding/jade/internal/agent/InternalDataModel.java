package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import energy.FixedVariableList;
import energy.OptionModelController;
import energy.optionModel.FixedBoolean;
import energy.optionModel.FixedDouble;
import energy.optionModel.FixedInteger;
import energy.optionModel.FixedVariable;
import hygrid.agent.AbstractInternalDataModel;

/**
 * This class represents the internal data model of the agent.
 *
 * Only use these String constants when assigning values to the fixedVariableListMeasurements within IOBehaviours
 *
 */
public class InternalDataModel extends AbstractInternalDataModel {
    private static final long serialVersionUID = 3913554312467337020L;

    /**
     * elements of the internal data model
     */
    public static final String VAR_WASHING_PROGRAM = "WashingProgram";     // Has to correlate with EOM I/O variable
    public static final String VAR_LOCKED_N_LOADED = "LockedNLoaded";      // Has to correlate with EOM I/O variable
    public static final String VAR_POWER_CONSUMPTION = "PowerConsumption"; // Has to correlate with EOM I/O variable
    public static final String VAR_POWERED_ON = "PoweredOn";               // Has to correlate with EOM I/O variable

    public static final double VAR_POWER_CONSUMPTION_DEFAULT = 0.0;

    protected CustomOptionModelController optionModelController;

    protected SmartHomeAgent myAgent;

    /**
     * @param agent the agent
     */
    public InternalDataModel(SmartHomeAgent agent) {
        super(agent);
        myAgent = agent;
    }

    @Override
    public OptionModelController getOptionModelController() {
        if (optionModelController == null) {
            optionModelController = new CustomOptionModelController();
            optionModelController.setControllingAgent(this.myAgent);
            optionModelController.getEvaluationStrategy();
        }
        return optionModelController;
    }

    @Override
    public ControlledSystemType getTypeOfControlledSystem() {
        return ControlledSystemType.TechnicalSystem;
    }

    public static FixedVariableList produceVariableList(Object newValue, String variableID) {
        FixedVariableList variableList = new FixedVariableList();
        variableList.add(produceVariable(newValue, variableID));
        return variableList;
    }

    public static FixedVariable produceVariable(Object newValue, String variableID) {
        FixedVariable var = null;
        if (newValue instanceof Boolean) {
            FixedBoolean var1 = new FixedBoolean();
            var1.setValue((Boolean) newValue);
            var = var1;
        } else if (newValue instanceof Double) {
            FixedDouble var2 = new FixedDouble();
            var2.setValue((Double) newValue);
            var = var2;
        } else if (newValue instanceof Integer) {
            FixedInteger var3 = new FixedInteger();
            var3.setValue((Integer) newValue);
            var = var3;
        } else {
            System.err.println("CONVERSION ERROR IN RealIOBehaviour");
        }
        if (var != null) {
            var.setVariableID(variableID);
        }
        return var;
    }

    public static boolean deriveVariable(FixedVariableList variableList, String variableID) {
        FixedVariable sP1 = variableList.getVariable(variableID);
        if (sP1 instanceof FixedBoolean) {
            return ((FixedBoolean) sP1).isValue();
        } else if (sP1 == null) {
            System.out.println("deriveVariable() " + variableID + " not found! :-(");
        }
        return false;
    }

    public static void dumpVariableList(FixedVariableList variableList) {
        String variableString = "";
        String variableID = "";
        if (variableList != null) {
            for (FixedVariable variable : variableList) {
                variableID = variable.getVariableID();
                variableString += variableID;
                variableString += "=";
                if (variable instanceof FixedBoolean) {
                    variableString += ((FixedBoolean) variable).isValue();
                } else if (variable instanceof FixedDouble) {
                    variableString += ((FixedDouble) variable).getValue();
                } else if (variable instanceof FixedInteger) {
                    variableString += ((FixedInteger) variable).getValue();
                } else {
                    variableString += "UNKNOWNTYPE(" + variable.getClass().getSimpleName() + ")";
                }

                variableString += "; ";
            }
        }
        System.out.println(variableString);
    }
}