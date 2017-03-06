package jade.osgi.service.runtime.internal;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import jade.osgi.internal.AgentManager;
import jade.osgi.service.runtime.JadeRuntimeService;
import jade.wrapper.ContainerController;

public class JadeRuntimeServiceFactory implements ServiceFactory<Object> {

    private ContainerController container;
    private AgentManager agentManager;
    private Map<Long, JadeRuntimeService> usedJadeServices = new HashMap<Long, JadeRuntimeService>();
    private boolean split;

    public JadeRuntimeServiceFactory(ContainerController container, AgentManager agentManager) {
        this.container = container;
        this.agentManager = agentManager;
        this.split = false;
    }

    public JadeRuntimeServiceFactory(AgentManager agentManager) {
        this.agentManager = agentManager;
        this.split = true;
    }

    @Override
    public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
        JadeRuntimeService jadeService;
        if (split) {
            jadeService = new JadeMicroRuntimeServiceImpl(bundle);
        } else {
            jadeService = new JadeRuntimeServiceImpl(container, agentManager, bundle);
        }
        usedJadeServices.put(bundle.getBundleId(), jadeService);
        return jadeService;
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
        usedJadeServices.remove(bundle.getBundleId());
        if (service instanceof JadeRuntimeService) {
            // FIXME do something?
            // JadeRuntimeServiceImpl jadeService = (JadeRuntimeServiceImpl) service;
            // try {
            // jadeService.removeAgents();
            // } catch (Exception e) {
            // e.printStackTrace();
            // }
        }
    }

}