import java.io.Serializable;
import java.util.Objects;

public class Block implements Serializable {
    private static final long serialVersionUID = 100000000000l;

    Transaction[] transactions;
    Integer previousHash, hash;
    long timeStamp;

    Block(long timeStamp, Integer previousHash, Transaction[] transactions){
        this.timeStamp = timeStamp;
        this.previousHash = previousHash;
        this.transactions = transactions;
        this.hash = Objects.hash(this);
    }
}
