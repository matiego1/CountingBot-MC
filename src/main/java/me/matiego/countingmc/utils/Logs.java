package me.matiego.countingmc.utils;

import me.matiego.countingmc.Main;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class Logs {
    private static final Logger logger = Main.getInstance().getLogger();

    public static void info(@NotNull String message) {
        logger.info(message);
    }

    public static void warning(@NotNull String message) {
        warning(message, null);
    }

    public static void warning(@NotNull String message, @Nullable Throwable throwable) {
        logger.warning(message);

        if (throwable == null) return;
        logThrowable(throwable, logger::warning);
    }

    public static void error(@NotNull String message) {
        error(message, null);
    }

    public static void error(@NotNull String message, @Nullable Throwable throwable) {
        logger.severe(message);

        if (throwable == null) return;
        logThrowable(throwable, logger::severe);
    }

    private static void logThrowable(@NotNull Throwable throwable, @NotNull Consumer<String> logLine) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        for (String line : stringWriter.toString().split("\n")) {
            logLine.accept(line);
        }
    }
}
