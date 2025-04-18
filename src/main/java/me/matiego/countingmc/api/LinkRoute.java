package me.matiego.countingmc.api;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.RequestTimeoutResponse;
import me.matiego.countingmc.Main;
import me.matiego.countingmc.utils.Pair;
import me.matiego.countingmc.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class LinkRoute {
    public LinkRoute(@NotNull Main instance) {
        this.instance = instance;
    }

    private final int CODE_VALID_SECONDS = 300;

    private final Main instance;

    public void link(@NotNull Context ctx) {
        String code = ctx.formParam("code");
        if (code == null) {
            throw new BadRequestResponse("Missing code");
        }

        Pair<UUID, Long> pair = instance.getVerificationCode().remove(code);
        if (pair == null) {
            throw new NotFoundResponse("Unknown verification code");
        }

        if (Utils.now() - pair.getSecond() > CODE_VALID_SECONDS * 1000L) {
            throw new RequestTimeoutResponse("Verification code timed out");
        }

        ctx.status(200).json(Map.of("uuid", pair.getFirst().toString()));
    }
}
