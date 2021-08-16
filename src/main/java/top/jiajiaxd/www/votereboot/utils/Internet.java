package top.jiajiaxd.www.votereboot.utils;

import org.bukkit.plugin.PluginLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

/**
 * @author jiajiaxd
 */
public class Internet {
    public static String get(String url) {
        StringBuilder data = new StringBuilder();
        try {
            //创建一个URL实例
            java.net.URL urlInstance = new java.net.URL(url);
            //通过URL的openStream方法获取URL对象所表示的资源字节输入流
            InputStream is = urlInstance.openStream();
            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            //为字符输入流添加缓冲
            BufferedReader br = new BufferedReader(isr);
            //读取数据
            String line;
            //循环读取数据
            while ((line = br.readLine()) != null) {
                //输出数据
                data.append(line);
            }
            br.close();
            isr.close();
            is.close();
        } catch (Exception e) {
            PluginLogger.getAnonymousLogger().log(Level.WARNING, "[VoteReboot] 连接失败！", e);
        }
        return data.toString();
    }
}
