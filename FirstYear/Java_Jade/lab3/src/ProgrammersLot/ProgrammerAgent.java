package ProgrammersLot;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class ProgrammerAgent extends Agent {
    private int experience;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            experience = Integer.parseInt(args[0].toString());
        } else {
            experience = 0; // Default experience
        }

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("programmer");
        sd.setName(getLocalName());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new ProjectSearchBehaviour());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private class ProjectSearchBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(new AID("coordinator", AID.ISLOCALNAME));
            request.setContent("Looking for projects with experience: " + experience);
            send(request);

            ACLMessage reply = blockingReceive();
            if (reply != null) {
                if (reply.getPerformative() == ACLMessage.INFORM) {
                    String projectDetails = reply.getContent();
                    if (projectDetails.startsWith("Project:")) {
                        System.out.println("(Prog)" + getLocalName() + " received project: " + projectDetails);
                        doDelete();
                    } else {
                        System.out.println("(Prog)" + getLocalName() + " received: " + projectDetails);
                        doWait(3000);
                    }
                }
            }
        }
    }
}


