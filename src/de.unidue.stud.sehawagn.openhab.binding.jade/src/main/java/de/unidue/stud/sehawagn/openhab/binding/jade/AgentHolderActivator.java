package de.unidue.stud.sehawagn.openhab.binding.jade;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import jade.osgi.service.agentFactory.AgentFactoryService;

public class AgentHolderActivator implements BundleActivator {

    private AgentFactoryService agentFactory;

    @Override
    public void start(BundleContext context) throws Exception {
        System.out.println("FooBarActivator");
        // Register AgentFactory service
        Bundle b = context.getBundle();
        agentFactory = new AgentFactoryService();
        agentFactory.init(b);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        agentFactory.clean();
    }

}
