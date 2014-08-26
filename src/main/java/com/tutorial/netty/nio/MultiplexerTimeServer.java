package com.tutorial.netty.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable {

    private Selector selector;

    private ServerSocketChannel servChannel;

    private volatile boolean stop;

    /**
     * 初始化多路复用器、绑定监听端口
     *
     * @param port
     */
    public MultiplexerTimeServer(int port) {
        try {
            //1.
            selector = Selector.open();
            //2.
            servChannel = ServerSocketChannel.open();
            //3.
            servChannel.configureBlocking(false);
            servChannel.socket().bind(new InetSocketAddress(port), 1024);
            //4.
            servChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("The time server is start in port : " + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stop() {
        this.stop = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        while (!stop) {
            try {
                //5. 休眠时间为1S，无论是否有读写等事件发生，selector每隔1S都被唤醒一次
                selector.select(1000);
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                //6.
                Iterator<SelectionKey> it = selectedKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        handleInput(key);
                    } catch (Exception e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null)
                                key.channel().close();
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        // 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleInput(SelectionKey key) throws IOException {

        if (key.isValid()) {
            // 处理新接入的请求消息
            if (key.isAcceptable()) {
                // Accept the new connection
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                //7.
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                //8. Add the new connection to the selector
                sc.register(selector, SelectionKey.OP_READ);
                System.out.println("Accept connection from " + sc.socket().getRemoteSocketAddress());
                doWrite(sc, "Got you!");
            }
            if (key.isReadable()) {
                // 客户端连接 Read the data
                SocketChannel sc = (SocketChannel) key.channel();
                System.out.println("Read data from " + sc.socket().getRemoteSocketAddress());
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                // read是非阻塞的！
                int readBytes = sc.read(readBuffer);
                if (readBytes > 0) {
                    //重置缓冲区。 将缓冲区当前的limit设置为position，position设置为0，用于后续对缓冲区的读取操作
                    readBuffer.flip();
                    //构造返回消息体
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String body = new String(bytes, "UTF-8");
                    System.out.println("The time server receive order : " + body);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ?
                            new java.util.Date(System.currentTimeMillis()).toString()
                            : "BAD ORDER";
                    //返回消息体
                    doWrite(sc, currentTime);
                } else if (readBytes < 0) {
                    // 对端链路关闭
                    key.cancel();
                    sc.close();
                    System.out.println("Connection Closed");
                } else {
                    System.out.println("Empty"); // 读到0字节，忽略
                }
            }
        }
    }

    private void doWrite(SocketChannel channel, String response)
            throws IOException {
        if (response != null && response.trim().length() > 0) {
            // 将字符串编码成字节数组
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);
            //将buffer切换成read模式
            writeBuffer.flip();
            //往客户端返回数据
            channel.write(writeBuffer);

            // SocketChannel是异步非阻塞的，它并不保证一次能够把需要发送的字节数组发送完，此时会出现“写半包”问题，
            // 我们需要注册写操作，不断轮询Selector将没有发送完的ByteBuffer发送完毕，
            // 可以通过ByteBuffer的hasRemain()方法判断消息是否发送完成
        }
    }
}

