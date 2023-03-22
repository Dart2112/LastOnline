/*
 * Copyright 2023 Benjamin Martin
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
import net.lapismc.lapisui.LapisUI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public final class LastOnline extends LapisCorePlugin implements Listener {

    private LapisUpdater updater;
    private final File usersFile = new File(getDataFolder(), "users.yml");
    private YamlConfiguration users;
    public List<PlayerData> playerDataList = new ArrayList<>();
    private int taskNumber;

    @Override
    public void onEnable() {
        configs();
        loadUsers();
        new Metrics(this);
        update();
        new LapisUI().registerPlugin(this);
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
            PlayerData data = new PlayerData(UUID.fromString(array[0]), Long.valueOf(array[1]));
            playerDataList.add(data);
        }
    }

    private void saveUsers() {
        List<String> usersList = new ArrayList<>();
        for (PlayerData data : playerDataList) {
            usersList.add(data.getUUID().toString() + ":" + data.getTime().toString());
        }
        users.set("List", usersList);
        try {
            users.save(usersFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configs() {
        registerConfiguration(new LapisCoreConfiguration(this, 3, 2));
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
            processPlayer(e.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        processPlayer(e.getPlayer());
    }

    private void processPlayer(Player p) {
        if (isPlayerInList(p.getUniqueId()) || (playerDataList.size() < getConfig().getInt("MaxUsers") || getConfig().getInt("MaxUsers") == -1)) {
            enterData(p.getUniqueId());
        } else {
            PlayerData oldest = null;
            Long smallest = new Date().getTime();
            for (PlayerData data : playerDataList) {
                if (data.getTime() < smallest) {
                    oldest = data;
                    smallest = data.getTime();
                }
            }
            playerDataList.remove(oldest);
            enterData(p.getUniqueId());
        }
    }

    boolean isPlayerInList(UUID uuid) {
        for (PlayerData data : playerDataList) {
            if (data.getUUID().equals(uuid))
                return true;
        }
        return false;
    }

    private void enterData(UUID uuid) {
        if (isPlayerInList(uuid)) {
            PlayerData data = getPlayerData(uuid);
            playerDataList.remove(data);
            data.setTime(new Date().getTime());
            playerDataList.add(data);
        } else {
            PlayerData data = new PlayerData(uuid, new Date().getTime());
            playerDataList.add(data);
        }
    }


    public PlayerData getPlayerData(UUID uuid) {
        for (PlayerData data : playerDataList) {
            if (data.getUUID().equals(uuid))
                return data;
        }
        return null;
    }
}
