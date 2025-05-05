package me.matiego.countingmc.utils;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

@Getter
public class Response {
    public Response(int statusCode, @NotNull String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    @Setter
    private String id = null;
    private final int statusCode;
    private final String message;

    public @NotNull String getAsJSON() {
        JSONObject json = new JSONObject();
        json.put("id", String.valueOf(id)); // properly handle null
        json.put("status-code", statusCode);
        json.put("message", message);
        return json.toString();
    }
}
