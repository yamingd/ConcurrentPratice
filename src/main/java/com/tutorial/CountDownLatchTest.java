package com.tutorial;

import java.util.Date;
import java.util.concurrent.CountDownLatch;

/**
 * http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CountDownLatch.html
 * <p/>
 * CountDownLatch是一个线程执行倒计时器。
 * <p/>
 * 1. 能让线程在资源未准备好前，等待资源准备、资源有效释放、执行指令后才执行实际逻辑代码
 * 2. 可用在多个线程协作交互、分解任务的场景
 * 3. 本倒计时器不可逆，不可重用, 不可重置 (CyclicBarrier可重置, 可重用)
 * 4. 常用方法:
 * await() 目标线程在等待资源或执行指令
 * countDown() 协作线程通知资源已经准备好
 * <p/>
 * Created by yaming_deng on 14-7-3.
 */
public class CountDownLatchTest {

    static class Worker implements Runnable {
        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;

        public Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
        }

        public void run() {
            try {
                System.out.println(Thread.currentThread().getId() + " is waiting: " + new Date().getTime());
                //等待协作线程的完成
                startSignal.await();
                //执行线程逻辑
                doWork();
                //通知协作线程执行完
                doneSignal.countDown();
            } catch (InterruptedException ex) {

            }
        }

        void doWork() {
            System.out.println(Thread.currentThread().getId() + " is running: " + new Date().getTime());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main() throws InterruptedException {
        int N = 10;
        //创建信号量
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(N);
        //启动工作线程
        for (int i = 0; i < N; ++i) {
            // create and start threads
            // start方法会触发JVM启动native线程。实质上start会创建2个线程出来, 而run是真正的线程
            new Thread(new Worker(startSignal, doneSignal)).start();
        }

        //准备资源
        prepare();
        //启动监视线程
        startMonitor();

        //通知Worker线程资源已经准备好
        startSignal.countDown();
        //等待所有线程执行完(countDown=0)
        doneSignal.await();
    }

    private static void prepare() {
        //可以启动资源准备线程
        try {
            Thread.sleep(2000);
            System.out.println("prepare thread has finished.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void startMonitor() {
        //可以启动任务执行情况监视线程
        System.out.println("start monitor thread.");

    }
}
