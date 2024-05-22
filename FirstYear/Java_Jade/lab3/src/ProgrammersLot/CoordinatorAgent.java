package ProgrammersLot;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CoordinatorAgent extends Agent {
    private List<Project> projects = new ArrayList<>();

    @Override
    protected void setup() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("coordinator");
        sd.setName(getLocalName());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

//        addBehaviour(new FindProjectsBehaviour());

        addBehaviour(new HandleRequestsBehaviour());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void FillProjectsList(Agent myAgent)
    {
        projects.clear();

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("project");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(myAgent, template);
            for (DFAgentDescription dfd : result) {
                AID projectAID = dfd.getName();
                Iterator<ServiceDescription> services = dfd.getAllServices();
                while (services.hasNext()) {
                    ServiceDescription service = services.next();
                    String[] projectDetails = service.getName().split(",");
                    int cost = Integer.parseInt(projectDetails[0].trim());
                    int requiredProgrammers = Integer.parseInt(projectDetails[1].trim());
                    int minimumExperience = Integer.parseInt(projectDetails[2].trim());
                    projects.add(new Project(projectAID, cost, requiredProgrammers, minimumExperience));
                }
            }
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

//    private class FindProjectsBehaviour extends CyclicBehaviour {
//        @Override
//        public void action() {
//            projects.clear();
//
//            DFAgentDescription template = new DFAgentDescription();
//            ServiceDescription sd = new ServiceDescription();
//            sd.setType("project");
//            template.addServices(sd);
//
//            try {
//                DFAgentDescription[] result = DFService.search(myAgent, template);
//                for (DFAgentDescription dfd : result) {
//                    AID projectAID = dfd.getName();
//                    Iterator<ServiceDescription> services = dfd.getAllServices();
//                    while (services.hasNext()) {
//                        ServiceDescription service = services.next();
//                        String[] projectDetails = service.getName().split(",");
//                        int cost = Integer.parseInt(projectDetails[0].trim());
//                        int requiredProgrammers = Integer.parseInt(projectDetails[1].trim());
//                        int minimumExperience = Integer.parseInt(projectDetails[2].trim());
//                        projects.add(new Project(projectAID, cost, requiredProgrammers, minimumExperience));
//                    }
//                }
//            } catch (FIPAException fe) {
//                fe.printStackTrace();
//            }
//
//            block(5000);
//        }
//    }

    private class HandleRequestsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                FillProjectsList(myAgent);
                String content = msg.getContent();
                String[] parts = content.split(":");
                if (parts.length == 2 && parts[0].trim().equals("Looking for projects with experience")) {
                    int experience = Integer.parseInt(parts[1].trim());

                    List<Project> suitableProjects = new ArrayList<>();
                    for (Project project : projects) {
                        if (project.minimumExperience <= experience) {
                            suitableProjects.add(project);
                        }
                    }

                    Project bestProject = null;
                    int maxCost = 0;
                    for (Project project : suitableProjects) {
                        if (project.cost > maxCost) {
                            bestProject = project;
                            maxCost = project.cost;
                        }
                    }

                    ACLMessage reply = msg.createReply();
                    if (bestProject != null) {
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent("Project: " + bestProject.aid.getLocalName() + ", Project Payment: " + bestProject.cost + ", Required Programmers: " + bestProject.requiredProgrammers + ", Minimum Experience: " + bestProject.minimumExperience);
                        send(reply);

                        ACLMessage projectMsg = new ACLMessage(ACLMessage.INFORM);
                        projectMsg.addReceiver(bestProject.aid);
//                        projectMsg.setContent("Adding programmer: " + msg.getSender().getLocalName());
                        projectMsg.setContent(msg.getSender().getName());
                        send(projectMsg);
                    } else {
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent("No suitable projects found");
                    }
                    send(reply);
                }
            } else {
                block();
            }
        }
    }

    private static class Project {
        AID aid;
        int cost;
        int requiredProgrammers;
        int minimumExperience;

        Project(AID aid, int cost, int requiredProgrammers, int minimumExperience) {
            this.aid = aid;
            this.cost = cost;
            this.requiredProgrammers = requiredProgrammers;
            this.minimumExperience = minimumExperience;
        }
    }
}


