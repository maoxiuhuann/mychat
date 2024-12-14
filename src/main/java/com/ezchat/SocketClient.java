package com.ezchat;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class SocketClient {
    public static void main(String[] args) {
        Socket socket = null;
        try{
            socket = new Socket("127.0.0.1", 5648);

            OutputStream outputStream = socket.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream);
            //给服务端发送消息
            System.out.println("请输入内容：");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        Scanner scanner = new Scanner(System.in);
                        String input = scanner.nextLine();
                        try{
                            printWriter.println(input);
                            printWriter.flush();
                        }catch (Exception e){
                            e.printStackTrace();
                            break;//报异常跳出死循环
                        }
                    }
                }
            }).start();


            //接受服务端消息
            InputStream inputStream = null;
            inputStream = socket.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"Utf-8");
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true){
                        try {
                            String readData = bufferedReader.readLine();
                            System.out.println("收到服务端发来的确认消息-->" + readData);
                        } catch (IOException e) {
                            e.printStackTrace();
                            break;//报异常跳出死循环
                        }
                    }
                }
            }).start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
