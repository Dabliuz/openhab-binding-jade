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
    public static final String VAR_WASHING_PROGRAM = "WashingProgram";     // Has to correlate with EOM I/O variable
    public static final String VAR_LOCKED_N_LOADED = "LockedNLoaded";      // Has to correlate with EOM I/O variable
    public static final String VAR_POWER_CONSUMPTION = "PowerConsumption"; // Has to correlate with EOM I/O variable
    public static final String VAR_POWERED_ON = "PoweredOn";               // Has to correlate with EOM I/O variable

    public static final Integer SP_WASHING_PROGRAM_DEFAULT = 0;
    public static final boolean SP_LOCKED_N_LOADED_DEFAULT = false;
    public static final double VAR_POWER_CONSUMPTION_DEFAULT = 0.0;
    public static final boolean SP_POWERED_ON_DEFAULT = true;

    /**
     * @param agent the agent
     */
    public InternalDataModel(Agent agent) {
        super(agent);
    }
}