/**
 *
 */
package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import agentgui.core.project.Project;
import agentgui.simulationService.time.JPanel4TimeModelConfiguration;
import agentgui.simulationService.time.TimeModelContinuous;

/**
 *
 */
public class TimeModelPresent extends TimeModelContinuous {

    /**
     *
     */
    private static final long serialVersionUID = 7524432551373560286L;

    /**
     *
     */
    public TimeModelPresent() {
        setAccelerationFactor(1.0);
        setTimeStart(getTime());
        setTimeStop(Long.MAX_VALUE);
    }

    @Override
    public JPanel4TimeModelConfiguration getJPanel4Configuration(Project project) {
        return new TimeModelPresentConfiguration(project);
    }

    @Override
    public long getTime() {
        return getSystemTimeSynchronized();
    }

    @Override
    public long getTimeStart() {
        return getTime();
    }

    @Override
    protected void logTookLocalTime() {
        // don't output message
    }

}
