package servlets;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import models.ErrorMsg;
import models.ReviewMessage;
import rabbitmq.RabbitmqConnectionManager;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author xiaorui
 */
@Slf4j
public class ReviewServlet extends HttpServlet {
    private final Gson gson = new Gson();
    private RabbitmqConnectionManager rabbitmqConnectionManager;
    private static final String QUEUE_NAME = "ReviewQueue";

    public void init() throws ServletException {
        super.init();
        try {
            rabbitmqConnectionManager = new RabbitmqConnectionManager();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // POST: /review/{likeornot}/{albumId}
        res.setContentType("application/json");

        String urlPath = req.getPathInfo();
        if (!isValidUrl(urlPath)) {
            ErrorMsg errorMsg = new ErrorMsg("Missing or empty url");
            res.getWriter().write(gson.toJson(errorMsg));
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        String[] urlParts = urlPath.split("/");
        if (!isValidUrlParts(urlParts)) {
            ErrorMsg errorMsg = new ErrorMsg("Invalid format url");
            res.getWriter().write(gson.toJson(errorMsg));
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        String likeOrNot = urlParts[1];
        int albumId = Integer.parseInt(urlParts[2]);
        ReviewMessage reviewMessage = new ReviewMessage(albumId, likeOrNot);

        try {
            String jsonMessage = gson.toJson(reviewMessage);
            Channel channel = null;

            try {
                channel = rabbitmqConnectionManager.borrowChannel();
                channel.basicPublish("", QUEUE_NAME, null, jsonMessage.getBytes());
                res.setStatus(HttpServletResponse.SC_OK);
            } catch (Exception e) {
                log.error("Error publishing message: ", e);
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                ErrorMsg errorMsg = new ErrorMsg("Internal server error");
                res.getWriter().write(gson.toJson(errorMsg));
            } finally {
                if (channel != null) {
                    rabbitmqConnectionManager.returnChannel(channel);
                }
            }
        } catch (NumberFormatException e) {
            log.error("Invalid number format: ", e);
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ErrorMsg errorMsg = new ErrorMsg("Invalid album ID format");
            res.getWriter().write(gson.toJson(errorMsg));
        }

    }

    private boolean isValidUrl(String urlPath) {
        return urlPath != null && !urlPath.isEmpty();
    }

    private boolean isValidUrlParts(String[] urlParts) {
        if (urlParts.length == 3) {
            return urlParts[1].equals("like") || urlParts[1].equals("dislike");
        }
        return false;
    }


}
