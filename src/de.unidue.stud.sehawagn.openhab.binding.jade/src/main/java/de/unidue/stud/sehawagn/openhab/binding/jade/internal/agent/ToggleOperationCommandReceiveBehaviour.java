package de.unidue.stud.sehawagn.openhab.binding.jade.internal.agent;

import hygrid.agent.SwitchableEomAdapter;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

class ToggleOperationCommandReceiveBehaviour extends CyclicBehaviour {

    /**
     *
     */
    private final SmartHomeAgent smartHomeAgent;
    public static final MessageTemplate messageTemplate = MessageTemplate.MatchConversationId(SwitchableEomAdapter.CONVERSATION_ID_TOGGLE_OPERATION);

    /**
     * @param smartHomeAgent
     */
    ToggleOperationCommandReceiveBehaviour(SmartHomeAgent smartHomeAgent) {
        this.smartHomeAgent = smartHomeAgent;
    }

    private static final long serialVersionUID = 3879355382412163550L;

    @Override
    public void action() {
        ACLMessage msg = this.myAgent.receive(messageTemplate);
        if (msg != null) {
            try {
                Boolean operating = (Boolean) msg.getContentObject();
                this.smartHomeAgent.setOperating(operating);
                System.out.println("Received TOGGLE_OPERATION command");
            } catch (UnreadableException e) {
                System.err.println("Error getting content object from toggle operation command message");
                e.printStackTrace();
            }
        }
    }
}