import com.google.gson.Gson;
import constant.Constant;
import lombok.extern.slf4j.Slf4j;
import models.PostResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xiaorui
 */
@Slf4j
public class ClientTest {
    private static String ipAddr;
    private static final AtomicInteger SUCCESSFUL_REQUESTS = new AtomicInteger(0);
    private static final AtomicInteger FAILED_REQUESTS = new AtomicInteger(0);
    private static ApiHttpClient apiClient;
    private static final Gson gson = new Gson();

    public static void main(String[] args) throws InterruptedException {
        // Check arguments
        if (args.length < Constant.ARGS_NUM) {
            log.error("Usage: java ApiClient <threadGroupSize> <numThreadGroups> <delay> <IPAddr>");
            System.exit(1);
        }

        // Initialize arguments
        int threadGroupSize = Integer.parseInt(args[0]);
        int numThreadGroups = Integer.parseInt(args[1]);
        int delay = Integer.parseInt(args[2]);
        ipAddr = args[3];
        apiClient = new ApiHttpClient(ipAddr);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                threadGroupSize * numThreadGroups,
                threadGroupSize * numThreadGroups + 2,
                1L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < numThreadGroups; i++) {
            for (int j = 0; j < threadGroupSize; j++) {
                executor.submit(() -> {
                    HttpPost albumPostRequest = apiClient.createAlbumPostRequest();
                    for (int k = 0; k < Constant.API_CALLS; k++) {
                        String albumId = executeAlbumPost(albumPostRequest);
                        HttpPost reviewLikeRequest = apiClient.createPostReviewRequest("like", albumId);
                        HttpPost reviewDislikeRequest = apiClient.createPostReviewRequest("dislike", albumId);
                        executeReviewPost(reviewLikeRequest);
                        executeReviewPost(reviewLikeRequest);
                        executeReviewPost(reviewDislikeRequest);
                    }
                });
            }
            if (i < numThreadGroups - 1) {
                Thread.sleep(delay * 1000L);
            }
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Main thread interrupted while waiting for task completion", e);
        }
        long endTime = System.currentTimeMillis();

        // Time in s
        long totalTime = endTime - startTime;
        // Time in ms
        double wallTime = totalTime / 1000.0;
        log.info("Wall Time: " + wallTime + " seconds");
        log.info("Successful Requests: " + SUCCESSFUL_REQUESTS.get() + " times");
        log.info("Failed Requests: " + FAILED_REQUESTS.get() + " times");
        double throughput = (SUCCESSFUL_REQUESTS.get() + FAILED_REQUESTS.get()) / wallTime;
        log.info("Throughput: " + throughput + " request/second");
    }

    private static String executeAlbumPost(HttpUriRequest request) {
        int retries = Constant.MAX_RETRIES;

        while (retries > 0) {
            try (CloseableHttpResponse response = apiClient.executeRequest(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                PostResponse postResponse = gson.fromJson(responseBody, PostResponse.class);
                String albumId = postResponse.getAlbumId();
                EntityUtils.consume(response.getEntity());

                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    SUCCESSFUL_REQUESTS.incrementAndGet();
                    return albumId;
                } else if (statusCode >= 400 && statusCode < 600) {
                    FAILED_REQUESTS.incrementAndGet();
                    retries--;
                    if (retries == 0) {
                        log.error("Failed to execute request after 5 retries. URL: " + request.getURI() + request.getMethod());
                    }
                }
            } catch (Exception e) {
                FAILED_REQUESTS.incrementAndGet();
                e.printStackTrace();
                log.info("Exception in Album Post: " + request.getMethod());
            }
        }
        return null;
    }

    private static void executeReviewPost(HttpUriRequest request) {
        int retries = Constant.MAX_RETRIES;

        while (retries > 0) {
            try (CloseableHttpResponse response = apiClient.executeRequest(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                EntityUtils.consume(response.getEntity());

                if (statusCode >= 200 && statusCode < 300) {
                    SUCCESSFUL_REQUESTS.incrementAndGet();
                    return;
                } else if (statusCode >= 400 && statusCode < 600) {
                    FAILED_REQUESTS.incrementAndGet();
                    retries--;
                    if (retries == 0) {
                        log.error("Failed to execute request after 5 retries. URL: " + request.getURI() + request.getMethod());
                    }
                }
            } catch (Exception e) {
                FAILED_REQUESTS.incrementAndGet();
                e.printStackTrace();
                log.info("Exception in Review Post: " + request.getMethod());
            }
        }
    }
}