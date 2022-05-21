import com.hazelcast.cp.IAtomicLong;

import java.io.Serializable;

public class Transaction implements Serializable {
    private static final long serialVersionUID = 100000000001l;
    String id, function;
    String[] args;
    Long timeStamp;

    Transaction(String id, Long timeStamp, String function, String[] args){
        this.id = id;
        this.timeStamp = timeStamp;
        this.function = function;
        this.args = args;
    }
}
