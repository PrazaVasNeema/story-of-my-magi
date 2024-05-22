package TaxiDriver;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.Random;

public class TaxiAgent extends Agent {

    private boolean busy = false;
    private String location;
    private String localName;

    @Override
    protected void setup() {
        Random rand = new Random();
        location = rand.nextInt(10) + "," + rand.nextInt(10);
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("taxi");
        sd.setName(getLocalName());
        localName = getLocalName();
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("(Taxi)" + localName + ": Is ready, start location: " + location);
        addBehaviour(new RespondToCoordinatorBehaviour());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private class RespondToCoordinatorBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.REQUEST && !busy) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(location);
                    send(reply);
                } else if (msg.getPerformative() == ACLMessage.CONFIRM) {
                    busy = true;
                    String clientLocation = msg.getContent();
                    System.out.println("(Taxi)" + localName + ": Picking up client at " + clientLocation);

                    doWait(5000);

                    Random rand = new Random();
                    location = rand.nextInt(10) + "," + rand.nextInt(10);
                    busy = false;
                    System.out.println("(Taxi)" + localName + ": Finished the drive and now is free, current location: " + location);
                    ACLMessage done = new ACLMessage(ACLMessage.INFORM);
                    done.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
                    done.setContent("done");
                    send(done);
                }
            } else {
                block();
            }
        }
    }

}
