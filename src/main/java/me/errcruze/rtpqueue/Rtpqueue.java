package me.errcruze.rtpqueue;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Rtpqueue extends JavaPlugin {
    private Map<UUID, Long> cooldowns = new HashMap<>();
    private List<Player> queue = new ArrayList<>();
    private Map<UUID, Boolean> searching = new HashMap<>();

    private ConfigManager configManager;
    private MessagesManager messagesManager;

    @Override
    public void onEnable() {
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.messagesManager = new MessagesManager(this);

        // Register commands
        getCommand("rtpqueue").setExecutor(new RTPQueueCommand());
        getCommand("rtpqclear").setExecutor(new RTPQueueClearCommand());
        getCommand("rtpqreload").setExecutor(new RTPQueueReloadCommand());

        // Schedule automatic queue clear if enabled
        if (configManager.isAutoQueueClear()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    clearQueue();
                    if (configManager.isDebugEnabled()) {
                        getLogger().info("Auto-cleared RTP Queue");
                    }
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        if (player.hasPermission("rtpq.notify")) {
                            player.sendMessage(messagesManager.getMessage("messages.queue.auto-cleared"));
                        }
                    });
                }
            }.runTaskTimer(this, configManager.getClearInterval() * 20L, configManager.getClearInterval() * 20L);
        }

        getLogger().info("RTPQueue has been enabled!");
    }

    @Override
    public void onDisable() {
        clearQueue();
        getLogger().info("RTPQueue has been disabled!");
    }

    private void playSound(Player player, String soundPath) {
        if (!configManager.isSoundsEnabled()) return;

        try {
            Sound sound = Sound.valueOf(configManager.getSoundName(soundPath));
            float volume = (float) configManager.getSoundVolume(soundPath);
            float pitch = (float) configManager.getSoundPitch(soundPath);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            if (configManager.isDebugEnabled()) {
                getLogger().warning("Invalid sound name in configuration: " + soundPath);
            }
        }
    }

    private void showTeleportTitle(Player player, Player partner) {
        if (!configManager.isTitlesEnabled()) return;

        String title = messagesManager.getMessage("titles.teleport-success.title", player);
        String subtitle = messagesManager.getMessage("titles.teleport-success.subtitle", player, "{partner}", partner.getName());

        int fadeIn = configManager.getTitleFadeIn();
        int stay = configManager.getTitleStay();
        int fadeOut = configManager.getTitleFadeOut();

        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    private class RTPQueueCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(messagesManager.getMessage("messages.general.player-only"));
                return true;
            }

            Player player = (Player) sender;

            // Check disabled worlds
            if (configManager.isWorldDisabled(player.getWorld().getName())) {
                player.sendMessage(messagesManager.getMessage("messages.errors.world-disabled"));
                playSound(player, "sounds.error");
                return true;
            }

            // Check cooldown (bypass with permission)
            if (configManager.isCooldownEnabled() && !player.hasPermission(configManager.getCooldownBypassPermission())) {
                if (cooldowns.containsKey(player.getUniqueId())) {
                    long timeLeft = ((cooldowns.get(player.getUniqueId()) / 1000) + configManager.getCooldownTime())
                            - (System.currentTimeMillis() / 1000);
                    if (timeLeft > 0) {
                        player.sendMessage(messagesManager.getMessage("messages.cooldown.in-cooldown", player, "{time}", String.valueOf(timeLeft)));
                        playSound(player, "sounds.error");
                        return true;
                    }
                }
            } else if (player.hasPermission(configManager.getCooldownBypassPermission())) {
                player.sendMessage(messagesManager.getMessage("messages.cooldown.bypass"));
            }

            // Update cooldown
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

            // Check if player is already in queue
            if (searching.containsKey(player.getUniqueId()) && searching.get(player.getUniqueId())) {
                player.sendMessage(messagesManager.getMessage("messages.queue.already-in-queue"));
                playSound(player, "sounds.error");
                return true;
            }

            // Check queue size limit
            if (queue.size() >= configManager.getMaxQueueSize()) {
                player.sendMessage(messagesManager.getMessage("messages.errors.queue-full"));
                playSound(player, "sounds.error");
                return true;
            }

            // Add player to queue
            queue.add(player);
            searching.put(player.getUniqueId(), true);
            player.sendMessage(messagesManager.getMessage("messages.queue.joined", player,
                    "{queue_size}", String.valueOf(queue.size()),
                    "{queue_needed}", String.valueOf(configManager.getMaxQueueSize())));

            playSound(player, "sounds.join-queue");

            // Check if we have enough players
            if (queue.size() >= configManager.getMaxQueueSize()) {
                // Send message to all queued players
                queue.forEach(p -> p.sendMessage(messagesManager.getMessage("messages.teleport.partner-found")));

                // Schedule teleport with delay
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        teleportPlayers();
                    }
                }.runTaskLater(Rtpqueue.this, 60L); // Use Rtpqueue.this to refer to the outer plugin class
            }

            return true;
        }
    }

    private class RTPQueueClearCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("rtpq.clear")) {
                sender.sendMessage(messagesManager.getMessage("messages.general.no-permission"));
                playSound(sender instanceof Player ? (Player) sender : null, "sounds.error");
                return true;
            }

            clearQueue();
            sender.sendMessage(messagesManager.getMessage("messages.queue.queue-cleared"));
            return true;
        }
    }

    private class RTPQueueReloadCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("rtpq.reload")) {
                sender.sendMessage(messagesManager.getMessage("messages.general.no-permission"));
                playSound(sender instanceof Player ? (Player) sender : null, "sounds.error");
                return true;
            }

            try {
                configManager.loadConfig();
                messagesManager.reloadMessages();
                sender.sendMessage(messagesManager.getMessage("messages.general.reload-success"));
            } catch (Exception e) {
                sender.sendMessage(messagesManager.getMessage("messages.general.reload-failed"));
                getLogger().severe("Failed to reload configuration: " + e.getMessage());
            }
            return true;
        }
    }

    private void clearQueue() {
        queue.forEach(player -> {
            if (player.isOnline()) {
                player.sendMessage(messagesManager.getMessage("messages.queue.left-queue"));
                playSound(player, "sounds.leave-queue");
            }
        });
        queue.clear();
        searching.clear();
    }

    private void teleportPlayers() {
        if (queue.size() < configManager.getMaxQueueSize()) return;

        List<Player> playersToTeleport = new ArrayList<>(queue.subList(0, configManager.getMaxQueueSize()));

        // Check if all players are still online
        for (Player player : playersToTeleport) {
            if (!player.isOnline()) {
                playersToTeleport.forEach(p -> {
                    if (p.isOnline()) {
                        p.sendMessage(messagesManager.getMessage("messages.errors.player-offline"));
                        playSound(p, "sounds.error");
                    }
                });
                clearQueue();
                return;
            }
        }

        World world = Bukkit.getWorld(configManager.getWorldName());
        if (world == null) {
            playersToTeleport.forEach(p -> {
                p.sendMessage(messagesManager.getMessage("messages.teleport.world-not-found",
                        p, "{world}", configManager.getWorldName()));
                playSound(p, "sounds.error");
            });
            clearQueue();
            return;
        }

        // Find safe location
        Location safeLoc = findSafeLocation(world);
        if (safeLoc == null) {
            playersToTeleport.forEach(p -> {
                p.sendMessage(messagesManager.getMessage("messages.teleport.failed"));
                playSound(p, "sounds.error");
            });
            clearQueue();
            return;
        }

// Teleport players
        for (int i = 0; i < playersToTeleport.size(); i++) {
            Player player = playersToTeleport.get(i);
            Player partner = playersToTeleport.get(i == 0 ? 1 : 0); // Still need partner for title

            player.teleport(safeLoc);
            player.sendMessage(messagesManager.getMessage("messages.teleport.success", player,
                    "{x}", String.valueOf(safeLoc.getBlockX()),
                    "{y}", String.valueOf(safeLoc.getBlockY()),
                    "{z}", String.valueOf(safeLoc.getBlockZ())));

            showTeleportTitle(player, partner); // Keep titles with partner name
            playSound(player, "sounds.teleport-success");
        }

        // Log if debug is enabled
        if (configManager.isDebugEnabled() && configManager.isDebugLogLocations()) {
            getLogger().info(String.format("Teleported %d players to %s, %d, %d, %d",
                    playersToTeleport.size(), world.getName(),
                    safeLoc.getBlockX(), safeLoc.getBlockY(), safeLoc.getBlockZ()));
        }

        // Clear queue after successful teleport
        queue.clear();
        searching.clear();
    }

    private Location findSafeLocation(World world) {
        Random random = new Random();
        for (int attempts = 0; attempts < configManager.getMaxAttempts(); attempts++) {
            int x = random.nextInt(configManager.getMaxX() - configManager.getMinX() + 1) + configManager.getMinX();
            int z = random.nextInt(configManager.getMaxZ() - configManager.getMinZ() + 1) + configManager.getMinZ();

            Location loc = world.getHighestBlockAt(x, z).getLocation();

            // Check Y bounds
            if (loc.getY() < configManager.getMinY() || loc.getY() > configManager.getMaxY()) {
                continue;
            }

            Block block = loc.getBlock();
            Block above = block.getRelative(0, 1, 0);
            Block below = block.getRelative(0, -1, 0);

            if (above.isEmpty() &&
                    above.getRelative(0, 1, 0).isEmpty() &&
                    !below.isEmpty() &&
                    !below.isLiquid()) {
                return loc.add(0.5, 1, 0.5);
            }
        }
        return null;
    }
}