import javax.jms.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class SubClient {

    TopicSubscriber subscriber;
    TopicSession session;

    SubClient (String factoryName, String topicName){
        InitialContext ctx;

        try {
            ctx = new InitialContext();

            TopicConnectionFactory factory = (TopicConnectionFactory) ctx.lookup(factoryName);
            TopicConnection connection = factory.createTopicConnection();
            connection.start();

            session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

            Topic topic = (Topic) ctx.lookup(topicName);

            subscriber = session.createSubscriber(topic);

        } catch (NamingException | JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void setMessageListener(MessageListener listener){
        try {
            subscriber.setMessageListener(listener);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws JMSException {
        subscriber.close();
    }
}
