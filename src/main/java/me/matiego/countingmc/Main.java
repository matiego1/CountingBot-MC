package me.matiego.countingmc;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import me.matiego.countingmc.commands.CountingMcCommand;
import me.matiego.countingmc.commands.LinkDiscordCommand;
import me.matiego.countingmc.utils.Logs;
import me.matiego.countingmc.utils.Pair;
import me.matiego.countingmc.utils.Utils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitWorker;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public final class Main extends JavaPlugin {
    public Main() {
        instance = this;
    }

    @Getter private static Main instance;
    @Getter private long startTime;

    @Getter private Economy economy;
    private CommandsHandler commandsHandler;
    private WebServer webServer;

    @Getter(onMethod_ = {@Synchronized})
    @Setter(onMethod_ = {@Synchronized})
    private boolean depositAllowed = false;
    @Getter private Map<String, Pair<UUID, Long>> verificationCode = Collections.synchronizedMap(Utils.createLimitedSizeMap(500));

    @Override
    public void onEnable() {
        startTime = Utils.now();

        // Check Bukkit version
        if (!Bukkit.getBukkitVersion().equals("1.20.2-R0.1-SNAPSHOT")) {
            Logs.error("This plugin isn't compatible with this version: " + Bukkit.getBukkitVersion() + ".");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Check if the server is PaperMC
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
        } catch (ClassNotFoundException e) {
            Logs.error("This plugin is only compatible with PaperMC!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Save config file
        try {
            saveDefaultConfig();
        } catch (Exception e) {
            Logs.error("Failed to load a config file", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        commandsHandler = new CommandsHandler(
                new LinkDiscordCommand(instance),
                new CountingMcCommand(instance)
        );

        // Setup economy
        if (!setupEconomy()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Setup web server
        Logs.info("Starting a web server...");
        try {
            setupWebServer();
        } catch (Exception e) {
            Logs.error("Failed to start a web server", e);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        commandsHandler.setEnabled(true);
        setDepositAllowed(true);

        Logs.info("Plugin enabled in " + (Utils.now() - getStartTime()) + " ms!");
    }

    public boolean setupEconomy() {
        RegisteredServiceProvider<Economy> service = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (service == null) {
            Logs.error("No economy providers detected");
            return false;
        }

        Logs.info("Hooked economy provider: " + service.getProvider().getName());
        economy = service.getProvider();
        return true;
    }

    public void setupWebServer() {
        webServer = new WebServer(this, getConfig());
        webServer.start(getConfig().getInt("port", 4567));
        webServer.addRoutes();
    }

    public void reload() {
        long time = Utils.now();
        Logs.info("Reloading the plugin...");

        // Disable
        setDepositAllowed(false);
        commandsHandler.setEnabled(false);

        Logs.info("Stopping the web server...");
        webServer.stop();

        // Enable
        reloadConfig();
        setupEconomy();

        Logs.info("Starting a web server...");
        try {
            setupWebServer();
        } catch (Exception e) {
            Logs.error("Failed to start a web server.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        commandsHandler.setEnabled(true);
        setDepositAllowed(true);

        Logs.info("Plugin reloaded in " + (Utils.now() - time) + " ms!");
    }

    @Override
    public void onDisable() {
        long time = Utils.now();

        setDepositAllowed(false);
        if (commandsHandler != null) commandsHandler.setEnabled(false);
        HandlerList.unregisterAll(this);

        Logs.info("Stopping the web server...");
        if (webServer != null) webServer.stop();

        // End all tasks
        Bukkit.getAsyncScheduler().cancelTasks(this);
        Bukkit.getScheduler().cancelTasks(this);
        for (BukkitWorker task : Bukkit.getScheduler().getActiveWorkers()) {
            if (task.getOwner().equals(this)) {
                Logs.error("A task with id " + task.getTaskId() + " has not been canceled. Interrupting...");
                try {
                    task.getThread().interrupt();
                } catch (Exception ignored) {}
            }
        }

        Logs.info("Plugin disabled in " + (Utils.now() - time) + " ms!");
        instance = null;
    }
}
