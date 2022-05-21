import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Node extends Remote {
    String sendTransaction(String function, String[] args) throws RemoteException;
}