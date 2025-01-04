package org.apiv1.milkchanApi;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import java.util.Set;
import java.util.HashSet;
import org.bukkit.Location;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerJoinEvent;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.command.ConsoleCommandSender;

/*
Copyright (c) 2025 milkchan128

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

public final class MilkchanApi extends JavaPlugin implements Listener {

    private boolean pvpEnabledForAll = false; // 서버의 전체 PVP 상태를 나타내는 변수

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        getCommand("uc").setExecutor(new UCCommandExecutor(this)); // /uc reload 명령어 처리 추가

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("pvp").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "이 명령어는 플레이어만 사용할 수 있습니다!");
                return true;
            }

            Player player = (Player) sender;

            // 오피 권한이 없는 플레이어는 명령어를 사용하지 못하도록 처리
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "오피 권한이 필요합니다!");
                return true;
            }

            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "사용법: /pvp <on|off>");
                return true;
            }

            if (args[0].equalsIgnoreCase("on")) {
                pvpEnabledForAll = true;  // 서버 전체 PVP 활성화
                player.sendMessage(ChatColor.GREEN + "전체 PVP가 활성화되었습니다.");
            } else if (args[0].equalsIgnoreCase("off")) {
                pvpEnabledForAll = false;  // 서버 전체 PVP 비활성화
                player.sendMessage(ChatColor.RED + "전체 PVP가 비활성화되었습니다.");
            } else {
                player.sendMessage(ChatColor.RED + "잘못된 사용법입니다. /pvp <on|off>");
            }
            return true;
        });
    }

    @Override
    public void onDisable() {
        getLogger().info("MilkchanAPI 플러그인이 비활성화되었습니다.");
    }

    // /uc reload 명령어 처리 클래스
    public class UCCommandExecutor implements CommandExecutor {

        private final MilkchanApi plugin;

        public UCCommandExecutor(MilkchanApi plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
                return false;
            }

            if (sender.hasPermission("milkchanapi.config.reload")) { // 오피 또는 권한 있는 사람만 사용 가능
                plugin.reloadConfig(); // config.yml을 리로드
                sender.sendMessage(ChatColor.GREEN + "config.yml이 리로드되었습니다.");
            } else {
                sender.sendMessage(ChatColor.RED + "이 명령어를 사용할 권한이 없습니다.");
            }
            return true;
        }
    }

    // 알수없는 명령어 입력시 메시지 커스텀
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().split(" ")[0].substring(1);

        if (!getServer().getCommandMap().getKnownCommands().containsKey(command)) {
            event.setCancelled(true);
            String unknownCommandMessage = getConfig().getString(
                    "unknown-command-message",
                    ChatColor.RED + "&4&l[!] &c알수없는 명령어 입니다. &7&oby milkchan128"
            );
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', unknownCommandMessage));
        }
    }

    //
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();

        int x = deathLocation.getBlockX();
        int y = deathLocation.getBlockY();
        int z = deathLocation.getBlockZ();
        String worldName = deathLocation.getWorld().getName();

        // 죽은 플레이어에게 좌표를 전달
        player.sendMessage(ChatColor.RED + "☠ " + ChatColor.YELLOW + "당신은 사망했습니다!");
        player.sendMessage(ChatColor.AQUA + "죽은 위치: " + ChatColor.GREEN + "X: " + x + ", Y: " + y + ", Z: " + z + " (월드: " + worldName + ")");

        // 주변에 알림
        for (Player onlinePlayer : getServer().getOnlinePlayers()) {
            if (!onlinePlayer.equals(player)) {
                onlinePlayer.sendMessage(ChatColor.GRAY + player.getName() + "님이 " + ChatColor.RED + "사망" + ChatColor.GRAY + "했습니다. " +
                        ChatColor.AQUA + "(위치: X: " + x + ", Y: " + y + ", Z: " + z + ")");
            }
        }
    }

    // pvp 비활성화시 데미지를 차단함
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            if (!pvpEnabledForAll) {
                event.setCancelled(true);  // PVP가 비활성화되어 있으면 데미지 차단
            }
        }
    }
}
