package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import energy.evaluation.TechnicalSystemStateDeltaEvaluation;
import energy.optionModel.FixedBoolean;
import energy.optionModel.FixedDouble;
import energy.optionModel.FixedInteger;
import energy.optionModel.FixedVariable;
import energy.optionModel.TechnicalSystemStateEvaluation;
import hygrid.agent.monitoring.MonitoringEvent;

public class Helper {

	public Helper() {
		// TODO Auto-generated constructor stub
	}

	private static String dateFormatDefinition = "dd.MM.yy HH:mm:ss:SSS";

	private static DateFormat dateFormat;

	/**
	 * Gets the date format.
	 *
	 * @return the date format
	 */
	private static DateFormat getDateFormat() {
		if (dateFormat == null) {
			dateFormat = new SimpleDateFormat(dateFormatDefinition);
		}
		return dateFormat;
	}

	public static void dumpTESSEAndMeasurement(MonitoringEvent monitoringEvent) {
		String output = "";

		TechnicalSystemStateEvaluation tsse = monitoringEvent.getTSSE();
		long timestamp = tsse.getGlobalTime();

		String measurementTime = getDateFormat().format(new Date(timestamp));
		output += tsse.getStateID() + "\t" + measurementTime + "\t";
		output += "StateTime: " + tsse.getStateTime() + "\t";

		output += "Measurements: " + variableListToString(monitoringEvent.getMeasurements(), tsse) + "\t";
		output += "SetPoints: " + variableListToString(monitoringEvent.getSetpoints(), tsse);

		System.out.println(output);
	}

	public static void dumpTSSE(TechnicalSystemStateEvaluation tsse) {
		String output = "";

		long timestamp = tsse.getGlobalTime();

		String measurementTime = getDateFormat().format(new Date(timestamp));
		output += tsse.getStateID() + "\t" + measurementTime + "\t";
		output += "StateTime: " + tsse.getStateTime() + "\t";

		output += "TSSE IOVars: " + variableListToString(tsse.getIOlist(), null) + "\t";  // dont write TSSE separately

		System.out.println(output);
	}

	/**
	 * Build a String representation from a VariableList.
	 *
	 * @param variablesList the variable list
	 * @param tsse the corresponding tsse
	 * @return the String representation
	 */
	public static String variableListToString(List<FixedVariable> variablesList, TechnicalSystemStateEvaluation tsse) {
		String variableString = "";
		String variableID = "";
		if (variablesList != null) {
			for (FixedVariable variable : variablesList) {
				variableID = variable.getVariableID();
				variableString += variableID;
				variableString += "=";
				FixedVariable tsseVariable = null;
				if (tsse != null) {
					tsseVariable = getVariableByID(tsse.getIOlist(), variableID);
				}
				if (variable instanceof FixedBoolean) {
					variableString += ((FixedBoolean) variable).isValue();
					FixedBoolean tsseValue = (FixedBoolean) tsseVariable;
					if (tsseValue != null) {
						variableString += "(TSSE:" + tsseValue.isValue() + ")";
					}
				} else if (variable instanceof FixedDouble) {
					variableString += ((FixedDouble) variable).getValue();
					FixedDouble tsseValue = (FixedDouble) tsseVariable;
					if (tsseValue != null) {
						variableString += "(TSSE:" + tsseValue.getValue() + ")";
					}
				} else if (variable instanceof FixedInteger) {
					variableString += ((FixedInteger) variable).getValue();
					FixedInteger tsseValue = (FixedInteger) tsseVariable;
					if (tsseValue != null) {
						variableString += "(TSSE:" + tsseValue.getValue() + ")";
					}
				} else {
					variableString += "UNKNOWNTYPE(" + variable.getClass().getSimpleName() + ")";
				}

				variableString += "; ";
			}
		}
		return variableString;
	}

	/**
	 * Gets a {@link FixedVariable} with a specified ID from a list of {@link FixedVariable}s.
	 *
	 * @param variablesList The variables list
	 * @param variableID The variable ID
	 * @return the variable, null if not found
	 */
	public static FixedVariable getVariableByID(List<FixedVariable> variablesList, String variableID) {
		for (FixedVariable fv : variablesList) {
			if (fv.getVariableID().equals(variableID)) {
				return fv;
			}
		}
		return null;
	}

	public static void dumpTSSEList(Vector<TechnicalSystemStateDeltaEvaluation> inputList, String comment) {
		if (inputList == null) {
			System.out.println(comment + " EMPTY! :-(");
			return;
		}
		System.out.println("=======");
		System.out.println(comment + ":");

		for (Iterator<TechnicalSystemStateDeltaEvaluation> iterator = inputList.iterator(); iterator.hasNext();) {
			TechnicalSystemStateDeltaEvaluation tssde = iterator.next();
			TechnicalSystemStateEvaluation tsse = tssde.getTechnicalSystemStateEvaluation();
			System.out.print(" - ");
			dumpTSSE(tsse);
		}
		System.out.println("=======");

	}

}
