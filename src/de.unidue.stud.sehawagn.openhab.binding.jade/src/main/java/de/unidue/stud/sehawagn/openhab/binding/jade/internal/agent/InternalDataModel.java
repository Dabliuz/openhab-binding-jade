package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import hygrid.agent.AbstractInternalDataModel;
import jade.core.Agent;

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
    public static final String VAR_VOLTAGE = "Voltage";
    public static final String VAR_OCCUPIED = "occupied";
    public static final boolean VAR_OCCUPIED_DEFAULT = true;
    public static final double VAR_VOLTAGE_DEFAULT = 0.0;

    /**
     * @param agent the agent
     */
    public InternalDataModel(Agent agent) {
        super(agent);
    }
}