package rabbitmq;

import com.rabbitmq.client.*;
import com.google.gson.Gson;
import db.AlbumDAO;
import lombok.extern.slf4j.Slf4j;
import models.ReviewMessage;

import java.sql.Connection;
import java.sql.SQLException;
/**
 * @author xiaorui
 */
@Slf4j
public class ReviewConsumer implements Runnable {
    private final RabbitmqConnectionManager rabbitmqConnectionManager;
    private final Gson gson = new Gson();
    private static final String QUEUE_NAME = "ReviewQueue";

    public ReviewConsumer(RabbitmqConnectionManager rabbitmqConnectionManager) {
        this.rabbitmqConnectionManager = rabbitmqConnectionManager;
    }


    @Override
    public void run() {
        try {
            Channel channel = rabbitmqConnectionManager.borrowChannel();
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.basicQos(10);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String messageJson = new String(delivery.getBody(), "UTF-8");
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                ReviewMessage reviewMessage = gson.fromJson(messageJson, ReviewMessage.class);
                processReviewMessage(reviewMessage);
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
        } catch (Exception e) {
            log.error("Failed to consume messages: ", e);
        }
    }

    private void processReviewMessage(ReviewMessage reviewMessage) {
        try (Connection conn = AlbumDAO.getConnection()) {
            boolean like = reviewMessage.getMessage().equalsIgnoreCase("like");
            AlbumDAO.updateAlbumReview(reviewMessage.getAlbumId(), like, conn);
            log.info("Processed review for album ID: " + reviewMessage.getAlbumId());
        } catch (SQLException e) {
            log.error("Database error while processing review message: ", e);
        }
    }
}
