package me.matiego.countingmc.api;

import me.matiego.countingmc.Main;
import me.matiego.countingmc.utils.Pair;
import me.matiego.countingmc.utils.Response;
import me.matiego.countingmc.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.UUID;

public class LinkRoute {
    public LinkRoute(@NotNull Main instance) {
        this.instance = instance;
    }

    private final int CODE_VALID_SECONDS = 300;

    private final Main instance;

    public @NotNull Response handle(@NotNull JSONObject params) {
        String code = Utils.getString(params, "code");
        if (code == null) {
            return new Response(400, "Missing code");
        }

        Pair<UUID, Long> pair = instance.getVerificationCode().remove(code);
        if (pair == null) {
            return new Response(404, "Unknown verification code");
        }

        if (Utils.now() - pair.getSecond() > CODE_VALID_SECONDS * 1000L) {
            return new Response(408, "Verification code timed out");
        }

        JSONObject json = new JSONObject();
        json.put("uuid", pair.getFirst().toString());
        return new Response(200, json.toString());
    }
}
