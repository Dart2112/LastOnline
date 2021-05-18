/*
 * Copyright 2021 Benjamin Martin
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

import net.lapismc.lapiscore.LapisCoreConfiguration;
import net.lapismc.lapiscore.LapisCorePlugin;
import net.lapismc.lapiscore.utils.LapisUpdater;
import net.lapismc.lapiscore.utils.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class LastOnline extends LapisCorePlugin implements Listener {

    private LapisUpdater updater;
    private final File usersFile = new File(getDataFolder(), "users.yml");
    private YamlConfiguration users;
    public HashMap<UUID, Long> userMap = new HashMap<>();
    private int taskNumber;

    @Override
    public void onEnable() {
        configs();
        loadUsers();
        new Metrics(this);
        update();
        Bukkit.getPluginManager().registerEvents(this, this);
        taskNumber = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> saveUsers(), 20 * 60 * 10, 20 * 60 * 10);
        new LastOnlineCommand(this);
        getLogger().info("LastOnline v." + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        saveUsers();
        Bukkit.getScheduler().cancelTask(taskNumber);
        getLogger().info("LastOnline has been disabled!");
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
        registerConfiguration(new LapisCoreConfiguration(this, 1, 1));
        if (!usersFile.exists()) {
            try {
                if (!usersFile.createNewFile()) {
                    getLogger().warning("Failed to create users.yml file!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        users = YamlConfiguration.loadConfiguration(usersFile);
    }

    private void update() {
        updater = new LapisUpdater(this, "LastOnline", "Dart2112", "LastOnline", "master");
        if (updater.checkUpdate()) {
            if (getConfig().getBoolean("AutoUpdate")) {
                updater.downloadUpdate();
            } else {
                getLogger().info("There is an update for LastOnline, go to spigot to download it!");
            }
        } else {
            getLogger().info("You are using the latest version of LastOnline");
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


}
