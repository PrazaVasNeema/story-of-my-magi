package TaxiDriver;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Main {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        ContainerController cc = rt.createMainContainer(p);

        try {
            AgentController coordinator = cc.createNewAgent("coordinator", "CoordinatorAgent", null);
            coordinator.start();

            AgentController client = cc.createNewAgent("client", "ClientAgent", new Object[]{"2,3"});
            client.start();

            AgentController taxi1 = cc.createNewAgent("taxi1", "TaxiAgent", null);
            taxi1.start();

            AgentController taxi2 = cc.createNewAgent("taxi2", "TaxiAgent", null);
            taxi2.start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
