package me.matiego.countingmc.api;

import me.matiego.countingmc.Main;
import me.matiego.countingmc.utils.Logs;
import me.matiego.countingmc.utils.Response;
import me.matiego.countingmc.utils.Utils;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.UUID;

public class DepositRoute {
    public DepositRoute(@NotNull Main instance) {
        this.instance = instance;
    }

    private final int MAX_DEPOSIT_AMOUNT = 1000 * 1000 * 1000;

    private final Main instance;

    public @NotNull Response handle(@NotNull JSONObject params) {
        String uuidParam = Utils.getString(params, "uuid");
        String amountParam = Utils.getString(params, "amount");

        if (uuidParam == null || amountParam == null) {
            return new Response(400, "Missing uuid and/or amount");
        }

        Economy economy = instance.getEconomy();
        if (economy == null) {
            return new Response(503, "Economy provider is not available.");
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidParam);
        } catch (IllegalArgumentException e) {
            return new Response(400, "Invalid UUID");
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (!player.hasPlayedBefore()) {
            return new Response(404, "This player has never played on this server");
        }

        double amount;
        try {
            amount = Double.parseDouble(amountParam);
        } catch (NumberFormatException e) {
            return new Response(400, "Invalid amount");
        }
        if (amount <= 0) {
            return new Response(400, "Amount must be a value greater than zero");
        }
        if (amount > MAX_DEPOSIT_AMOUNT) {
            return new Response(400, "Amount must be a value smaller than " + MAX_DEPOSIT_AMOUNT);
        }

        if (!instance.isDepositAllowed()) {
            return new Response(403, "Deposits are currently disabled");
        }

        FileConfiguration config = instance.getConfig();
        if (config.getStringList("blocked-uuids").contains(uuid.toString())) {
            return new Response(403, "This uuid is blocked");
        }

        EconomyResponse response = economy.depositPlayer(player, amount);
        if (response.type != EconomyResponse.ResponseType.SUCCESS) {
            return new Response(500, response.errorMessage);
        }

        if (config.getBoolean("log-every-deposit", true)) {
            String name = uuid.toString();
            if (player.getName() != null) {
                name = player.getName() + " [" + uuid + "]";
            }
            Logs.info("Deposited " + economy.format(amount) + " into " + name + " account.");
        }

        return new Response(200, "success");
    }
}
