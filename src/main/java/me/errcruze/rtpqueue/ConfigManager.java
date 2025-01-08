package me.errcruze.rtpqueue;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

public class ConfigManager {
    private final Rtpqueue plugin;
    private FileConfiguration config;

    // Cooldown settings
    private boolean cooldownEnabled;
    private int cooldownTime;
    private String cooldownBypassPermission;

    // Teleport settings
    private String worldName;
    private int minX, maxX, minZ, maxZ;
    private int maxAttempts;
    private int minY, maxY;
    private List<String> disabledWorlds;

    // Queue settings
    private boolean autoQueueClear;
    private int clearInterval;
    private int maxQueueSize;

    // Sound settings
    private boolean soundsEnabled;

    // Title settings
    private boolean titlesEnabled;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;

    // Debug settings
    private boolean debugEnabled;
    private boolean debugLogLocations;

    public ConfigManager(Rtpqueue plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Load cooldown settings
        cooldownEnabled = config.getBoolean("cooldown.enabled", true);
        cooldownTime = config.getInt("cooldown.time", 50);
        cooldownBypassPermission = config.getString("cooldown.bypass-permission", "rtpq.cooldown.bypass");

        // Load teleport settings
        worldName = config.getString("teleport.world", "world");
        minX = config.getInt("teleport.min-x", -2000);
        maxX = config.getInt("teleport.max-x", 2000);
        minZ = config.getInt("teleport.min-z", -2000);
        maxZ = config.getInt("teleport.max-z", 2000);
        maxAttempts = config.getInt("teleport.safe-location.max-attempts", 10);
        minY = config.getInt("teleport.safe-location.min-y", 0);
        maxY = config.getInt("teleport.safe-location.max-y", 256);
        disabledWorlds = config.getStringList("teleport.disabled-worlds");

        // Load queue settings
        autoQueueClear = config.getBoolean("queue.auto-clear", true);
        clearInterval = config.getInt("queue.clear-interval", 120);
        maxQueueSize = config.getInt("queue.max-queue-size", 2);

        // Load sound settings
        soundsEnabled = config.getBoolean("sounds.enabled", true);

        // Load title settings
        titlesEnabled = config.getBoolean("titles.teleport-success.enabled", true);
        titleFadeIn = config.getInt("titles.teleport-success.fade-in", 10);
        titleStay = config.getInt("titles.teleport-success.stay", 70);
        titleFadeOut = config.getInt("titles.teleport-success.fade-out", 20);

        // Load debug settings
        debugEnabled = config.getBoolean("debug.enabled", false);
        debugLogLocations = config.getBoolean("debug.log-locations", false);
    }

    // Cooldown getters
    public boolean isCooldownEnabled() { return cooldownEnabled; }
    public int getCooldownTime() { return cooldownTime; }
    public String getCooldownBypassPermission() { return cooldownBypassPermission; }

    // Teleport getters
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public int getMaxAttempts() { return maxAttempts; }
    public int getMinY() { return minY; }
    public int getMaxY() { return maxY; }
    public boolean isWorldDisabled(String worldName) {
        return disabledWorlds.contains(worldName);
    }

    // Queue getters
    public boolean isAutoQueueClear() { return autoQueueClear; }
    public int getClearInterval() { return clearInterval; }
    public int getMaxQueueSize() { return maxQueueSize; }

    // Sound configuration getters
    public boolean isSoundsEnabled() { return soundsEnabled; }

    public String getSoundName(String path) {
        return config.getString(path + ".sound", "BLOCK_NOTE_BLOCK_PLING");
    }

    public double getSoundVolume(String path) {
        return config.getDouble(path + ".volume", 1.0);
    }

    public double getSoundPitch(String path) {
        return config.getDouble(path + ".pitch", 1.0);
    }

    // Title configuration getters
    public boolean isTitlesEnabled() { return titlesEnabled; }
    public int getTitleFadeIn() { return titleFadeIn; }
    public int getTitleStay() { return titleStay; }
    public int getTitleFadeOut() { return titleFadeOut; }

    // Debug getters
    public boolean isDebugEnabled() { return debugEnabled; }
    public boolean isDebugLogLocations() { return debugLogLocations; }

    // Get raw config for direct access if needed
    public FileConfiguration getConfig() {
        return config;
    }
}