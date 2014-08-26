package com.tutorial.netty.guide22;

import com.tutorial.netty.TimeServerHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * 1. 服务端处理缓慢，返回应答消息耗费60S，平时只需要10MS；
   2. 采用伪异步IO的线程正在读取故障服务节点的响应，由于读取输入流是阻塞的，因此，它将会被同步阻塞60S；
   3. 假如所有的可用线程都被故障服务器阻塞，那后续所有的IO消息都将在队列中排队；
   4. 由于线程池采用阻塞队列实现，当队列积满之后，后续入队列的操作将被阻塞；
   5. 由于前端只有一个Accptor线程接收客户端接入，它被阻塞在线程池的同步阻塞队列之后，新的客户端请求消息将被拒绝，客户端会发生大量的连接超时；
   6. 由于几乎所有的连接都超时，调用者会认为系统已经崩溃，无法接收新的请求消息。
 *
 */
public class TimeServer {

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                // 采用默认值
            }
        }
        ServerSocket server = null;
        try {
            server = new ServerSocket(port);
            System.out.println("The time server is start in port : " + port);
            Socket socket = null;
            TimeServerHandlerExecutePool singleExecutor = new TimeServerHandlerExecutePool(
                    50, 10000);// 创建IO任务线程池
            while (true) {
                socket = server.accept();
                singleExecutor.execute(new TimeServerHandler(socket));
            }
        } finally {
            if (server != null) {
                System.out.println("The time server close");
                server.close();
                server = null;
            }
        }
    }
}

