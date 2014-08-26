package com.tutorial;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CyclicBarrier.html
 * <p/>
 * CyclicBarrier用来实现线程间的协调、依赖等待。
 * <p/>
 * 1. 实现工作线程在完成任务后通知主线程，直到所有线程都声明是完成得。这样主线程就开始下一步的工作。
 * 2. 可重置、重用
 * 3. 使用all-or-none策略来控制工作线程的状态。
 * 只要有一个线程发现中断、失败、超时，其他等待的工作线程全部被终止
 * <p/>
 * 4. 常用方法:
 * await() 通知主线程任务已完成
 * <p/>
 * <p/>
 * Created by yaming_deng on 14-7-3.
 */
public class CyclicBarrierTest {

    private static AtomicInteger count = new AtomicInteger();

    static class Worker implements Runnable {

        int myRow;
        CyclicBarrier barrier;

        Worker(int row, final CyclicBarrier barrier) {
            this.myRow = row;
            this.barrier = barrier;
        }

        public void run() {
            processRow(myRow);
            try {
                barrier.await();
            } catch (InterruptedException ex) {
                return;
            } catch (BrokenBarrierException ex) {
                return;
            }
        }

        private void processRow(int c) {
            System.out.println(Thread.currentThread().getId() + " processRow: " + c);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count.incrementAndGet();
        }

    }

    static class Solver {

        final int N;
        final float[][] data;
        final CyclicBarrier barrier;
        boolean done;

        public Solver(float[][] matrix) {
            done = false;
            data = matrix;
            N = matrix.length;
            barrier = new CyclicBarrier(N,
                    new Runnable() {
                        public void run() {
                            mergeRows();
                        }
                    }
            );
        }

        private void mergeRows() {
            System.out.println("Main mergeRows: " + count.get());
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            done = true;
        }

        private void waitUntilDone() {
            System.out.println("Main waiting");
            while (!done){
                System.out.println("Main barrier: " + barrier.getNumberWaiting());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            System.out.println("Main Done.");
        }

        public void start(){
            for (int i = 0; i < N; ++i)
                new Thread(new Worker(i, barrier)).start();

            waitUntilDone();
        }
    }

    public static void main() throws InterruptedException {
        float[][] data = new float[20][2];
        for(int i=0; i<data.length;i++) {
            data[i][0] = i + 1;
            data[i][1] = i + 2;
        }

        Solver solver = new Solver(data);
        solver.start();

    }

}
