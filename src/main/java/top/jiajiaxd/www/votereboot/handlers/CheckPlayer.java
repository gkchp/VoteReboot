package top.jiajiaxd.www.votereboot.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import top.jiajiaxd.www.votereboot.VoteReboot;

import java.util.Iterator;
import java.util.Map;

/**
 * @author jiajiaxd
 */
public class CheckPlayer implements org.bukkit.event.Listener {


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                VoteReboot.semap.put(player.getPlayer().getName(), 0);
                VoteReboot.isGuaji.put(player.getPlayer().getName(), false);
            }
        }.runTaskAsynchronously(VoteReboot.me);

    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void playerMoving(PlayerMoveEvent player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String name = player.getPlayer().getName();
                if (VoteReboot.isGuaji.get(name)) {
                    if (VoteReboot.notice) {
                        VoteReboot.sendGlobalMessage(name + "回来了");
                    }
                    VoteReboot.isGuaji.put(name, false);
                }
                VoteReboot.semap.put(name, 0);
            }
        }.runTaskAsynchronously(VoteReboot.me);
    }

    @EventHandler
    public void playerChat(AsyncPlayerChatEvent player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String name = player.getPlayer().getName();
                VoteReboot.semap.put(name, 0);
                if (VoteReboot.isGuaji.get(name)) {
                    VoteReboot.isGuaji.put(name, false);
                    if (VoteReboot.notice) {
                        VoteReboot.sendGlobalMessage(name + "回来了");
                    }
                }
            }
        }.runTaskAsynchronously(VoteReboot.me);


    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Iterator<Map.Entry<String, Integer>> it = VoteReboot.semap.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<String, Integer> entry = it.next();
                    if (entry.getKey().equals(player.getPlayer().getName())) {
                        it.remove();
                    }
                }
            }
        }.runTaskAsynchronously(VoteReboot.me);
    }
}
