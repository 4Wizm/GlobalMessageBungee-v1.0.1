package com.globalmessage.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class GlobalCommandBungee extends Command {

    private final GlobalMessageBungee plugin;
    private static final String PERMISSION = "globalmessage.use.global";

    public GlobalCommandBungee(GlobalMessageBungee plugin) {
        super("global", PERMISSION, "gmsg", "gc");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(plugin.getMessage("no-permission"));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("usage"));
            return;
        }

        String message = String.join(" ", args);
        if (message.trim().isEmpty()) {
            sender.sendMessage(plugin.getMessage("empty"));
            return;
        }

        ProxiedPlayer proxiedSender = null;
        if (sender instanceof ProxiedPlayer) {
            proxiedSender = (ProxiedPlayer) sender;
            if (plugin.isOnCooldown(proxiedSender)) {
                return;
            }
        }

        String senderName = proxiedSender != null ? proxiedSender.getName() : "Console";
        String serverName = plugin.getServerName(proxiedSender);

        String layout = plugin.getConfig().getString("layout",
                "&c&lGLOBAL &7%player% &7[&7%server%&7]&7: %message%");

        String finalMessage = layout
                .replace("%server%", serverName)
                .replace("%player%", senderName)
                .replace("%message%", message);

        finalMessage = plugin.color(finalMessage);

        plugin.broadcast(finalMessage);
        plugin.getLogger().info(finalMessage);
    }
}