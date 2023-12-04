package rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * @author xiaorui
 */
public class ChannelPooledObjectFactory implements PooledObjectFactory<Channel> {
    private final Connection connection;

    public ChannelPooledObjectFactory(Connection connection) {
        this.connection = connection;
    }

    @Override
    public PooledObject<Channel> makeObject() throws Exception {
        Channel channel = connection.createChannel();
        return new DefaultPooledObject<>(channel);
    }

    @Override
    public void passivateObject(PooledObject<Channel> pooledObject) {
    }

    @Override
    public void activateObject(PooledObject<Channel> pooledObject) {
    }

    @Override
    public void destroyObject(PooledObject<Channel> p) throws Exception {
        p.getObject().close();
    }

    @Override
    public boolean validateObject(PooledObject<Channel> p) {
        return p.getObject().isOpen();
    }
}
