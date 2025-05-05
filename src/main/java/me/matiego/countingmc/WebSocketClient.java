package me.matiego.countingmc;

import me.matiego.countingmc.utils.Logs;
import me.matiego.countingmc.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketClient {
    public WebSocketClient(@NotNull Main instance) {
        this.instance = instance;
    }

    private static final String KEY_COOKIE = "x-counting-key";
    private static final long RECONNECT_DELAY = 5;
    private static final long RECONNECT_DELAY_MULTIPLIER = 2;
    private static final int MAX_RECONNECT_DELAY = 600;
    private static final int PING_DELAY = 5;

    private final Main instance;
    private HttpClient client;
    private WebSocket webSocket;
    private ScheduledExecutorService scheduler;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private String apiKey;
    private URI apiUri;
    private boolean closed = false;

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

        connect(0).join();

        scheduler.scheduleWithFixedDelay(() -> {
            if (webSocket == null) return;
            if (webSocket.isOutputClosed()) return;
            webSocket.sendText("ping", true);
        }, PING_DELAY, PING_DELAY, TimeUnit.SECONDS);
    }

    public void scheduleReconnect() {
        scheduleReconnect(0);
    }
    public void scheduleReconnect(int additionalDelay) {
        int attempt = reconnectAttempts.getAndIncrement();

        long delay = RECONNECT_DELAY * Math.min(MAX_RECONNECT_DELAY, Utils.pow(RECONNECT_DELAY_MULTIPLIER, attempt));
        if (attempt == 0) {
            delay += additionalDelay;
        }
        delay = Math.min(MAX_RECONNECT_DELAY, delay);

        connect(delay);
    }

    public void close() {
        closeWebSocket();
        if (client == null) return;
        client.close();
        client = null;
    }

    private @NotNull CompletableFuture<Void> connect(long delay) {
        synchronized (this) {
            closed = false;
        }

        CompletableFuture<Void> result = new CompletableFuture<>();
        scheduler.schedule(() -> {
            client.newWebSocketBuilder()
                    .header("Cookie", KEY_COOKIE + "=" + apiKey)
                    .buildAsync(apiUri, new WebSocketListener(instance))
                    .thenAccept(ws -> {
                        webSocket = ws;
                        reconnectAttempts.set(0);
                        Logs.info("Connected to WebSocket");
                        result.complete(null);
                    })
                    .exceptionally(e -> {
                        if (e instanceof CompletionException e1 && e1.getCause() != null) {
                            e = e1.getCause();
                        }

                        if (e instanceof IllegalArgumentException) {
                            Logs.error("Failed to connect to WebSocket! Fix the config.yml file, and use a \"/countingmc reload\" command or restart the server.", e);
                            close();
                            result.complete(null);
                            return null;
                        }
                        Logs.error("Failed to connect to WebSocket: " + e.getClass().getName() + ": " + e.getMessage() + ". Scheduling a reconnect...");
                        scheduleReconnect();
                        result.complete(null);
                        return null;
                    });
        }, delay, TimeUnit.SECONDS);
        return result;
    }

    private void closeWebSocket() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        reconnectAttempts.set(0);
        if (webSocket == null) return;
        if (webSocket.isOutputClosed()) return;

        synchronized (this) {
            if (closed) return;
            closed = true;
        }

        try {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Closing")
                    .orTimeout(5, TimeUnit.SECONDS)
                    .whenComplete((result, e) -> {
                        if (e != null) {
                            Logs.error("Failed to gracefully close the WebSocket, aborting...", e);
                            webSocket.abort();
                        }
                    })
                    .join();
        } catch (Exception e) {
            Logs.error("Failed to close the WebSocket", e);
        }
    }

    public boolean isClosed() {
        synchronized (this) {
            return closed;
        }
    }
}
