package me.matiego.countingmc.commands;

import me.matiego.countingmc.Main;
import me.matiego.countingmc.utils.Command;
import me.matiego.countingmc.utils.Utils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CountingMcCommand extends Command {
    public CountingMcCommand(@NotNull Main instance) {
        super(instance);
    }

    @Override
    public @NotNull String getCommandName() {
        return "countingmc";
    }

    @Override
    public int onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length != 1) return -1;

        return switch (args[0].toLowerCase()) {
            case "version" -> {
                //noinspection deprecation
                sender.sendMessage(Utils.getComponentByString("&bCountingMC version: &9" + instance.getDescription().getVersion()));
                yield 0;
            }
            case "reload" -> {
                sender.sendMessage(Utils.getComponentByString("&bReloading..."));
                instance.reload();
                sender.sendMessage(Utils.getComponentByString("&bReloaded!"));
                yield 0;
            }
            case "enable" -> {
                setDepositAllowed(true, "enabled", sender);
                yield 0;
            }
            case "disable" -> {
                setDepositAllowed(false, "disabled", sender);
                yield 0;
            }
            default -> -1;
        };
    }

    private void setDepositAllowed(boolean newValue, @NotNull String string, @NotNull CommandSender sender) {
        if (instance.isDepositAllowed() == newValue) {
            sender.sendMessage(Utils.getComponentByString("&bDeposits are already " + string + "."));
            return;
        }
        instance.setDepositAllowed(newValue);
        sender.sendMessage(Utils.getComponentByString("&bSuccessfully " + string + " deposits."));
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        return List.of("reload", "enable", "disable");
    }
}
