package me.matiego.countingmc.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Utils {
    public static long now() {
        return System.currentTimeMillis();
    }

    @Contract(value = "_ -> new", pure = true)
    public static <K, V> @NotNull HashMap<K, V> createLimitedSizeMap(int maxEntries) {
        return new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    public static @NotNull Component getComponentByString(@NotNull String string) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(string).asComponent();
    }

    public static @NotNull String parseMillisToString(long time, boolean useMilliseconds) {
        time = Math.round((double) time / (useMilliseconds ? 1 : 1000));
        int x = useMilliseconds ? 1000 : 1;
        String result = "";

        //days
        int d = (int) (time / (3600 * 24 * x));
        time -= (long) d * 3600 * 24 * x;
        if (d != 0) result += d + "d ";
        //hours
        int h = (int) (time / (3600 * x));
        time -= (long) h * 3600 * x;
        if (h != 0) result += h + "h ";
        //minutes
        int m = (int) (time / (60 * x));
        time -= (long) m * 60 * x;
        if (m != 0) result += m + "m ";
        //seconds
        int s = (int) (time / x);
        time -= (long) s * x;
        if (s != 0) result += s + "s ";
        //milliseconds
        int ms = (int) (time);
        if (ms != 0) result += ms + "ms ";

        if (result.isEmpty()) return useMilliseconds ? "0ms" : "0s";
        return result.substring(0, result.length() - 1);
    }
}
