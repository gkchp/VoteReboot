package top.jiajiaxd.www.votereboot.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import top.jiajiaxd.www.votereboot.VoteReboot;

/**
 * @author jiajiaxd
 */
public class NoticeHandler implements org.bukkit.event.Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getPlayer().isOp() && VoteReboot.update) {
                    player.getPlayer().sendMessage("§b[VoteReboot]当前版本不是最新版本，请前往https://open.jiajiaxd.top/vr/进行更新！");
                }
            }
        }.runTaskAsynchronously(JavaPlugin.getPlugin(VoteReboot.class));
    }
}
