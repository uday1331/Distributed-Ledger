import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.Serializable;

public class PubClient {
    TopicPublisher publisher;
    TopicSession session;

    PubClient (String factoryName, String topicName){
        InitialContext ctx = null;
        try {
            ctx = new InitialContext();

            TopicConnectionFactory factory = (TopicConnectionFactory) ctx.lookup(factoryName);
            TopicConnection connection = factory.createTopicConnection();
            connection.start();

            session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            Topic topic = (Topic) ctx.lookup(topicName);

            publisher = session.createPublisher(topic);
        } catch (NamingException | JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void send (Serializable msg) throws JMSException {
        publisher.send(session.createObjectMessage(msg));
    }

    public void close() throws JMSException {
        publisher.close();
    }
}
