package me.matiego.countingmc.utils;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Map;

@Getter
public class Response {
    public Response(int statusCode, @NotNull String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    private final int statusCode;
    private final String message;

    public @NotNull String getAsJSON() {
        return new JSONObject(Map.of(
                "status-code", String.valueOf(statusCode),
                "message", message
        )).toString();
    }
}
