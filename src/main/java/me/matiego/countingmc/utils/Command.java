package me.matiego.countingmc.utils;

import me.matiego.countingmc.Main;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class Command {
    public Command(@NotNull Main instance) {
        this.instance = instance;

        String commandName = getCommandName();
        command = instance.getCommand(commandName);
        if (command == null) {
            Logs.warning("The command /" + commandName + " does not exist in the plugin.yml file and cannot be registered.");
        }
    }

    protected final Main instance;
    private final PluginCommand command;

    public abstract @NotNull String getCommandName();

    public @Nullable PluginCommand getCommand() {
        return command;
    }

    public abstract int onCommand(@NotNull CommandSender sender, @NotNull String[] args);

    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
