import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;
import com.hazelcast.map.IMap;

import javax.jms.JMSException;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NodeImpl implements Node {
    final static int BLOCK_SIZE = 10;
    String transactionsKey = "transactions-1111112", sequenceNoKey = "sequenceNo";
    IMap<Long, Transaction> transactionIMap;
    ConcurrentHashMap<Integer, Block> chain;
    PubClient pubClient;
    IAtomicLong atomicSeqNo;
    Long latestPublishedSeqNo;
    Integer tailHash;
    final Lock lock = new ReentrantLock();
    final Condition notBlockSize  = lock.newCondition();

    NodeImpl(){
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setClusterName("comp3358-cluster");

        HazelcastInstance hz = HazelcastClient.newHazelcastClient(clientConfig);
        transactionIMap = hz.getMap(transactionsKey);
        atomicSeqNo = hz.getCPSubsystem().getAtomicLong(sequenceNoKey);
        atomicSeqNo.set(0);

        chain = new ConcurrentHashMap<>();
        latestPublishedSeqNo = -1l;
        pubClient = new PubClient("jms/DefaultTopicConnectionFactory", "jms/DLTClient");
    }

    private void printChain(){
        Integer currHash = tailHash;

        System.out.println("CHAIN STATE:");

        Block b;
        while (currHash != null && (b = chain.get(currHash))!= null){
            System.out.println(b.hash + "->" + b.previousHash);
            currHash = b.previousHash;
        }
    }

    private void createAndPublishBlock() throws JMSException {
        Transaction[] transactions = new Transaction[BLOCK_SIZE];

        int i;
        for (i = 0; i < BLOCK_SIZE; i++){
            long seqNo = latestPublishedSeqNo + i + 1;
            transactions[i] = transactionIMap.get(seqNo);
        }

        Block b = new Block(System.currentTimeMillis(), tailHash, transactions);
        chain.put(b.hash, b);

        tailHash = b.hash;
        latestPublishedSeqNo += BLOCK_SIZE;

        String transactionIds = "";
        for (int j = 0; j < BLOCK_SIZE; j++) {
            transactionIds += String.format("%s, ", transactions[j].id);
        }

        String blockMsg = String.format("BLOCK[%s]{%s}", b.hash, transactionIds);
        pubClient.send(blockMsg);
        System.out.println("Published: " + blockMsg);
        printChain();
    }

    public String sendTransaction(String function, String[] args) {
        if (atomicSeqNo.get() % BLOCK_SIZE == 0){
            new Thread(() -> {
                lock.lock();
                try {
                    while (atomicSeqNo.get() - latestPublishedSeqNo < BLOCK_SIZE) notBlockSize.await();
                    createAndPublishBlock();
                } catch (InterruptedException | JMSException e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
            }).start();
        }

        Transaction transaction = new Transaction(UUID.randomUUID().toString(), System.currentTimeMillis(), function, args);
        transactionIMap.put(atomicSeqNo.getAndIncrement(), transaction);

        lock.lock();
        try {
            notBlockSize.signal();
        }finally {
          lock.unlock();
        }

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