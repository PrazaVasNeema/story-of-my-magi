package TaxiDriver;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;

public class ClientAgent extends Agent {

    private String location;
    private String localName;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(",");
                sb.append(args[i]);
            }
            location = sb.toString();
        } else {
            location = "0,0";
        }
        localName = getLocalName();
        addBehaviour(new RequestTaxiBehaviour());
    }

    private class RequestTaxiBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
            msg.setContent(location);
            send(msg);

            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage reply = blockingReceive(mt);
            if (reply != null) {
                String taxiID = reply.getContent();
                System.out.println("(Client) " + localName + ": Taxi " + taxiID + " will pick me up at " + location);
                doDelete();
            }
        }
    }

}

