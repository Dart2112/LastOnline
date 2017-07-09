/*
 * Copyright 2017 Benjamin Martin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lapismc.lastonline;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.JustNow;
import org.ocpsoft.prettytime.units.Millisecond;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public final class LastOnline extends JavaPlugin implements Listener {

    private PrettyTime pt = new PrettyTime(Locale.ENGLISH);
    private LapisUpdater updater;
    private File usersFile = new File(getDataFolder(), "users.yml");
    private File messagesFile = new File(getDataFolder(), "messages.yml");
    private YamlConfiguration users;
    private YamlConfiguration messages;
    private HashMap<UUID, Long> userMap = new HashMap<>();
    private Logger logger = Bukkit.getLogger();
    private String PrimaryColor = "&6";
    private String SecondaryColor = "&c";

    @Override
    public void onEnable() {
        configs();
        loadUsers();
        new Metrics(this);
        updater = new LapisUpdater(this, "LastOnline", "Dart2112", "LastOnline", "master");
        update();
        pt.removeUnit(JustNow.class);
        pt.removeUnit(Millisecond.class);
        Bukkit.getPluginManager().registerEvents(this, this);
        logger.info("LastOnline v." + getDescription().getVersion() + " has been enabled!");
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> saveUsers(), 20 * 60 * 10, 20 * 60 * 10);
    }

    @Override
    public void onDisable() {
        saveUsers();
        logger.info("LastOnline has been disabled!");
    }

    private String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s.replace("&p", PrimaryColor).replace("&s", SecondaryColor));
    }

    private void loadUsers() {
        List<String> usersList = users.getStringList("List");
        for (String s : usersList) {
            String[] array = s.split(":");
            userMap.put(UUID.fromString(array[0]), Long.valueOf(array[1]));
        }
    }

    private void saveUsers() {
        List<String> usersList = new ArrayList<>();
        for (UUID uuid : userMap.keySet()) {
            usersList.add(uuid.toString() + ":" + userMap.get(uuid).toString());
        }
        users.set("List", usersList);
        try {
            users.save(usersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configs() {
        saveDefaultConfig();
        if (!usersFile.exists()) {
            try {
                if (!usersFile.createNewFile()) {
                    logger.info("Failed to create users.yml file!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        users = YamlConfiguration.loadConfiguration(usersFile);
        if (!messagesFile.exists()) {
            saveResource("messages.yml", true);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        PrimaryColor = messages.getString("PrimaryColor");
        SecondaryColor = messages.getString("SecondaryColor");
    }

    private void update() {
        if (updater.checkUpdate()) {
            if (getConfig().getBoolean("AutoUpdate")) {
                updater.downloadUpdate();
            } else {
                logger.info("There is an update for LastOnline, go to spigot to download it!");
            }
        } else {
            logger.info("You are using the latest version of LastOnline");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (getConfig().getBoolean("CountOnlinePlayers")) {
            enterData(e.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        enterData(e.getPlayer());
    }

    private void enterData(Player p) {
        if (userMap.containsKey(p.getUniqueId()) || (userMap.size() < getConfig().getInt("MaxUsers") || getConfig().getInt("MaxUsers") == -1)) {
            Date date = new Date();
            userMap.put(p.getUniqueId(), date.getTime());
        } else {
            UUID oldest = p.getUniqueId();
            Long smallest = new Date().getTime();
            for (UUID uuid : userMap.keySet()) {
                if (userMap.get(uuid) < smallest) {
                    oldest = uuid;
                    smallest = userMap.get(uuid);
                }
            }
            userMap.remove(oldest);
            userMap.put(p.getUniqueId(), new Date().getTime());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("lastonline")) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                if (!p.hasPermission("lastonline.use")) {
                    p.sendMessage(ChatColor.RED + "No permission");
                    return true;
                }
            }
            if (args.length == 0) {
                if (userMap.size() == 0) {
                    sender.sendMessage(colorize(messages.getString("Error.NoUsers")));
                    return true;
                }
                TreeMap<Long, UUID> lastOnline = new TreeMap<>(Comparator.reverseOrder());
                Integer reportingLimit = getConfig().getInt("MaxUsersReporting");
                for (UUID uuid : userMap.keySet()) {
                    if (lastOnline.size() >= reportingLimit) {
                        Long current = userMap.get(uuid);
                        Long smallest = lastOnline.lastKey();
                        if (smallest < current) {
                            lastOnline.remove(smallest);
                            lastOnline.put(current, uuid);
                        }
                    } else {
                        lastOnline.put(userMap.get(uuid), uuid);
                    }
                }
                StringBuilder sb = new StringBuilder();
                String format = messages.getString("ListFormat");
                Integer i = 1;
                while (!lastOnline.isEmpty()) {
                    Map.Entry<Long, UUID> entry = lastOnline.firstEntry();
                    List<Duration> durationList = reduceList(pt.calculatePreciseDuration(new Date(entry.getKey())));
                    String dateFormat = pt.format(durationList).replace("from now", "ago");
                    OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getValue());
                    String playerName = op.getName();
                    String line = format.replace("%NAME", playerName).replace("%TIME", dateFormat).replace("%NUMBER", i.toString())
                            .replace("%STATUS", op.isOnline() ? "online" : "offline");
                    sb.append(line);
                    lastOnline.remove(entry.getKey());
                    i++;
                }
                String message = messages.getString("ReportingFormat").replace("%NUMBER", i - 1 + "").replace("%LIST", sb.toString());
                sender.sendMessage(colorize(message));
            } else if (args.length == 1) {
                String name = args[0];
                @SuppressWarnings("deprecation")
                OfflinePlayer op = Bukkit.getOfflinePlayer(name);
                UUID uuid = op.getUniqueId();
                if (userMap.containsKey(uuid)) {
                    List<Duration> durationList = reduceList(pt.calculatePreciseDuration(new Date(userMap.get(uuid))));
                    String timeFormat = pt.format(durationList);
                    String format = messages.getString("SingleReportFormat");
                    String message = format.replace("%NAME", name).replace("%TIME", timeFormat).replace("%STATUS", op.isOnline() ? "online" : "offline");
                    sender.sendMessage(colorize(message));
                } else if (userMap.size() > 0) {
                    sender.sendMessage(colorize(messages.getString("Error.NoSuchUser")));
                } else {
                    sender.sendMessage(colorize(messages.getString("Error.NoUsers")));
                }
            } else {
                sender.sendMessage(colorize(messages.getString("Error.Usage")));
            }
        }
        return true;
    }

    private List<Duration> reduceList(List<Duration> durationList) {
        while (durationList.size() > 3) {
            Duration smallest = null;
            Iterator<Duration> it = durationList.iterator();
            while (it.hasNext()) {
                Duration current = it.next();
                if (smallest == null || smallest.getUnit().getMillisPerUnit() > current.getUnit().getMillisPerUnit()) {
                    smallest = current;
                }
            }
            durationList.remove(smallest);
        }
        return durationList;
    }
}
