package top.jiajiaxd.www.votereboot;


import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import top.jiajiaxd.www.votereboot.handlers.CheckPlayer;
import top.jiajiaxd.www.votereboot.handlers.Notice;
import top.jiajiaxd.www.votereboot.utils.CommandUtil;
import top.jiajiaxd.www.votereboot.utils.Internet;
import top.jiajiaxd.www.votereboot.utils.Metrics;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static top.jiajiaxd.www.votereboot.utils.Constants.*;

/**
 * @author jiajiaxd
 */
public class VoteReboot extends JavaPlugin {
    public static boolean isVoting;
    public static String[] VotedPlayer;
    public static Plugin me;
    public static HashMap<String, Integer> semap = new HashMap<>();
    public static HashMap<String, Boolean> isGuaji = new HashMap<>();
    public static String prefix;
    public static HashMap<String, Boolean> IPMap = new HashMap<>();
    public static Boolean ipcheck;
    public static boolean isRebooting = false;
    public static int rs = 0;
    public static int vs = 3;
    public static boolean cancel = false;
    public static boolean notice;
    public static boolean reload=false;
    public static String version="Release-2.3";
    public static boolean update=false;
    public static String updatelog;
    Metrics metrics = new Metrics(this, 7670);

    @Override
    public void onEnable() {
        me = this;
        isVoting = false;
        if (!cancel) {
            VotedPlayer = new String[Bukkit.getMaxPlayers() + 1];
            isGuaji.replaceAll((k, v) -> false);
            cliMessage("欢迎使用VoteReboot " + version);
        }
        Arrays.fill(VotedPlayer, "空玩家233333333标志@!=~&*^");
        getCommand(COMMAND_ROOT).setTabCompleter(this);
        if (!getDataFolder().exists()) {
            boolean mkdirs = getDataFolder().mkdir();
            if (!mkdirs) {
                cliMessage("无法创建配置文件夹，可能是因为权限问题！");
                cliMessage("因无法创建配置文件夹，插件正在关闭");
                Bukkit.getPluginManager().disablePlugin(this);
            }
        }
        File file = new File(getDataFolder(), "config.yml");
        if (!(file.exists())) {
            cliMessage("没有检测到配置文件！正在创建...");
            saveDefaultConfig();
            cliMessage("创建配置文件完毕！");
        }
        reloadConfig();
        if (!TRUE.equals(getConfig(ENABLE_PLUGIN))) {
            cliMessage("插件已经关闭。如果需要启动，请在config.yml内将EnablePlugin设置为true。");
            Bukkit.getPluginManager().disablePlugin(this);
        }
//        if (getConfig("checkupdate").equals(TRUE)) {
//            checkUpdate();
//        }
        if (!cancel && !reload) {
            Bukkit.getPluginManager().registerEvents(new Notice(), this);
        }
        prefix = getConfig("prefix");
        if (TRUE.equals(getConfig("IPCheck"))) {
            ipcheck = true;
        } else {
            ipcheck = false;
        }
        rs = Integer.valueOf(getConfig("rs"));
        if (TRUE.equals(getConfig("checkplayer"))) {
            for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                if (!cancel) {
                    semap.put(p.getName(), 0);
                    isGuaji.put(p.getName(), false);
                }
            }
            if (!cancel && !reload) {
                Bukkit.getPluginManager().registerEvents(new CheckPlayer(), this);
            }
            if (!cancel) {
                cliMessage("已经启用挂机玩家检测");
            }
            if (TRUE.equals(getConfig("notice"))) {
                notice = true;
            } else {
                notice = false;
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (String key : semap.keySet()) {
                        int value = semap.get(key);
                        semap.put(key, value + 1);
                        if (value > Integer.parseInt(getConfig("s")) && !isGuaji.get(key)) {
                            isGuaji.put(key, true);
                            if (TRUE.equals(getConfig("notice"))) {
                                if (VoteReboot.notice) {
                                    sendGlobalMessage(key + "暂时离开了");
                                }
                            }
                        }
                    }
                }
            }.runTaskTimerAsynchronously(this, 0L, 20L);
            //参数是,主类、延迟、多少秒运行一次,比如5秒那就是5*20L
        }
        cancel = false;
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args){
        if (COMMAND_ROOT.equals(command.getName())) {
            String[] subCommands = {"reload", "now", "bug"};
            if (args.length > 1) {
                return new ArrayList<>();
            }
            if (args.length == 0) {
                return Arrays.asList(subCommands);
            }
            return Arrays.stream(subCommands).filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public static void cliMessage(String msg) {
        Bukkit.getLogger().info("\u001B[0;33;22m[\u001B[0;36;1mVoteReboot\u001B[0;33;22m] \u001B[0;32;1m" + msg + "\u001B[m");
    }

    public String getConfig(String path) {
        String value;
        value = "null";
        if (!getConfig().contains(path)) {
            cliMessage("检测到配置文件损坏，正在重置配置文件...");
            saveDefaultConfig();
            cliMessage("已经重置配置文件。");
        } else {
            value = getConfig().getString(path);
        }
        return value;
    }

    public static void sendWrongMessage(CommandSender sender, String message) {
        sender.sendMessage(prefix + " §c" + message);
    }

    public static void sendGlobalMessage(String message) {
        Bukkit.broadcastMessage(prefix + " §a" + message);
    }

    public static void sendPlayerMessage(CommandSender sender, String message) {
        sender.sendMessage(prefix + " §a" + message);
    }

    public Integer getNeedPlayers() {
        int gjNum = 0;
        int needPlayers = 0;
        if (TRUE.equals(getConfig("checkplayer"))) {
            for (String key : isGuaji.keySet()) {
                if (isGuaji.get(key)) {
                    gjNum++;
                }
            }
        }
        int onlinePlayers = Bukkit.getOnlinePlayers().size() - gjNum;
        double rate = Double.parseDouble(getConfig("rate"));
        needPlayers = (int) (onlinePlayers * rate + 0.5);
        return needPlayers;
    }

    public void addVotedPlayer(CommandSender player) {
        int real = 0;
        String playerName = player.getName();
        String ip = String.valueOf(((Player) player).getAddress().getAddress());
        boolean havevoted = false;
        boolean voted = false;
        for (int i = 0; i <= Bukkit.getMaxPlayers(); i++) {
            if (VotedPlayer[i].equals(playerName)) {
                sendWrongMessage(player, "你已经投过票了！");
                havevoted = true;
                break;
            }
        }
        if (ipcheck) {
            if (IPMap.get(ip)) {
                sendWrongMessage(player, "此IP已经有玩家投过票了，请勿重复投票！");
                havevoted = true;
            }
        }
        if (!havevoted) {
            IPMap.put(ip, true);
            if (!havevoted) {
                int onlinePlayers = Bukkit.getOnlinePlayers().size();
                double rate = Double.parseDouble(getConfig("rate"));
                int needPlayers = (int) (onlinePlayers * rate + 0.5);
                for (String s : VotedPlayer) {
                    if (!("空玩家233333333标志@!=~&*^".equals(s))) {
                        real++;
                    }
                }
                VotedPlayer[real + 1] = playerName;
                if (!TRUE.equals(getConfig("checkplayer"))) {
                    sendGlobalMessage("§e" + playerName + " §a进行了投票 本服共§e" + onlinePlayers + "§a人在线，共需要§e" + needPlayers + "§a人投票 目前票数：§e" + (real + 1));
                } else {
                    sendGlobalMessage("§e" + playerName + " §a进行了投票 本服共§e" + onlinePlayers + "§a人在线，共需要§e" + needPlayers + "§a人投票（不包含正在挂机的玩家） 目前票数：§e" + (real + 1));
                }
            }
        }
        if (real + 1 == getNeedPlayers()||real + 1 > getNeedPlayers()) {
            sendGlobalMessage("投票已经完成！服务器将在十秒后重启！");
            isVoting = false;
            isRebooting = true;

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (isRebooting) {
                        sendGlobalMessage("服务器将在" + rs + "秒后重启！");
                        rs--;
                    } else {
                        sendGlobalMessage("管理员取消了本次重启！");
                        this.cancel();
                    }
                    if (rs == 0) {
                        sendGlobalMessage("正在重启...");
                        try {
                            reboot();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        this.cancel();
                    }
                }
            }.runTaskTimerAsynchronously(this, 0L, 20L);
            //参数是,主类、延迟、多少秒运行一次,比如5秒那就是5*20L
        }
    }

    public void reboot() throws IOException {
        metrics.addCustomChart(new Metrics.SimplePie("number_of_restarts", () -> "1"));
        if (TRUE.equals(getConfig("save"))) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getConfig("savec"));
        }
        if ("1".equals(getConfig("way"))) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getConfig("cc"));
        } else {
            cliMessage("正在尝试重启...");
            cliMessage("正在执行命令" + getConfig("ccwl"));
            cliMessage("返回结果：" + CommandUtil.run(getConfig("ccwl")));
        }
    }

    public void cancel() {
        isVoting = false;
        isRebooting = false;
        Arrays.fill(VotedPlayer, "空玩家233333333标志@!=~&*^");
        for (Iterator<Map.Entry<String, Boolean>> it = VoteReboot.IPMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Boolean> entry = it.next();
            it.remove();
        }
        cancel = true;
        onEnable();
    }

    public void reload(){
        for (Iterator<Map.Entry<String, Integer>> it = VoteReboot.semap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Integer> entry = it.next();
            it.remove();
        }
        for (Iterator<Map.Entry<String, Boolean>> it = VoteReboot.isGuaji.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Boolean> entry = it.next();
            it.remove();
        }
        isVoting = false;
        isRebooting = false;
        Arrays.fill(VotedPlayer, "空玩家233333333标志@!=~&*^");
        for (Iterator<Map.Entry<String, Boolean>> it = VoteReboot.IPMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Boolean> entry = it.next();
            it.remove();
        }
        reload = true;
        onEnable();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        boolean a = false;
        if ("vote".equalsIgnoreCase(label)) {
            a = true;
            if (!(sender instanceof Player)) {
                cliMessage("控制台无法使用此命令");
            } else {
                if (!isVoting) {
                    int onlinePlayers = Bukkit.getOnlinePlayers().size();
                    if (getNeedPlayers() < 2) {
                        cliMessage("need" + getNeedPlayers());
                        if (TRUE.equals(getConfig("checkplayer"))) {
                            sendWrongMessage(sender, "需要至少三名不在挂机的在线玩家才可以发起重启投票");
                        } else {
                            sendWrongMessage(sender, "需要至少三名在线玩家才可以发起重启投票");
                        }
                    } else {
                        isVoting = true;
                        VotedPlayer[0] = sender.getName();
                        String ip = String.valueOf(((Player) sender).getAddress().getAddress());
                        IPMap.put(ip, true);
                        sendGlobalMessage("§e" + sender.getName() + " §a发起了投票重启 本服共§e" + onlinePlayers + "§a人在线，需要§e" + getNeedPlayers() + "§a人投票 目前票数：§e1");
                        sendGlobalMessage("请同意重启的玩家输入/voteaccept");
                        sendGlobalMessage("本次投票在3分钟内有效");
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (isVoting) {
                                    vs--;
                                    if (vs == 0) {
                                        sendGlobalMessage("本次重启投票已经过期");
                                        cancel();
                                        this.cancel();
                                    } else {
                                        sendGlobalMessage("投票将在" + vs + "分钟后结束！");
                                    }
                                } else {
                                    this.cancel();
                                }
                            }
                        }.runTaskTimerAsynchronously(this, 1200L, 1200L);
                        //参数是,主类、延迟、多少秒运行一次,比如5秒那就是5*20L
                    }
                } else {
                    sendWrongMessage(sender, "已经有一个投票正在进行中，请勿重复发起投票！");
                    sendWrongMessage(sender, "若需要同意重启，请输入/voteaccept");
                }
            }
        }
        if ("voteaccept".equalsIgnoreCase(label)) {
            a = true;
            if (!(sender instanceof Player)) {
                cliMessage("控制台无法使用此命令");
            } else {
                if (isVoting) {
                    addVotedPlayer(sender);
                } else {
                    sendWrongMessage(sender, "当前没有正在进行的投票！请输入/vote发起一个重启投票");
                }
            }
        }
        if ("votecancel".equalsIgnoreCase(label)) {
            a = true;
            cancel();
            sendPlayerMessage(sender, "已经取消当前所有操作");
            if (isVoting) {
                sendGlobalMessage("本次投票被管理员结束！");
            }
        }
        if (COMMAND_ROOT.equalsIgnoreCase(label)) {
            if (args.length > 0 && sender.isOp()) {
                if ("bug".equals(args[0])) {
                    a = true;
                    sender.sendMessage("§b若插件出现BUG或你想给插件提出建议，请前往https://github.com/jiajiaxd/VRissues/issues进行反馈");
                    sender.sendMessage("§b或在MCBBS回复插件所在帖子");
                }
                if ("reload".equals(args[0])) {
                    a = true;
                    sendPlayerMessage(sender, "正在重载，请稍后...");
                    reload();
                    sendPlayerMessage(sender, "重载完毕");
                }
                if ("now".equals(args[0])) {
                    a = true;
                    try {
                        reboot();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            if (args.length > 0 && !sender.isOp()) {
                sendWrongMessage(sender, "仅服务器管理员可以使用此命令！");
                a = true;
            }
            if (!a) {
                a = true;
                sender.sendMessage("§b----------§eVoteReboot 菜单§b----------");
                sender.sendMessage("§3/vote 发起一次重启投票");
                sender.sendMessage("§3/voteaccept 投票");
                sender.sendMessage("§3/votereboot reload 重载插件");
                sender.sendMessage("§3/votereboot now 立刻重启");
                sender.sendMessage("§3/votereboot bug 反馈BUG");
                sender.sendMessage("§3插件作者：甲甲");
                sender.sendMessage("§3若出现任何BUG");
                sender.sendMessage("§3请输入/votereboot bug");
                sender.sendMessage("§b----------§eVoteReboot 菜单§b----------");
            }
        }
        return a;
    }

    private void checkUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Boolean ok = false;
                String nr;
                cliMessage("正在检查更新...");
                nr = Internet.get("https://open.jiajiaxd.top/vr/" + version + ".html");
                if (!nr.equals("null")) {
                    if (nr.equals("bug")) {
                        ok = true;
                        cliMessage("本版本因为存在重大BUG而被停用，请前往https://open.jiajiaxd.top/vr/更新！");
                        cliMessage("本版本因为存在重大BUG而被停用，请前往https://open.jiajiaxd.top/vr/更新！");
                        cliMessage("本版本因为存在重大BUG而被停用，请前往https://open.jiajiaxd.top/vr/更新！");
                        cliMessage("本版本因为存在重大BUG而被停用，请前往https://open.jiajiaxd.top/vr/更新！");
                        cliMessage("本版本因为存在重大BUG而被停用，请前往https://open.jiajiaxd.top/vr/更新！");
                        cliMessage("为了防止出现问题，插件自动停用。");
                        Bukkit.getPluginManager().disablePlugin(me);
                    }
                    if (nr.equals("latest")) {
                        ok = true;
                        cliMessage("插件已经是最新版本");
                    }
                    if (nr.equals("notlatest")) {
                        ok = true;
                        updatelog = Internet.get("https://open.jiajiaxd.top/vr/uplog.html");
                        cliMessage("插件不是最新版本！本次新版更新日志：");
                        cliMessage(updatelog);
                        update = true;
                    }
                    if (!ok) {
                        cliMessage("在检查更新时出现了错误！代码：121");
                    }
                } else {
                    cliMessage("检查更新失败！请检查是否有安全插件拦截了网络（如Yum）");
                }
            }
        }.runTaskAsynchronously(VoteReboot.me);
    }
}

