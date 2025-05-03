package me.matiego.countingmc;

import me.matiego.countingmc.utils.Logs;
import org.jetbrains.annotations.NotNull;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketClient {
    public WebSocketClient(@NotNull Main instance) {
        this.instance = instance;
    }

    private static final String KEY_COOKIE = "x-counting-key";
    private static final long RECONNECT_DELAY = 3;
    private static final long RECONNECT_DELAY_MULTIPLIER = 2;
    private static final int MAX_RECONNECT_DELAY = 600;


    private final Main instance;
    private HttpClient client;
    private WebSocket webSocket;
    private ScheduledExecutorService scheduler;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private String apiKey;
    private URI apiUri;

    public void start() {
        close();

        scheduler = Executors.newSingleThreadScheduledExecutor();

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);

        client = HttpClient.newBuilder()
                .cookieHandler(CookieHandler.getDefault())
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        apiKey = instance.getConfig().getString("key", "");
        apiUri = URI.create(instance.getConfig().getString("url", ""));

        connect(0);
    }

    public void scheduleReconnect() {
        int attempt = reconnectAttempts.getAndIncrement();

        int delay = (int) Math.min(MAX_RECONNECT_DELAY, RECONNECT_DELAY * Math.pow(RECONNECT_DELAY_MULTIPLIER, attempt));
        connect(delay);
    }

    public void close() {
        closeWebSocket();
        if (client == null) return;
        client.close();
        client = null;
    }

    private void connect(long delay) {
        scheduler.schedule(() -> {
            client.newWebSocketBuilder()
                    .header("Cookie", KEY_COOKIE + "=" + apiKey)
                    .buildAsync(apiUri, new WebSocketListener(instance))
                    .thenAccept(ws -> {
                        webSocket = ws;
                        reconnectAttempts.set(0);
                        Logs.info("WebSocket connected successfully!");
                    })
                    .exceptionally(e -> {
                        Logs.error("Failed to connect to a WebSocket", e);
                        scheduleReconnect();
                        return null;
                    });
        }, delay, TimeUnit.SECONDS);
    }

    private void closeWebSocket() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        if (webSocket == null) return;
        try {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Closing")
                    .orTimeout(5, TimeUnit.SECONDS)
                    .whenComplete((result, e) -> {
                        if (e != null) {
                            Logs.error("Failed to gracefully close the WebSocket, aborting...", e);
                            webSocket.abort();
                        } else {
                            Logs.info("WebSocket is closed");
                        }
                    })
                    .join();
        } catch (Exception e) {
            Logs.error("Failed to close the WebSocket", e);
        }
    }


}
