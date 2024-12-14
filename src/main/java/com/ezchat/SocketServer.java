package com.ezchat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SocketServer {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        //缓存所有连接服务器的对象
        Map<String, Socket> CLIENT_MAP = new HashMap<String, Socket>();
        try {
            serverSocket = new ServerSocket(5648);
            System.out.println("客户端启动,等待客户端连接");
            //循环接收新的连接
            while (true) {
                Socket socket = serverSocket.accept();
                String ip = socket.getInetAddress().getHostAddress();
                System.out.println("客户端链接，ip：" + ip + ",端口：" + socket.getPort());
                //Key
                String clientKey = ip + ":" + socket.getPort();
                CLIENT_MAP.put(clientKey, socket);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                //接收消息
                                InputStream inputStream = null;
                                inputStream = socket.getInputStream();
                                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "Utf-8");
                                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                                String readData = bufferedReader.readLine();
                                System.out.println("收到客户端" + ip + ":" + socket.getPort() + "消息-->" + readData);

                                //给每个客户端对象回复消息
                                CLIENT_MAP.forEach((k, v) -> {
                                    try {
                                        OutputStream outputStream = v.getOutputStream();
                                        PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream, "Utf-8"));
                                        //socket.getPort() 获取的是当前线程正在处理的客户端 Socket 对象的端口号（即发送消息的客户端）。
                                        // 这是因为 socket 是从 serverSocket.accept() 返回的，它表示刚连接的客户端 Socket，当前线程专门处理这个 Socket。
                                        //v.getPort() 表示当前迭代到的客户端，而不是消息发送者。
                                        // 所以，v.getPort() 获取的端口号可能是任何已连接客户端的端口号，而不一定是实际发送消息的客户端端口号。
                                        printWriter.println(ip + ":" + socket.getPort() + "发送了消息：" + readData);
                                        printWriter.flush();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                });

                            } catch (IOException e) {
                                e.printStackTrace();
                                break;//报异常跳出死循环
                            }
                        }
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
