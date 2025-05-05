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

    private final static int UNAUTHORIZED_CODE = 3000;
    private final Main instance;
    private final DepositRoute deposit;
    private final LinkRoute link;

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (statusCode == UNAUTHORIZED_CODE) {
            Logs.error("WebSocket is closed. (Code: " + statusCode + "; Reason: " + reason + ") Fix the config.yml file, and use a \"/countingmc reload\" command or restart the server.");
            return null;
        }

        if (instance.getWebSocketClient().isClosed()) {
            Logs.info("WebSocket is closed. (Code: " + statusCode + "; Reason: " + reason + ")");
            return null;
        }

        Logs.warning("WebSocket is closed. (Code: " + statusCode + "; Reason: " + reason + ") Reconnecting...");
        instance.getWebSocketClient().scheduleReconnect(statusCode == WebSocket.NORMAL_CLOSURE ? 10 : 0);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        Logs.error("An WebSocket error occurred", error);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try {
            Response response = getResponse(String.valueOf(data));
            webSocket.sendText(response.getAsJSON(), true);
        } catch (Exception e) {
            Logs.error("Failed to handle a WebSocket message", e);
        }

        webSocket.request(1);
        return null;
    }

    private @NotNull Response getResponse(@Nullable String data) {
        try {
            if (data == null) return new Response(400, "Empty text data");

            JSONObject json = new JSONObject(data);
            String id = json.getString("id");

            Response response = switch (json.getString("path")) {
                case "link" -> link.handle(json.getJSONObject("params"));
                case "deposit" -> deposit.handle(json.getJSONObject("params"));
                default -> new Response(400, "Unknown request path");
            };
            response.setId(id);
            return response;
        } catch (Exception e) {
            Logs.error("Failed to get a response to a WebSocket message", e);
            return new Response(500, e.getMessage());
        }
    }
}
