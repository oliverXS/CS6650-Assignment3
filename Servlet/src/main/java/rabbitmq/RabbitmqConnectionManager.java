package rabbitmq;

import lombok.extern.slf4j.Slf4j;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author xiaorui
 */
@Slf4j
public class RabbitmqConnectionManager {
    private Connection rabbitmqConnection;
    private GenericObjectPool<Channel> channelPool;
    private static final String HOST = "54.245.156.243";
    private static final int PORT = 5672;
    private static final String USER_NAME = "";
    private static final String PWD = "";
    private static final String V_HOST = "/";

    public RabbitmqConnectionManager() throws IOException, TimeoutException {
        ConnectionFactory conFactory = new ConnectionFactory();
        conFactory.setHost(HOST);
        conFactory.setPort(PORT);
        conFactory.setUsername(USER_NAME);
        conFactory.setPassword(PWD);
        conFactory.setVirtualHost(V_HOST);

        rabbitmqConnection = conFactory.newConnection();

        // Setting up the channel pool
        ChannelPooledObjectFactory pooledObjectFactory = new ChannelPooledObjectFactory(rabbitmqConnection);
        GenericObjectPoolConfig<Channel> config = new GenericObjectPoolConfig<>();
        this.channelPool = new GenericObjectPool<>(pooledObjectFactory, config);
    }

    public Connection getConnection() {
        return rabbitmqConnection;
    }

    public Channel borrowChannel() throws Exception {
        return channelPool.borrowObject();
    }

    public void returnChannel(Channel channel) {
        channelPool.returnObject(channel);
    }

}
