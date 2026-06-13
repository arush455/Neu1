package dev.neu1.blazeprogress.client;

import com.mojang.brigadier.Command;
import dev.neu1.blazeprogress.config.ConfigManager;
import dev.neu1.blazeprogress.config.ModConfig;
import dev.neu1.blazeprogress.core.ComparisonResult;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/** {@code /blazeprogress} and sub-commands. Client-side only. */
public final class Commands {

    private Commands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
                dispatcher.register(literal("blazeprogress")
                        .executes(ctx -> {
                            report(ctx.getSource()::sendFeedback);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(literal("toggle").executes(ctx -> {
                            ModConfig cfg = ConfigManager.get();
                            cfg.overlayEnabled = !cfg.overlayEnabled;
                            ConfigManager.save();
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§6[BlazeProgress] §fOverlay " + (cfg.overlayEnabled ? "§aon" : "§coff")));
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(literal("details").executes(ctx -> {
                            ModConfig cfg = ConfigManager.get();
                            cfg.showDetails = !cfg.showDetails;
                            ConfigManager.save();
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§6[BlazeProgress] §fDetailed view " + (cfg.showDetails ? "§aon" : "§coff")));
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(literal("reload").executes(ctx -> {
                            ConfigManager.load();
                            ctx.getSource().sendFeedback(Text.literal(
                                    "§6[BlazeProgress] §fConfig reloaded."));
                            return Command.SINGLE_SUCCESS;
                        }))));
    }

    private interface Feedback {
        void send(Text text);
    }

    private static void report(Feedback feedback) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ComparisonResult res = ProgressHud.currentComparison(mc);
        for (String line : res.toChatLines()) {
            feedback.send(Text.literal(line));
        }
    }
}
