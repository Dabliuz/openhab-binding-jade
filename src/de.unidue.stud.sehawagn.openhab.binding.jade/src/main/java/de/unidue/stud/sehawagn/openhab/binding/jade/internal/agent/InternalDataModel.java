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
    public static final String VAR_POWER_CONSUMPTION = "PowerConsumption";
    public static final String VAR_OCCUPIED = "occupied";
    public static final boolean VAR_OCCUPIED_DEFAULT = true;
    public static final double VAR_POWER_CONSUMPTION_DEFAULT = 0.0;

    private boolean isOperating;

    public boolean isOperating() {
        return isOperating;
    }

    public void setOperating(boolean isOperating) {
        this.isOperating = isOperating;
    }

    /**
     * @param agent the agent
     */
    public InternalDataModel(Agent agent) {
        super(agent);
        isOperating = VAR_OCCUPIED_DEFAULT;
    }
}