package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import hygrid.agent.AbstractInternalDataModel;
import jade.core.Agent;

/**
 * This class represents the internal data model of the corresponding agent.
 *
 * It is advised to only specify 'protected final String' elements and use these
 * (within IOSimulated or IOReal) when assigning values to the fixedVariableListMeasurements
 * (within AbstractInternalDataModel).
 *
 */
public class InternalDataModel extends AbstractInternalDataModel {
    private static final long serialVersionUID = 3913554312467337020L;
    static final String VAR_VOLTAGE = "Voltage";

    /** Specification of the internal data model's elements */

    /**
     * @param agent the agent
     */
    public InternalDataModel(Agent agent) {
        super(agent);
    }
}