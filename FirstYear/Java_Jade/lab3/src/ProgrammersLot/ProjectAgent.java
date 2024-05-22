package ProgrammersLot;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.ArrayList;
import java.util.List;
import jade.core.AID;

public class ProjectAgent extends Agent {
    private int cost;
    private int requiredProgrammers;
    private int minimumExperience;
    private List<AID> programmers = new ArrayList<>();

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 3) {
            cost = Integer.parseInt(args[0].toString());
            requiredProgrammers = Integer.parseInt(args[1].toString());
            minimumExperience = Integer.parseInt(args[2].toString());
        }

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("project");
        sd.setName(cost + "," + requiredProgrammers + "," + minimumExperience);
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new AddProgrammerBehaviour());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private class AddProgrammerBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {
                AID programmer = new AID(msg.getContent(), AID.ISGUID);
                programmers.add(programmer);
                System.out.println("Project: " + getLocalName() + " added programmer: " + programmer.getLocalName());

                if (programmers.size() >= requiredProgrammers) {
                    System.out.println("Project: " + getLocalName() + " is fully staffed and will now terminate.");
                    doDelete();
                }
            } else {
                block();
            }
        }
    }
}

