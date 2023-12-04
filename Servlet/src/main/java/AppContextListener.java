import rabbitmq.RabbitmqConnectionManager;
import rabbitmq.ReviewConsumer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author xiaorui
 */
@WebListener
public class AppContextListener implements ServletContextListener {
    private ExecutorService executorService;
    private RabbitmqConnectionManager connectionManager;
    private static final int NUM_OF_THREADS = 100;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        executorService = Executors.newFixedThreadPool(NUM_OF_THREADS);
        try {
            connectionManager = new RabbitmqConnectionManager();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < NUM_OF_THREADS; i++) {
            ReviewConsumer reviewConsumer = new ReviewConsumer(connectionManager);
            executorService.execute(reviewConsumer);
        }

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
