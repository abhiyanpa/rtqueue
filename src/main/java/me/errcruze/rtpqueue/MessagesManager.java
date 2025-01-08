package me.errcruze.rtpqueue;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessagesManager {
    private final Rtpqueue plugin;
    private FileConfiguration messages;
    private File messagesFile;
    private final Pattern hexPattern = Pattern.compile("#[a-fA-F0-9]{6}");
    private String prefix;

    public MessagesManager(Rtpqueue plugin) {
        this.plugin = plugin;
        setupMessages();
    }

    private void setupMessages() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = colorize(messages.getString("prefix", "&8[&bRTPQueue&8] &r"));
    }

    public void reloadMessages() {
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = colorize(messages.getString("prefix", "&8[&bRTPQueue&8] &r"));
    }

    public void saveMessages() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml!");
            e.printStackTrace();
        }
    }

    public String getMessage(String path) {
        String message = messages.getString(path);
        if (message == null) {
            return "Missing message: " + path;
        }

        // Replace prefix first
        message = message.replace("{prefix}", prefix);

        return colorize(message);
    }

    public String getMessage(String path, Player player, Object... replacements) {
        String message = getMessage(path);

        // Replace player-specific placeholders
        if (player != null) {
            message = message.replace("{player}", player.getName())
                    .replace("{world}", player.getWorld().getName());
        }

        // Replace additional placeholders
        if (replacements != null && replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                message = message.replace(String.valueOf(replacements[i]),
                        String.valueOf(replacements[i + 1]));
            }
        }

        return colorize(message);
    }

    private String colorize(String message) {
        if (message == null) return "";

        // Convert hex colors (e.g., #FF5555)
        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            String color = message.substring(matcher.start(), matcher.end());
            message = message.replace(color, net.md_5.bungee.api.ChatColor.of(color).toString());
        }

        // Convert & color codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Get raw FileConfiguration if needed
    public FileConfiguration getMessages() {
        return messages;
    }

    // Get prefix
    public String getPrefix() {
        return prefix;
    }

    // Direct title methods for cleaner code
    public String getTitle(String path) {
        String title = messages.getString("titles." + path + ".title");
        if (title == null) {
            return "Missing title: " + path;
        }
        return colorize(title);
    }

    public String getSubtitle(String path) {
        String subtitle = messages.getString("titles." + path + ".subtitle");
        if (subtitle == null) {
            return "Missing subtitle: " + path;
        }
        return colorize(subtitle);
    }
}