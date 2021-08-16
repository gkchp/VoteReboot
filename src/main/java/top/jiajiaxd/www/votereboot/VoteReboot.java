package top.jiajiaxd.www.votereboot;


import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import top.jiajiaxd.www.votereboot.handlers.NoticeHandler;
import top.jiajiaxd.www.votereboot.handlers.PlayerEventHandler;
import top.jiajiaxd.www.votereboot.utils.CommandUtil;
import top.jiajiaxd.www.votereboot.utils.Internet;
import top.jiajiaxd.www.votereboot.utils.Metrics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static top.jiajiaxd.www.votereboot.utils.Constants.*;

/**
 * @author jiajiaxd
 */
public class VoteReboot extends JavaPlugin {
    public static final long SEC = 20;
    public static final String VERSION = "Release-2.3";

    public static String messagePrefix;
    public static String updateLog;
    public static boolean update = false;
    public static boolean notice;
    public static boolean ipCheck;
    public static boolean checkPlayer;
    Metrics metrics = new Metrics(this, 7670);

    public static ConcurrentHashMap<UUID, Boolean> VoteStatus = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<UUID, Boolean> isAFK = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Boolean> IPMap = new ConcurrentHashMap<>();
    private static boolean onInit = true;
    public static boolean checkingPlayer = false;
    public static boolean isVoting = false;
    public static boolean isRebooting = false;
    public static int rebootCountdown;
    public static int voteMinuteLeft;

    @Override
    public void onEnable() {
        if (onInit) {
            cliMessage("欢迎使用VoteReboot " + VERSION);
            Bukkit.getPluginManager().registerEvents(new NoticeHandler(), this);
            getCommand(COMMAND_ROOT).setTabCompleter(this);
        }
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
        if (!TRUE.equals(getConfigValue(CONFIG_ENABLE_PLUGIN))) {
            cliMessage("插件已经关闭。如果需要启动，请在config.yml内将" + CONFIG_ENABLE_PLUGIN + "设置为true。");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        messagePrefix = getConfigValue(CONFIG_MESSAGE_PREFIX);
        ipCheck = TRUE.equals(getConfigValue(CONFIG_CHECK_IP));
        checkPlayer = TRUE.equals(getConfigValue(CONFIG_CHECK_PLAYER));
        if (update && TRUE.equals(getConfigValue(CONFIG_CHECK_UPDATE))) {
            checkUpdate();
        }
        if (checkPlayer) {
            notice = TRUE.equals(getConfigValue(CONFIG_AFK_NOTICE));
            if (onInit) {
                Bukkit.getPluginManager().registerEvents(new PlayerEventHandler(), this);
            }
            for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                lastActivity.put(p.getUniqueId(), System.currentTimeMillis());
                isAFK.put(p.getUniqueId(), false);
            }
            cliMessage("已经启用挂机玩家检测");
            if (!checkingPlayer) {
                checkingPlayer = true;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!checkPlayer) {
                            checkingPlayer = false;
                            this.cancel();
                        }
                        for (UUID playerId : lastActivity.keySet()) {
                            long elapsed = (System.currentTimeMillis() - lastActivity.get(playerId)) / 1000;
                            if (elapsed > Integer.parseInt(getConfigValue(CONFIG_AFK_TIMEOUT)) && !isAFK.get(playerId)) {
                                isAFK.put(playerId, true);
                                if (TRUE.equals(getConfigValue(CONFIG_AFK_NOTICE))) {
                                    if (VoteReboot.notice) {
                                        globalMessage(playerId + "暂时离开了");
                                    }
                                }
                            }
                        }
                    }
                }.runTaskTimerAsynchronously(this, 0L, SEC);
                //参数是,主类、延迟、多少秒运行一次,比如5秒那就是5*20L
            }
        }
        reInitialize();
        onInit = false;
    }

    private void reInitialize() {
        IPMap.clear();
        isVoting = false;
        isRebooting = false;
        VoteStatus.forEach(((uuid, aBoolean) -> VoteStatus.put(uuid, false)));
        voteMinuteLeft = 3;
        rebootCountdown = Integer.parseInt(getConfigValue(CONFIG_REBOOT_COUNTDOWN));
    }

    private void reload() {
        VoteReboot.lastActivity.clear();
        VoteReboot.isAFK.clear();
        IPMap.clear();
        isVoting = false;
        isRebooting = false;
        checkingPlayer = false;
        onEnable();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (COMMAND_ROOT.equals(command.getName())) {
            String[] subCommands = {ARGS_RELOAD, ARGS_NOW, ARGS_BUG};
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

    public static void globalMessage(String message) {
        Bukkit.broadcastMessage(messagePrefix + " §a" + message);
    }

    public static void playerMessage(CommandSender sender, String message) {
        sender.sendMessage(messagePrefix + " §a" + message);
    }

    public String getConfigValue(String path) {
        if (!getConfig().contains(path)) {
            cliMessage("检测到配置文件损坏，正在重置配置文件...");
            saveDefaultConfig();
            cliMessage("已经重置配置文件。");
        }
        return getConfig().getString(path);
    }

    public Integer getNeedPlayers() {
        int afkCount = 0;
        if (checkPlayer) {
            afkCount = (int) isAFK.values().stream().filter(afk -> afk).count();
        }
        int onlinePlayers = Bukkit.getOnlinePlayers().size() - afkCount;
        double rate = Double.parseDouble(getConfigValue(CONFIG_VOTE_RATE));
        return Double.valueOf(Math.ceil(onlinePlayers * rate + 0.5)).intValue();
    }

    public void addVotedPlayer(CommandSender player) {
        String playerName = player.getName();
        UUID playerId = ((Player) player).getUniqueId();
        String ip = String.valueOf(((Player) player).getAddress().getAddress());
        long voteCount = VoteStatus.values().stream().filter(voted -> voted).count();
        boolean haveVoted = false;
        if (VoteStatus.get(playerId)) {
            playerMessage(player, "你已经投过票了！");
            haveVoted = true;
        }
        if (ipCheck) {
            if (IPMap.get(ip)) {
                playerMessage(player, "此IP已经有玩家投过票了，请勿重复投票！");
                haveVoted = true;
            }
        }
        if (!haveVoted) {
            IPMap.put(ip, true);
            VoteStatus.put(playerId, true);
            int onlinePlayers = Bukkit.getOnlinePlayers().size();
            int needPlayers = getNeedPlayers();

            if (!checkPlayer) {
                globalMessage("§e" + playerName + " §a进行了投票 本服共§e" + onlinePlayers + "§a人在线，共需要§e"
                        + needPlayers + "§a人投票 目前票数：§e" + voteCount);
            } else {
                globalMessage("§e" + playerName + " §a进行了投票 本服共§e" + onlinePlayers + "§a人在线，共需要§e"
                        + needPlayers + "§a人投票（不包含正在挂机的玩家） 目前票数：§e" + voteCount);
            }
        }
        checkVotes();
    }

    public void checkVotes() {
        long voteCount = VoteStatus.values().stream().filter(voted -> voted).count();
        if (voteCount >= getNeedPlayers()) {
            globalMessage("投票已经完成！服务器将在十秒后重启！");
            isVoting = false;
            isRebooting = true;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (isRebooting) {
                        globalMessage("服务器将在" + rebootCountdown + "秒后重启！");
                        rebootCountdown--;
                    } else {
                        globalMessage("管理员取消了本次重启！");
                        this.cancel();
                    }
                    if (rebootCountdown <= 0) {
                        globalMessage("正在重启...");
                        try {
                            reboot();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        this.cancel();
                    }
                }
            }.runTaskTimerAsynchronously(this, 0L, SEC);
            //参数是,主类、延迟、多少秒运行一次,比如5秒那就是5*20L
        }
    }

    public void reboot() throws IOException {
        metrics.addCustomChart(new Metrics.SimplePie("number_of_restarts", () -> "1"));
        if (TRUE.equals(getConfigValue(CONFIG_SAVE_FIRST))) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getConfigValue(CONFIG_SAVE_COMMAND));
        }
        if (CONFIG_REBOOT_METHOD_NATIVE.equals(getConfigValue(CONFIG_REBOOT_METHOD))) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), getConfigValue(CONFIG_NATIVE_COMMAND));
        } else {
            cliMessage("正在尝试重启...");
            cliMessage("正在执行命令" + getConfigValue(CONFIG_SHELL_COMMAND));
            cliMessage("返回结果：" + CommandUtil.run(getConfigValue(CONFIG_SHELL_COMMAND)));
        }
    }

    private void commandVote(CommandSender sender) {
        if (!(sender instanceof Player)) {
            cliMessage("控制台无法使用此命令");
        } else {
            if (!isVoting) {
                int onlinePlayers = Bukkit.getOnlinePlayers().size();
                int minimal = Integer.parseInt(getConfigValue(CONFIG_MINIMAL_VOTE));
                long afkCount = checkPlayer ? isAFK.values().stream().filter(afk -> afk).count() : 0;
                if (onlinePlayers - afkCount < minimal) {
                    cliMessage("need" + getNeedPlayers());
                    if (checkPlayer) {
                        playerMessage(sender, "需要至少" + minimal + "名不在挂机的在线玩家才可以发起重启投票");
                    } else {
                        playerMessage(sender, "需要至少" + minimal + "名在线玩家才可以发起重启投票");
                    }
                } else {
                    isVoting = true;
                    UUID playerId = ((Player) sender).getUniqueId();
                    VoteStatus.put(playerId, true);
                    IPMap.put(String.valueOf(((Player) sender).getAddress().getAddress()), true);
                    globalMessage("§e" + sender.getName() + " §a发起了投票重启 本服共§e" + onlinePlayers + "§a人在线，需要§e" + getNeedPlayers() + "§a人投票 目前票数：§e1");
                    globalMessage("请同意重启的玩家输入 /" + COMMAND_ACCEPT);
                    globalMessage("本次投票在3分钟内有效");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (isVoting) {
                                voteMinuteLeft--;
                                if (voteMinuteLeft <= 0) {
                                    globalMessage("本次重启投票已经过期");
                                    reInitialize();
                                    this.cancel();
                                } else {
                                    globalMessage("投票将在" + voteMinuteLeft + "分钟后结束！");
                                }
                            } else {
                                this.cancel();
                            }
                        }
                    }.runTaskTimerAsynchronously(this, 60 * SEC, 60 * SEC);
                    //参数是,主类、延迟、多少秒运行一次,比如5秒那就是5*20L
                }
            } else {
                playerMessage(sender, "已经有一个投票正在进行中，请勿重复发起投票！");
                playerMessage(sender, "若需要同意重启，请输入 /" + COMMAND_ACCEPT);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        boolean validCommand = false;
        if (COMMAND_VOTE.equalsIgnoreCase(label)) {
            validCommand = true;
            commandVote(sender);
        }
        if (COMMAND_ACCEPT.equalsIgnoreCase(label)) {
            validCommand = true;
            if (!(sender instanceof Player)) {
                cliMessage("控制台无法使用此命令");
            } else {
                if (isVoting) {
                    addVotedPlayer(sender);
                } else {
                    playerMessage(sender, "当前没有正在进行的投票！请输入 /" + COMMAND_VOTE + " 发起一个重启投票");
                }
            }
        }
        if (COMMAND_CANCEL.equalsIgnoreCase(label)) {
            validCommand = true;
            if (sender.isOp()) {
                reInitialize();
                playerMessage(sender, "已经取消当前所有操作");
                if (isVoting) {
                    globalMessage("本次投票被管理员结束！");
                }
            } else {
                playerMessage(sender, "仅服务器管理员可以使用此命令！");
            }
        }
        if (COMMAND_ROOT.equalsIgnoreCase(label)) {
            if (args.length > 0) {
                if (ARGS_RELOAD.equals(args[0])) {
                    validCommand = true;
                    if (sender.isOp()) {
                        playerMessage(sender, "正在重载，请稍后...");
                        reload();
                        playerMessage(sender, "重载完毕");
                    } else {
                        playerMessage(sender, "仅服务器管理员可以使用此命令！");
                    }
                }
                if (ARGS_NOW.equals(args[0])) {
                    validCommand = true;
                    if (sender.isOp()) {
                        try {
                            reboot();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        playerMessage(sender, "仅服务器管理员可以使用此命令！");
                    }
                }
                if (ARGS_BUG.equals(args[0])) {
                    validCommand = true;
                    sender.sendMessage("§b若插件出现BUG或你想给插件提出建议，请前往" + ISSUE_URL + "进行反馈");
                    sender.sendMessage("§b或在 MCBBS 回复插件所在帖子");
                }
            }
            if (!validCommand) {
                validCommand = true;
                sender.sendMessage("§b----------§eVoteReboot 菜单§b----------");
                sender.sendMessage("§3/" + COMMAND_VOTE + " 发起一次重启投票");
                sender.sendMessage("§3/" + COMMAND_ACCEPT + " 投票");
                sender.sendMessage("§3/" + COMMAND_ROOT + " " + ARGS_RELOAD + " 重载插件");
                sender.sendMessage("§3/" + COMMAND_ROOT + " " + ARGS_NOW + " 立刻重启");
                sender.sendMessage("§3/" + COMMAND_ROOT + " " + ARGS_BUG + " 反馈 BUG");
                sender.sendMessage("§3插件作者：甲甲");
                sender.sendMessage("§3若出现任何 BUG");
                sender.sendMessage("§3请输入/" + COMMAND_ROOT + " " + ARGS_BUG);
                sender.sendMessage("§b----------§eVoteReboot 菜单§b----------");
            }
        }
        return validCommand;
    }

    private void checkUpdate() {
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean codeMatches = false;
                cliMessage("正在检查更新...");
                String code = Internet.get(UPDATE_URL + VERSION + ".html");
                if (!StringUtils.isEmpty(code)) {
                    if (VERSION_VULNERABLE.equals(code)) {
                        codeMatches = true;
                        cliMessage("本版本因为存在重大 BUG 而被停用，请前往" + UPDATE_URL + "更新！");
                        cliMessage("为了防止出现问题，插件自动停用。");
                        Bukkit.getPluginManager().disablePlugin(JavaPlugin.getPlugin(VoteReboot.class));
                    }
                    if (VERSION_LATEST.equals(code)) {
                        codeMatches = true;
                        cliMessage("插件已经是最新版本");
                    }
                    if (VERSION_DEPRECATED.equals(code)) {
                        codeMatches = true;
                        updateLog = Internet.get(UPDATE_URL + "uplog.html");
                        cliMessage("插件不是最新版本！本次新版更新日志：");
                        cliMessage(updateLog);
                        update = true;
                    }
                    if (!codeMatches) {
                        cliMessage("在检查更新时出现了错误！代码：121");
                    }
                } else {
                    cliMessage("检查更新失败！请检查是否有安全插件拦截了网络（如Yum）");
                }
            }
        }.runTaskAsynchronously(JavaPlugin.getPlugin(VoteReboot.class));
    }
}

