/*
 * Copyright 2020 Benjamin Martin
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

import net.lapismc.lapiscore.LapisCoreCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.JustNow;
import org.ocpsoft.prettytime.units.Millisecond;

import java.util.*;

public class LastOnlineCommand extends LapisCoreCommand {

    private LastOnline plugin;
    private PrettyTime pt = new PrettyTime(Locale.ENGLISH);

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
                p.sendMessage(ChatColor.RED + "No permission");
                return;
            }
        }
        if (args.length == 0) {
            if (plugin.userMap.size() == 0) {
                sender.sendMessage(plugin.config.getMessage("Error.NoUsers"));
                return;
            }
            TreeMap<Long, UUID> lastOnline = new TreeMap<>(Comparator.reverseOrder());
            int reportingLimit = plugin.getConfig().getInt("MaxUsersReporting");
            for (UUID uuid : plugin.userMap.keySet()) {
                if (lastOnline.size() >= reportingLimit) {
                    Long current = plugin.userMap.get(uuid);
                    Long smallest = lastOnline.lastKey();
                    if (smallest < current) {
                        lastOnline.remove(smallest);
                        lastOnline.put(current, uuid);
                    }
                } else {
                    lastOnline.put(plugin.userMap.get(uuid), uuid);
                }
            }
            StringBuilder sb = new StringBuilder();
            String format = plugin.config.getMessage("ListFormat");
            int i = 1;
            while (!lastOnline.isEmpty()) {
                Map.Entry<Long, UUID> entry = lastOnline.firstEntry();
                List<Duration> durationList = reduceList(pt.calculatePreciseDuration(new Date(entry.getKey())));
                String dateFormat = pt.format(durationList).replace("from now", "ago");
                OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getValue());
                String playerName = op.getName();
                String line = format.replace("%NAME", playerName).replace("%TIME", dateFormat).replace("%NUMBER", Integer.toString(i))
                        .replace("%STATUS", op.isOnline() ? "online" : "offline");
                sb.append(line);
                lastOnline.remove(entry.getKey());
                i++;
            }
            String message = plugin.config.getMessage("ReportingFormat").replace("%NUMBER", i - 1 + "").replace("%LIST", sb.toString());
            sender.sendMessage(message);
        } else if (args.length == 1) {
            String name = args[0];
            @SuppressWarnings("deprecation")
            OfflinePlayer op = Bukkit.getOfflinePlayer(name);
            UUID uuid = op.getUniqueId();
            if (plugin.userMap.containsKey(uuid)) {
                List<Duration> durationList = reduceList(pt.calculatePreciseDuration(new Date(plugin.userMap.get(uuid))));
                String timeFormat = pt.format(durationList);
                String format = plugin.config.getMessage("SingleReportFormat");
                String message = format.replace("%NAME", name).replace("%TIME", timeFormat).replace("%STATUS", op.isOnline() ? "online" : "offline");
                sender.sendMessage(message);
            } else if (plugin.userMap.size() > 0) {
                sender.sendMessage(plugin.config.getMessage("Error.NoSuchUser"));
            } else {
                sender.sendMessage(plugin.config.getMessage("Error.NoUsers"));
            }
        } else {
            sender.sendMessage(plugin.config.getMessage("Error.Usage"));
        }
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
