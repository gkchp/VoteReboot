package top.jiajiaxd.www.votereboot.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

/**
 * @author jiajiaxd
 */
public class Internet {
    public static String get(String url) {
        String returns = "null";
        try {
            //创建一个URL实例
            java.net.URL urlInstance = new java.net.URL(url);

            try {
                //通过URL的openStrean方法获取URL对象所表示的自愿字节输入流
                InputStream is = urlInstance.openStream();
                InputStreamReader isr = new InputStreamReader(is, "utf-8");

                //为字符输入流添加缓冲
                BufferedReader br = new BufferedReader(isr);
                //读取数据
                String data = br.readLine();
                //循环读取数据
                while (data != null) {
                    //输出数据
                    returns = data;
                    data = br.readLine();
                }
                br.close();
                isr.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return returns;
    }
}
