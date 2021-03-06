package com.cs.tomcat.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.log.LogFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @description: 迷你浏览器 模拟发送http协议的请求 并获得完整的http响应
 * @author: chushi
 * @create: 2020-05-29 11:44
 **/
public class MiniBrowser {
    public static void main(String[] args) {
        String url = "https://www.baidu.com";
        String contentString = getContentString(url, false);
        System.out.println(contentString);
        String httpString = getHttpString(url, false);
        System.out.println(httpString);
    }

    public static byte[] getContentBytes(String url, Map<String, Object> params, boolean isGet) {
        return getContentBytes(url, false, params, isGet);
    }

    public static byte[] getContentBytes(String url, boolean gzip) {
        return getContentBytes(url, gzip, null, true);
    }

    public static byte[] getContentBytes(String url) {
        return getContentBytes(url, false, null, true);
    }

    public static String getContentString(String url, Map<String, Object> params, boolean isGet) {
        return getContentString(url, false, params, isGet);
    }

    public static String getContentString(String url, boolean gzip) {
        return getContentString(url, gzip, null, true);
    }

    public static String getContentString(String url) {
        return getContentString(url, false, null, true);
    }

    public static String getContentString(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        byte[] result = getContentBytes(url, gzip, params, isGet);
        if (null == result) {
            return null;
        }
        return new String(result, StandardCharsets.UTF_8).trim();
    }

    public static byte[] getContentBytes(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        byte[] reponse = getHttpBytes(url, gzip, params, isGet);
        byte[] doubleReturn = "\r\n\r\n".getBytes();
        int pos = -1;
        for (int i = 0; i < reponse.length - doubleReturn.length; i++) {
            byte[] temp = Arrays.copyOfRange(reponse, i, i + doubleReturn.length);

            if (Arrays.equals(temp, doubleReturn)) {
                pos = i;
                break;
            }
        }
        if (-1 == pos) {
            return null;
        }

        pos += doubleReturn.length;
        byte[] result = Arrays.copyOfRange(reponse, pos, reponse.length);
        return result;
    }

    public static String getHttpString(String url, boolean gzip) {
        return getHttpString(url, gzip, null, true);
    }

    public static String getHttpString(String url) {
        return getHttpString(url, false, null, true);
    }

    public static String getHttpString(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        byte[] bytes = getHttpBytes(url, gzip, params, isGet);
        return new String(bytes).trim();
    }

    public static String getHttpString(String url, Map<String, Object> params, boolean isGet) {
        return getHttpString(url, false, params, isGet);
    }

    /**
     * @author: cs
     * @date: 2020/5/30 11:08
     * @param: [url, gzip]
     * @return: byte[]
     * @desc: 通过socket发送http协议给服务器
     */
    public static byte[] getHttpBytes(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        String method = isGet ? "GET" : "POST";
        byte[] result = null;
        try {
            URL u = new URL(url);
            Socket client = new Socket();
            int port = u.getPort();
            if (-1 == port) {
                port = 80;
            }

            InetSocketAddress inetSocketAddress = new InetSocketAddress(u.getHost(), port);
            client.connect(inetSocketAddress, 1000);
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Host", u.getHost() + ":" + port);
            requestHeaders.put("Accept", "text/html");
            requestHeaders.put("Connection", "close");
            requestHeaders.put("User-Agent", "mini brower / java1.8");

            if (gzip) {
                requestHeaders.put("Accept-Encoding", "gzip");
            }

            String path = u.getPath();
            if (path.length() == 0) {
                path = "/";
            }

            if (null != params && isGet) {
                String paramsString = HttpUtil.toParams(params);
                path = path + "?" + paramsString;
            }
            String firstLine = method + " " + path + " HTTP/1.1\r\n";

            StringBuffer httpRequestString = new StringBuffer();
            httpRequestString.append(firstLine);
            Set<String> headers = requestHeaders.keySet();
            for (String header : headers) {
                String headerLine = header + ":" + requestHeaders.get(header) + "\r\n";
                httpRequestString.append(headerLine);
            }

            if (null != params && !isGet) {
                String paramString = HttpUtil.toParams(params);
                httpRequestString.append("\r\n");
                httpRequestString.append(paramString);
            }

            PrintWriter printWriter = new PrintWriter(client.getOutputStream(), true);
            printWriter.println(httpRequestString);
            InputStream is = client.getInputStream();

            result = readBytes(is, true);
            client.close();
        } catch (Exception e) {
            LogFactory.get().info(e.getMessage());
            result = e.toString().getBytes(StandardCharsets.UTF_8);
        }
        return result;
    }

    public static byte[] readBytes(InputStream is, boolean fully) throws IOException {
        //准备1024长度的缓存，不断从输入流读出到缓存中
        int bufferSize = 1024;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[bufferSize];
        while (true) {
            int length = is.read(buffer);
            //读取到的长度是-1，那么就表示到头了，停止循环
            if (-1 == length) {
                break;
            }
            //将读取到的数据根据实际长度写出到一个字节数组输出流
            baos.write(buffer, 0, length);
            //长度小于bufferSize说明读完  当fully为true即使读到的数据没有bufferSize这么长也会继续读取
            if (!fully && length != bufferSize) {
                break;
            }
        }
        //将ByteArrayOutputStream中的数组导出
        return baos.toByteArray();
    }
}
