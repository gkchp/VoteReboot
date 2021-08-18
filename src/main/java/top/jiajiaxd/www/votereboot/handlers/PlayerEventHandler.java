package top.jiajiaxd.www.votereboot.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import top.jiajiaxd.www.votereboot.VoteReboot;

import java.util.UUID;

/**
 * @author jiajiaxd
 */
public class PlayerEventHandler implements org.bukkit.event.Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent player) {
        VoteReboot instance = JavaPlugin.getPlugin(VoteReboot.class);
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID playerId = player.getPlayer().getUniqueId();
                VoteReboot.playerName.put(playerId, player.getPlayer().getName());
                VoteReboot.voteStatus.put(playerId, false);
                VoteReboot.isAFK.put(playerId, false);
                VoteReboot.estimatedAfkTime.put(playerId, System.currentTimeMillis() + VoteReboot.timeoutMillis);
                if (VoteReboot.isVoting) {
                    instance.checkVotes();
                }
            }
        }.runTaskAsynchronously(instance);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void playerMoving(PlayerMoveEvent player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndUpdateAfk(player);
            }
        }.runTaskAsynchronously(JavaPlugin.getPlugin(VoteReboot.class));
    }

    @EventHandler
    public void playerChat(AsyncPlayerChatEvent player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndUpdateAfk(player);
            }
        }.runTaskAsynchronously(JavaPlugin.getPlugin(VoteReboot.class));
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent player) {
        VoteReboot instance = JavaPlugin.getPlugin(VoteReboot.class);
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID playerId = player.getPlayer().getUniqueId();
                VoteReboot.voteStatus.remove(playerId);
                VoteReboot.playerName.remove(playerId);
                VoteReboot.isAFK.remove(playerId);
                VoteReboot.estimatedAfkTime.remove(playerId);
                if (VoteReboot.isVoting) {
                    instance.checkVotes();
                }
            }
        }.runTaskAsynchronously(instance);
    }

    private void checkAndUpdateAfk(PlayerEvent event) {
        String name = event.getPlayer().getName();
        UUID playerId = event.getPlayer().getUniqueId();
        VoteReboot.estimatedAfkTime.put(playerId, System.currentTimeMillis() + VoteReboot.timeoutMillis);
        if (VoteReboot.isAFK.get(playerId)) {
            //加同步防止多次提醒
            synchronized (VoteReboot.class) {
                if (VoteReboot.isAFK.get(playerId)) {
                    VoteReboot.isAFK.put(playerId, false);
                    if (VoteReboot.notice) {
                        VoteReboot.globalMessage(name + "回来了");
                    }
                }
            }
        }
    }
}
