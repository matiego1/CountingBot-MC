package me.matiego.countingmc.api;

import io.javalin.http.*;
import me.matiego.countingmc.Main;
import me.matiego.countingmc.utils.Logs;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DepositRoute {
    public DepositRoute(@NotNull Main instance) {
        this.instance = instance;
    }

    private final int MAX_DEPOSIT_AMOUNT = 1000 * 1000 * 1000;

    private final Main instance;

    public void deposit(@NotNull Context ctx) {
        String uuidParam = ctx.formParam("uuid");
        String amountParam = ctx.formParam("amount");

        if (uuidParam == null || amountParam == null) {
            throw new BadRequestResponse("Missing uuid and/or amount");
        }

        Economy economy = instance.getEconomy();
        if (economy == null) {
            throw new ServiceUnavailableResponse("Economy provider is not available.");
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidParam);
        } catch (IllegalArgumentException e) {
            throw new BadRequestResponse("Invalid UUID");
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (!player.hasPlayedBefore()) {
            throw new NotFoundResponse("This player has never played on this server");
        }

        double amount;
        try {
            amount = Double.parseDouble(amountParam);
        } catch (NumberFormatException e) {
            throw new BadRequestResponse("Invalid amount");
        }
        if (amount <= 0) {
            throw new BadRequestResponse("Amount must be a value greater than zero");
        }
        if (amount > MAX_DEPOSIT_AMOUNT) {
            throw new BadRequestResponse("Amount must be a value smaller than " + MAX_DEPOSIT_AMOUNT);
        }

        if (!instance.isDepositAllowed()) {
            throw new ForbiddenResponse("Deposits are currently disabled");
        }

        FileConfiguration config = instance.getConfig();
        if (config.getStringList("blocked-uuids").contains(uuid.toString())) {
            throw new ForbiddenResponse("This uuid is blocked");
        }

        EconomyResponse response = economy.depositPlayer(player, amount);
        if (response.type != EconomyResponse.ResponseType.SUCCESS) {
            throw new InternalServerErrorResponse(response.errorMessage);
        }

        if (config.getBoolean("log-every-deposit", true)) {
            String name = uuid.toString();
            if (player.getName() != null) {
                name = player.getName() + " [" + uuid + "]";
            }
            Logs.info("Deposited " + economy.format(amount) + " into " + name + " account.");
        }

        ctx.status(200).json("success");
    }
}
