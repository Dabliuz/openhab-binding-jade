package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import java.util.List;

import energy.FixedVariableList;
import energy.OptionModelController;
import energy.optionModel.FixedBoolean;
import energy.optionModel.FixedDouble;
import energy.optionModel.FixedInteger;
import energy.optionModel.FixedVariable;
import energy.optionModel.TechnicalSystemStateEvaluation;
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

	protected SmartifiedHomeOptionModelController optionModelController;

	protected SmartifiedHomeAgent myAgent;

	static final String EOM_STATE_UNSET = "UNSET";
	static final String EOM_STATE_AUTOMATIC_END = "10_Lockern_Ruhen";

	TechnicalSystemStateEvaluation technicalSystemStateEvaluation = null;

	public boolean waitForCoordination = false;

	public boolean programRunning = false;

	/**
	 * @param agent the agent
	 */
	public InternalDataModel(SmartifiedHomeAgent agent) {
		super(agent);
		myAgent = agent;
	}

	@Override
	public OptionModelController getOptionModelController() {
		if (optionModelController == null) {
			optionModelController = new SmartifiedHomeOptionModelController();
			optionModelController.setControllingAgent(this.myAgent);
			optionModelController.getEvaluationStrategy();
		}
		return optionModelController;
	}

	@Override
	public ControlledSystemType getTypeOfControlledSystem() {
		return ControlledSystemType.TechnicalSystem;
	}

	/**
	 * Gets a {@link FixedVariable} with a specified ID from a list of {@link FixedVariable}s.
	 *
	 * @param variablesList The variables list
	 * @param variableID The variable ID
	 * @return the variable, null if not found
	 */
	static FixedVariable extractVariableByID(List<FixedVariable> variablesList, String variableID) {
		for (FixedVariable fv : variablesList) {
			if (fv.getVariableID().equals(variableID)) {
				return fv;
			}
		}
		return null;
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

//    public static boolean deriveVariable(FixedVariableList variableList, String variableID) {
//        return deriveVariable(variableList, variableID, Boolean.class);
//    }

	@SuppressWarnings("unchecked")
	public static <T> T deriveVariable(FixedVariableList variableList, String variableID) {
		Object returnObject = null;
		Class<T> clazz = null;
		FixedVariable sP1 = variableList.getVariable(variableID);
		if (sP1 instanceof FixedBoolean) {
			returnObject = ((FixedBoolean) sP1).isValue();
			clazz = (Class<T>) Boolean.class;
		} else if (sP1 instanceof FixedInteger) {
			returnObject = ((FixedInteger) sP1).getValue();
			clazz = (Class<T>) Integer.class;
		} else if (sP1 == null) {
			System.err.println("deriveVariable() " + variableID + " not found! :-(");
			return null;
		} else {
			return null;
		}
		return clazz.cast(returnObject);
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

	public void startProgramAndExternalCoordination() {
		System.out.println("startProgramAndExternalCoordination()");
		myAgent.coordinateWithGrid();
		programRunning = true;
	}
}