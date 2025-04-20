package me.matiego.countingmc;

import io.javalin.Javalin;
import io.javalin.community.ssl.SslPlugin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import me.matiego.countingmc.api.DepositRoute;
import me.matiego.countingmc.api.LinkRoute;
import me.matiego.countingmc.utils.Logs;
import me.matiego.countingmc.utils.json.GsonJsonMapper;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

// Based on https://github.com/servertap-io/servertap
public class WebServer {
    public WebServer(@NotNull Main instance, @NotNull FileConfiguration config) {
        this.instance = instance;

        this.tlsEnabled = config.getBoolean("tls.enabled", false);
        this.sni = config.getBoolean("tls.sni", false);
        this.keyStorePath = config.getString("tls.keystore", "keystore.jks");
        this.keyStorePassword = config.getString("tls.keystorePassword", "");
        this.authKey = config.getString("key", "change_me");
        this.corsOrigin = config.getStringList("corsOrigins");
        this.securePort = config.getInt("port", 4567);

        javalin = Javalin.create(c -> configureJavalin(c, instance));
        javalin.before(this::manageAccess);
    }

    private static final String KEY_HEADER = "key";

    private final Main instance;
    private final Javalin javalin;

    private final boolean tlsEnabled;
    private final boolean sni;
    private final String keyStorePath;
    private final String keyStorePassword;
    private final String authKey;
    private final List<String> corsOrigin;
    private final int securePort;

    private void configureJavalin(@NotNull JavalinConfig config, @NotNull Main main) {
        config.jsonMapper(new GsonJsonMapper());
        config.http.defaultContentType = "application/json";
        config.showJavalinBanner = false;

        configureTLS(config, main);
        configureCors(config);

        if ("change_me".equalsIgnoreCase(authKey)) {
            Logs.error("AUTH KEY IS SET TO DEFAULT \"change_me\"");
            Logs.error("CHANGE THE key IN THE config.yml FILE");
            Logs.error("FAILURE TO CHANGE THE KEY MAY RESULT IN SERVER COMPROMISE");
        }
    }

    private void manageAccess(@NotNull Context ctx) {
        String authHeader = ctx.header(KEY_HEADER);
        if (authHeader != null && Objects.equals(authHeader, authKey)) return;

        throw new UnauthorizedResponse("Unauthorized key, reference the key existing in config.yml");
    }

    private void configureTLS(@NotNull JavalinConfig config, @NotNull Main instance) {
        if (!tlsEnabled) {
            Logs.warning("TLS is not enabled.");
            return;
        }
        try {
            final String fullKeystorePath = instance.getDataFolder().getAbsolutePath() + File.separator + keyStorePath;

            if (Files.exists(Paths.get(fullKeystorePath))) {
                SslPlugin plugin = new SslPlugin(conf -> {
                    conf.keystoreFromPath(fullKeystorePath, keyStorePassword);
                    conf.http2 = false;
                    conf.insecure = false;
                    conf.secure = true;
                    conf.securePort = securePort;
                    conf.sniHostCheck = sni;
                });
                config.registerPlugin(plugin);
                Logs.info("TLS is enabled.");
            } else {
                Logs.warning(String.format("TLS is enabled but %s doesn't exist. TLS disabled.", fullKeystorePath));
            }
        } catch (Exception e) {
            Logs.error("Error while enabling TLS: " + e.getMessage());
            Logs.warning("TLS is not enabled.");
        }
    }

    private void configureCors(@NotNull JavalinConfig config) {
        config.bundledPlugins.enableCors(cors -> cors.addRule(corsConfig -> {
            if (corsOrigin.contains("*")) {
                Logs.info("Enabling CORS for *");
                corsConfig.anyHost();
            } else {
                corsOrigin.forEach(origin -> {
                    Logs.info(String.format("Enabling CORS for %s", origin));
                    corsConfig.allowHost(origin);
                });
            }
        }));
    }

    public void addRoutes() {
        javalin.post("link", new LinkRoute(instance)::link);
        javalin.post("deposit", new DepositRoute(instance)::deposit);
    }

    public void start(int port) {
        javalin.start(port);
    }

    public void stop() {
        javalin.stop();
    }
}
