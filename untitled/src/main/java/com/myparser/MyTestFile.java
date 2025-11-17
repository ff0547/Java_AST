package com.myparser;

import java.io.IOException;
import java.net.Socket;

public class MyTestFile {

    public static void main(String[] args) {
        try {
            // 特征 1: 直接命令执行 (Runtime.exec)
            // 常见于 Webshell 的 CMD 执行功能
            String cmd = "calc.exe";
            Runtime.getRuntime().exec(cmd);

            // 特征 2: 使用 ProcessBuilder 执行命令
            // 另一种常见的命令执行方式，用于绕过简单的 Runtime 过滤
            new ProcessBuilder("notepad.exe").start();

            // 特征 3: 网络连接 (Socket)
            // 常见于反弹 Shell (Reverse Shell) 或连接 C&C 服务器
            String attackerIp = "192.168.1.100";
            int attackerPort = 8888;
            Socket socket = new Socket(attackerIp, attackerPort);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}