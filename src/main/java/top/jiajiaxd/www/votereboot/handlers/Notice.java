package top.jiajiaxd.www.votereboot.handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import top.jiajiaxd.www.votereboot.VoteReboot;

/**
 * @author jiajiaxd
 */
public class Notice implements org.bukkit.event.Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.getPlayer().isOp() && VoteReboot.update == true) {
                    player.getPlayer().sendMessage("§b[VoteRebbot]当前版本不是最新版本，请前往https://open.jiajiaxd.top/vr/进行更新！");
                }
            }
        }.runTaskAsynchronously(VoteReboot.me);
    }
}
