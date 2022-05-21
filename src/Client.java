import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    Node node;
    SubClient subClient;

    Client(){
        this.subClient = new SubClient("jms/DefaultTopicConnectionFactory", "jms/DLTClient");
        subClient.setMessageListener(m->{
            try {
                Object obj = ((ObjectMessage) m).getObject();
                System.out.println(obj.toString());
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void main(String[] args) {
        Client client = new Client();

        String host = (args.length < 1) ? null : args[0];
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            client.node = (Node) registry.lookup("node");

            for (int i = 0; i < 11; i++){
                System.out.println(client.node.sendTransaction("CreateAccount", new String[]{"client-1", "1000"}));
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e);
            e.printStackTrace();
        }

        while (true);
    }
}
