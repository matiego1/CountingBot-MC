package me.matiego.countingmc;

import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import me.matiego.countingmc.utils.Command;
import me.matiego.countingmc.utils.Logs;
import me.matiego.countingmc.utils.Utils;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommandsHandler implements CommandExecutor, TabCompleter, Listener {
    public CommandsHandler(@NotNull Command... handlers) {
        for (Command handler : handlers) {
            PluginCommand command = handler.getCommand();
            if (command == null) return;
            command.setExecutor(this);
            command.setTabCompleter(this);
            commands.put(command.getName().toLowerCase(), handler);
        }
    }

    private final HashMap<String, Command> commands = new HashMap<>();
    private final HashMap<String, Long> cooldown = Utils.createLimitedSizeMap(500);

    @Getter(onMethod_ = {@Synchronized})
    @Setter(onMethod_ = {@Synchronized})
    private boolean enabled = false;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player && !isEnabled()) {
            player.sendMessage(Utils.getComponentByString("&cCommands are currently disabled. Try again later."));
            return true;
        }

        // get handler
        Command handler = commands.get(command.getName().toLowerCase());
        if (handler == null) {
            sender.sendMessage(Utils.getComponentByString("&cUnknown command."));
            return true;
        }

        // check cooldown
        if (sender instanceof Player player) {
            long time = getRemainingCooldown(command.getName(), player.getUniqueId());
            if (time > 0) {
                player.sendMessage(Utils.getComponentByString("&cYou can use this command again in " + Utils.parseMillisToString(time, false)));
                return true;
            }
        }

        // execute command
        try {
            int cooldown = handler.onCommand(sender, args);
            if (cooldown < 0) return false;
            if (sender instanceof Player player && cooldown > 0) {
                putCooldown(command.getName(), player.getUniqueId(), cooldown);
            }
            return true;
        } catch (Exception e) {
            sender.sendMessage(Utils.getComponentByString("&cAn unexpected error occurred. Try again later."));
            Logs.error("Failed to execute a command.", e);
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        return commands.get(command.getName()).onTabComplete(sender, args).stream()
                .filter(complete -> complete.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    private long getRemainingCooldown(@NotNull String command, @NotNull UUID uuid) {
        long now = Utils.now();
        return Math.max(0, cooldown.getOrDefault(command + "#" + uuid, now) - now);
    }

    private void putCooldown(@NotNull String command, @NotNull UUID uuid, int seconds) {
        cooldown.put(command + "#" + uuid, seconds * 1000L + Utils.now());
    }
}
