package de.unidue.stud.sehawagn.openhab.binding.jade;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import jade.osgi.service.agentFactory.AgentFactoryService;

public class AgentHolderActivator implements BundleActivator {

    private AgentFactoryService agentFactory;

    @Override
    public void start(BundleContext context) throws Exception {
//        System.out.println("FooBarActivator");
        // It seems, that this is necessary for the AgentFactoryService to really start?!
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
