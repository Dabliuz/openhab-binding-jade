package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import java.io.IOException;

import hygrid.agent.SwitchableEomAdapter;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

class OperationCommandReceiveBehaviour extends CyclicBehaviour {

    /**
     *
     */
    private final SmartHomeAgent myAgent;
    public static final MessageTemplate messageTemplate = MessageTemplate.or(
            MessageTemplate.MatchConversationId(SwitchableEomAdapter.CONVERSATION_ID_TOGGLE_OPERATION),
            MessageTemplate.MatchConversationId(SwitchableEomAdapter.CONVERSATION_ID_REQUEST_OPERATION_STATE));

    /**
     * @param smartHomeAgent
     */
    OperationCommandReceiveBehaviour(SmartHomeAgent smartHomeAgent) {
        this.myAgent = smartHomeAgent;
    }

    private static final long serialVersionUID = 3879355382412163550L;

    @Override
    public void action() {
        ACLMessage msg = myAgent.receive(messageTemplate);
        if (msg != null) {
            if (msg.getConversationId().equals(SwitchableEomAdapter.CONVERSATION_ID_TOGGLE_OPERATION)) {
                System.out.println("Received TOGGLE_OPERATION command.");
                try {
                    Boolean poweredOn = (Boolean) msg.getContentObject();
                    myAgent.setPoweredOn(poweredOn);
                } catch (UnreadableException e) {
                    System.err.println("Error getting content object from TOGGLE_OPERATION command.");
                    e.printStackTrace();
                }
            } else if (msg.getConversationId().equals(SwitchableEomAdapter.CONVERSATION_ID_REQUEST_OPERATION_STATE)) {
                System.out.println("Received REQUEST_OPERATION_STATE command.");
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                boolean poweredOn = myAgent.getPoweredOn();
                try {
                    reply.setContentObject(poweredOn);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                myAgent.send(reply);
            }
        }
        block();
    }
}