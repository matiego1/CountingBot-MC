package me.matiego.countingmc.commands;

import me.matiego.countingmc.Main;
import me.matiego.countingmc.utils.Command;
import me.matiego.countingmc.utils.Pair;
import me.matiego.countingmc.utils.Utils;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class LinkDiscordCommand extends Command {
    public LinkDiscordCommand(@NotNull Main instance) {
        super(instance);
    }

    private final int VERIFICATION_CODE_LENGTH = 10;

    @Override
    public @NotNull String getCommandName() {
        return "linkdiscord";
    }

    @Override
    public int onCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length != 0) return -1;

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Utils.getComponentByString("&cThis command can only be run by players."));
            return 0;
        }

        UUID uuid = player.getUniqueId();
        Map<String, Pair<UUID, Long>> codes = instance.getVerificationCode();

        String code = getNewVerificationCode(codes);
        if (code == null) {
            sender.sendMessage(Utils.getComponentByString("&cFailed to generate a new verification code for you. Try again later."));
            return 5;
        }

        codes.entrySet().removeIf(e -> uuid.equals(e.getValue().getFirst()));
        codes.put(code, new Pair<>(uuid, Utils.now()));

        sender.sendMessage(Utils.getComponentByString("&bTo finish linking your accounts, use the &9/link-account&b command in Discord server with the code &9" + code + "&b. The code is only valid for 5 minutes!"));
        return 15;
    }

    private @Nullable String getNewVerificationCode(@NotNull Map<String, Pair<UUID, Long>> codes) {
        String code = RandomStringUtils.randomAlphanumeric(VERIFICATION_CODE_LENGTH);
        int attempts = 0;
        while (codes.containsKey(code)) {
            code = RandomStringUtils.randomAlphanumeric(VERIFICATION_CODE_LENGTH);
            if (attempts++ > 5000) return null;
        }
        return code;
    }
}
