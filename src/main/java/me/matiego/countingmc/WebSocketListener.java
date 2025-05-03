package me.matiego.countingmc;


import me.matiego.countingmc.api.DepositRoute;
import me.matiego.countingmc.api.LinkRoute;
import me.matiego.countingmc.utils.Logs;
import me.matiego.countingmc.utils.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class WebSocketListener implements WebSocket.Listener {
    public WebSocketListener(@NotNull Main instance) {
        this.instance = instance;
        deposit = new DepositRoute(instance);
        link = new LinkRoute(instance);
    }

    private final Main instance;
    private final DepositRoute deposit;
    private final LinkRoute link;

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (statusCode == WebSocket.NORMAL_CLOSURE) return null;
        Logs.info("WebSocket is closed. Reconnecting... Status code:" + statusCode + ", Reason: " + reason);
        instance.getWebSocketClient().scheduleReconnect();
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        Logs.error("An error occurred", error);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try {
            Response response = getResponse(data);
            webSocket.sendText(response.getAsJSON(), true);
        } catch (Exception e) {
            Logs.error("Failed to handle a WebSocket message", e);
        }

        webSocket.request(1);
        return null;
    }

    private @NotNull Response getResponse(@Nullable CharSequence data) {
        try {
            if (data == null) return new Response(400, "Empty text data");
            JSONObject json = new JSONObject(data);

            return switch (json.getString("path")) {
                case "link" -> link.handle(json.getJSONObject("params"));
                case "deposit" -> deposit.handle(json.getJSONObject("params"));
                default -> new Response(404, "Unknown request path");
            };
        } catch (Exception e) {
            Logs.error("Failed to get response to a WebSocket message", e);
            return new Response(500, e.getMessage());
        }
    }
}
