package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import hygrid.agent.AbstractEnergyAgent;
import hygrid.agent.SimulationConnectorRemoteForIOReal;

public class SafeSimulationConnectorRemoteForIOReal extends SimulationConnectorRemoteForIOReal {

    public SafeSimulationConnectorRemoteForIOReal(AbstractEnergyAgent myAgent) {
        super(myAgent);
    }

    @Override
    public void doDelete() {
        // Override doDelete to prevent agent restarting
    }

    @Override
    protected void restartAgent() {
        // Override restartAgent to prevent agent restarting
    }
}
