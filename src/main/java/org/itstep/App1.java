package org.itstep;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class App1 {
    public static void main(String[] args) throws IOException, InterruptedException {

        InputStream input = new FileInputStream("alice.txt");
        OutputStream output = new FileOutputStream("copyAlice.txt");

        final ReadWriteLock lock = new ReentrantReadWriteLock();

        final int nThreads = 10;

        ExecutorService executor = Executors.newFixedThreadPool(nThreads);

        for (int i = 0; i < nThreads; i++) {
            executor.submit(new ReadWriteFile(input, output, lock));
        }

        try {
            System.out.println("attempt to shutdown executor");
            executor.shutdown();
            //wait 3 seconds for completion
            executor.awaitTermination(60, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            System.err.println("tasks interrupted");
        }
        finally {
            //if service is not completed yet
            if (!executor.isTerminated()) {
                executor.shutdownNow();
                System.err.println("Make it to stop");
            }
            System.out.println("shutdown finished");
        }

        output.close();
    }

}

class ReadWriteFile implements Runnable {
    private final InputStream in;
    private final OutputStream out;
    private final ReadWriteLock lock;

    public ReadWriteFile(InputStream in, OutputStream out, ReadWriteLock lock) {
        this.in = in;
        this.out = out;
        this.lock = lock;
    }

    public void readWrite() throws IOException, InterruptedException {
        final int part = 4096;
        int bytes = 0;
        byte[] line = new byte[part];
        bytes = in.available();
        while (bytes > 0) {
            this.lock.readLock().tryLock();
            bytes = in.read(line);
            this.lock.readLock().unlock();
            this.lock.writeLock().tryLock();
            out.write(line, 0, bytes);
            this.lock.writeLock().unlock();
            System.out.println(Thread.currentThread().getName());
            Thread.sleep(100);
        }
    }

    @Override
    public void run() {
        try {
            readWrite();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
