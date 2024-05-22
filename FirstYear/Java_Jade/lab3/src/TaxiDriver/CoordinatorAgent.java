package TaxiDriver;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class CoordinatorAgent extends Agent {

    private Queue<ClientRequest> clientQueue = new LinkedList<>();

    @Override
    protected void setup() {
        addBehaviour(new HandleRequestsBehaviour());
        addBehaviour(new CheckQueueBehaviour());
    }

    private class HandleRequestsBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            if (msg != null && msg.getPerformative() == ACLMessage.REQUEST) {
                String clientLocation = msg.getContent();
                AID client = msg.getSender();
                clientQueue.add(new ClientRequest(client, clientLocation));
            } else {
                block();
            }
        }
    }

    private class CheckQueueBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            if (!clientQueue.isEmpty()) {
                ClientRequest clientRequest = clientQueue.peek();

                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("taxi");
                template.addServices(sd);

                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    List<AID> taxiAgents = new ArrayList<>();
                    for (DFAgentDescription dfd : result) {
                        taxiAgents.add(dfd.getName());
                    }
                    if (!taxiAgents.isEmpty()) {
                        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                        for (AID taxi : taxiAgents) {
                            request.addReceiver(taxi);
                        }
                        send(request);

                        Map<AID, Double> taxiDistances = new HashMap<>();
                        for (int i = 0; i < taxiAgents.size(); i++) {
                            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                            ACLMessage reply = blockingReceive(mt, 1000); // Ждем ответ в течение 1 секунды
                            if (reply != null) {
                                String taxiLocation = reply.getContent();
                                AID taxi = reply.getSender();
                                double distance = calculateDistance(clientRequest.location, taxiLocation);
                                taxiDistances.put(taxi, distance);
                            }
                        }

                        AID closestTaxi = null;
                        double minDistance = Double.MAX_VALUE;
                        for (Map.Entry<AID, Double> entry : taxiDistances.entrySet()) {
                            if (entry.getValue() < minDistance) {
                                minDistance = entry.getValue();
                                closestTaxi = entry.getKey();
                            }
                        }

                        if (closestTaxi != null) {
                            ACLMessage confirm = new ACLMessage(ACLMessage.CONFIRM);
                            confirm.addReceiver(closestTaxi);
                            confirm.setContent(clientRequest.location);
                            send(confirm);

                            ACLMessage informClient = new ACLMessage(ACLMessage.INFORM);
                            informClient.addReceiver(clientRequest.client);
                            informClient.setContent(closestTaxi.getLocalName());
                            send(informClient);
//                            System.out.println("Coordinator: Assigned " + closestTaxi.getLocalName() + " to client at " + clientRequest.location);
                            System.out.println("Coordinator: Assigned " + closestTaxi.getLocalName() + " to " + clientRequest.client.getLocalName() + " at " + clientRequest.location);
                            clientQueue.poll();
                        }
                    }
                    else
                    {
                        System.out.println("Coordinator: No available taxi to pick up " + clientRequest.client.getLocalName() + " at the moment");
                        doWait(1000);
                    }

                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            } else {
                block(1000);
            }
        }

        private double calculateDistance(String loc1, String loc2) {
            try {
                String[] coords1 = loc1.split(",");
                String[] coords2 = loc2.split(",");
                int x1 = Integer.parseInt(coords1[0]);
                int y1 = Integer.parseInt(coords1[1]);
                int x2 = Integer.parseInt(coords2[0]);
                int y2 = Integer.parseInt(coords2[1]);
                return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                System.err.println("Error parsing coordinates: " + loc1 + ", " + loc2);
                e.printStackTrace();
                return Double.MAX_VALUE;
            }
        }
    }

    private static class ClientRequest {
        AID client;
        String location;

        ClientRequest(AID client, String location) {
            this.client = client;
            this.location = location;
        }
    }

}
