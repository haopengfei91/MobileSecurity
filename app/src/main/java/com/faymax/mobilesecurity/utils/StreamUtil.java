package com.faymax.mobilesecurity.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by HPF on 2016/9/5.
 */
public class StreamUtil {

    /**
     * 流转换成字符串
     * @param is 流对象
     * @return 流转换成的字符串
     */
    public static String stream2String(InputStream is) throws IOException {
        //在读取过程中，将读取内容存储在内存中，一次性转换成字符串
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        //读取流的操作
        byte[] buffer = new byte[1024];
        int temp = -1;
        try {
            while ((temp = is.read(buffer)) != -1) {
                bos.write(buffer, 0, temp);
            }
            //返回读取数据
            return bos.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            is.close();
            bos.close();
        }
        return null;
    }
}
