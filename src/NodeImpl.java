import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;

import javax.jms.JMSException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NodeImpl implements Node {
    final static int BLOCK_SIZE = 10;
    String chainKey = "chain1", stateKey = "state1";
    String tail = "tail1";

    IMap<Integer, Block> chain;
    IMap<String, Object> state;
    List<Transaction> qTransactions;

    PubClient pubClient;

    NodeImpl(){
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName("comp3358-cluster");

        HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);

        chain = hz.getMap(chainKey);
        state = hz.getMap(stateKey);

        pubClient = new PubClient("jms/DefaultTopicConnectionFactory", "jms/DLTClient");

        qTransactions = new ArrayList<>();

        printChain();
    }

    private void printChain(){
        Integer currHash = (Integer) state.get(tail);

        Block b;
        while (currHash != null && (b = chain.get(currHash))!= null){
            System.out.println(b.hash + "->" + b.previousHash);
            currHash = b.previousHash;
        }
    }

    private void createAndPublishBlock(){
        /* order */
        qTransactions.sort((a, b)-> (int) (a.timeStamp - b.timeStamp));

        /* execute */

        /* publish */
        Transaction[] transactions = new Transaction[BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++){
            transactions[i] = qTransactions.remove(0);
        }

        Integer tailHash;
        state.lock(tail);
        try {
            tailHash = (Integer) state.get(tail);

            Block b = new Block(System.currentTimeMillis(), tailHash, transactions);
            chain.lock(b.hash);
            try {
                chain.put(b.hash, b);
                state.put(tail, b.hash);
            } finally {
                chain.unlock(b.hash);
            }

            pubClient.send(String.format("{ type:success, blockID: %s }", b.hash));
            System.out.println("BLOCK: " + b.hash);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        } finally {
            state.unlock(tail);
        }
    }

    public String sendTransaction(String function, String[] args) {
        Transaction transaction = new Transaction(UUID.randomUUID().toString(), System.currentTimeMillis(), function, args);
        qTransactions.add(transaction);

        new Thread(() -> {
            synchronized (qTransactions){
                if (qTransactions.size() >= BLOCK_SIZE) createAndPublishBlock();
            }
        }).start();

        return transaction.id;
    }

    public static void main(String[] args) {
        try {
            NodeImpl node = new NodeImpl();
            Node nodeStub = (Node) UnicastRemoteObject.exportObject(node, 0);

            Registry registry = LocateRegistry.createRegistry( 1099);
            registry.bind("node", nodeStub);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (AlreadyBoundException e) {
            throw new RuntimeException(e);
        }
    }
}