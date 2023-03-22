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

import net.lapismc.lapiscore.commands.LapisCoreCommand;
import net.lapismc.lapisui.menu.MultiPage;
import net.lapismc.lapisui.utils.LapisItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.JustNow;
import org.ocpsoft.prettytime.units.Millisecond;

import java.util.*;

public class LastOnlineCommand extends LapisCoreCommand {

    private final LastOnline plugin;
    private final PrettyTime pt = new PrettyTime(Locale.ENGLISH);

    protected LastOnlineCommand(LastOnline plugin) {
        super(plugin, "lastonline", "Shows the last players to be online", Arrays.asList("lo"));
        this.plugin = plugin;
        pt.removeUnit(JustNow.class);
        pt.removeUnit(Millisecond.class);
    }

    @Override
    protected void onCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.hasPermission("lastonline.use")) {
                p.sendMessage(plugin.config.getMessage("Error.Permission"));
                return;
            }
        }
        if (args.length == 0) {
            if (plugin.playerDataList.size() == 0) {
                sender.sendMessage(plugin.config.getMessage("Error.NoUsers"));
                return;
            }
            int reportingLimit = plugin.getConfig().getInt("MaxUsersReporting");
            if (plugin.getConfig().getBoolean("UseGUI") && sender instanceof Player) {
                TreeMap<Long, PlayerData> lastOnline = new TreeMap<>(Comparator.reverseOrder());
                for (PlayerData data : plugin.playerDataList) {
                    lastOnline.put(data.getTime(), data);
                }
                List<PlayerData> dataList = new ArrayList<>();
                int i = 1;
                for (Long time : lastOnline.keySet()) {
                    if (i > reportingLimit) break;
                    dataList.add(lastOnline.get(time));
                    i++;
                }
                new LastOnlineGUI(dataList).showTo((Player) sender);
            } else {
                TreeMap<Long, UUID> lastOnline = new TreeMap<>(Comparator.reverseOrder());
                for (PlayerData data : plugin.playerDataList) {
                    lastOnline.put(data.getTime(), data.getUUID());
                }
                StringBuilder sb = new StringBuilder();
                String format = plugin.config.getMessage("ListFormat");
                int i = 1;
                for (Long time : lastOnline.keySet()) {
                    if (i > reportingLimit) break;
                    UUID uuid = lastOnline.get(time);
                    List<Duration> durationList = reduceList(pt.calculatePreciseDuration(new Date(time)));
                    String dateFormat = pt.format(durationList).replace("from now", "ago");
                    OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                    String playerName = op.getName();
                    String line = format.replace("%NAME", playerName).replace("%TIME", dateFormat).replace("%NUMBER", i + "").replace("%STATUS", op.isOnline() ? "online" : "offline");
                    sb.append(line);
                    i++;
                }
                String message = plugin.config.getMessage("ReportingFormat").replace("%NUMBER", i - 1 + "").replace("%LIST", sb.toString());
                sender.sendMessage(message);
            }
        } else if (args.length == 1) {
            String name = args[0];
            OfflinePlayer op = Bukkit.getOfflinePlayer(name);
            UUID uuid = op.getUniqueId();
            if (plugin.isPlayerInList(uuid)) {
                List<Duration> durationList = reduceList(pt.calculatePreciseDuration(new Date(plugin.getPlayerData(uuid).getTime())));
                String timeFormat = pt.format(durationList);
                String format = plugin.config.getMessage("SingleReportFormat");
                String message = format.replace("%NAME", name).replace("%TIME", timeFormat).replace("%STATUS", op.isOnline() ? "online" : "offline");
                sender.sendMessage(message);
            } else if (plugin.playerDataList.size() > 0) {
                sender.sendMessage(plugin.config.getMessage("Error.NoSuchUser"));
            } else {
                sender.sendMessage(plugin.config.getMessage("Error.NoUsers"));
            }
        } else {
            sender.sendMessage(plugin.config.getMessage("Error.Usage"));
        }
    }

    private List<Duration> reduceList(List<Duration> durationList) {
        while (durationList.size() > plugin.getConfig().getInt("MaxTimeUnits")) {
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

    private class LastOnlineGUI extends MultiPage<PlayerData> {

        public LastOnlineGUI(List<PlayerData> list) {
            super(list, 3);
            setTitle("Last Online Players");
        }

        @Override
        protected ItemStack toItemStack(PlayerData data) {
            OfflinePlayer p = Bukkit.getOfflinePlayer(data.getUUID());
            List<Duration> durationList = reduceList(pt.calculatePreciseDuration(new Date(data.getTime())));
            String timeFormat = pt.format(durationList);
            String format = plugin.config.getMessage("GUIReportFormat");
            String message = format.replace("%TIME", timeFormat).replace("%STATUS", p.isOnline() ? "online" : "offline");
            return new LapisItemBuilder(p).setName(p.getName()).setLore(message).build();
        }

        @Override
        protected void onItemClick(Player p, PlayerData item) {

        }
    }


}
