package com.globalmessage.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GlobalMessageBungee extends Plugin {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("#([A-Fa-f0-9]{6})");

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private Configuration config;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        getProxy().getPluginManager().registerCommand(this, new GlobalCommandBungee(this));

        getLogger().info("GlobalMessage (Bungee/Waterfall) v" + getDescription().getVersion() + " by 4wizm abilitato!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GlobalMessage (Bungee/Waterfall) disabilitato.");
    }

    public Configuration getConfig() {
        return config;
    }

    public String getServerName(ProxiedPlayer sender) {
        String configured = config.getString("server-name", "");
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        try {
            if (sender != null && sender.getServer() != null && sender.getServer().getInfo() != null) {
                return sender.getServer().getInfo().getName();
            }
        } catch (Throwable ignored) {}
        try {
            return getProxy().getName();
        } catch (Throwable t) {
            return "Proxy";
        }
    }

    public String color(String text) {
        if (text == null) return "";

        Matcher m1 = HEX_PATTERN.matcher(text);
        StringBuilder b1 = new StringBuilder();
        while (m1.find()) {
            m1.appendReplacement(b1, ChatColor.of("#" + m1.group(1)).toString());
        }
        m1.appendTail(b1);
        text = b1.toString();

        Matcher m2 = HEX_PATTERN_2.matcher(text);
        StringBuilder b2 = new StringBuilder();
        while (m2.find()) {
            m2.appendReplacement(b2, ChatColor.of("#" + m2.group(1)).toString());
        }
        m2.appendTail(b2);
        text = b2.toString();

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String getMessage(String path) {
        String prefix = config.getString("prefix", "");
        String msg = config.getString("messages." + path, "");
        if (msg == null) msg = "";
        return color(prefix + msg);
    }

    public boolean isOnCooldown(ProxiedPlayer player) {
        int cd = config.getInt("cooldown", 0);
        if (cd <= 0) return false;
        if (player.hasPermission("globalmessage.bypass")) return false;

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long remaining = (last + (cd * 1000L)) - now;

        if (remaining > 0) {
            long seconds = remaining / 1000 + 1;
            player.sendMessage(getMessage("cooldown").replace("%time%", String.valueOf(seconds)));
            return true;
        }
        cooldowns.put(player.getUniqueId(), now);
        return false;
    }

    public void broadcast(String message) {
        for (ProxiedPlayer player : getProxy().getPlayers()) {
            player.sendMessage(message);
        }
    }
}