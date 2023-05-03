package ir.sharif.math.ap2023.hw5;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;

public class DThread extends Thread{
    private final int number;
    private Semaphore semaphore;
    private Writer writer;
    private SourceReader sourceReader;
    private final SourceProvider sourceProvider;
    private long workLength;
    private volatile long remainingWorkLength;
    private boolean isPaused = false;
    private ThreadsManager tM;
    private long offset;
    private boolean isAlive = true;
    public DThread(ThreadsManager tM,String dest, long offset,
                   long length, SourceProvider sourceProvider, int number) {
        // dependencies
        this.tM = tM;
        this.sourceProvider = sourceProvider;
        this.number = number;
        this.offset = offset;
        workLength = length;
        remainingWorkLength = workLength;
        initWriter(dest);
    }
    private void initWriter(String dest) {
        File file = new File(dest);
        try {
            writer = new Writer(file,tM.getSize());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void run(){
        // not letting otherThreads here
        sourceReader = sourceProvider.connect(offset);
        this.semaphore = tM.getSemaphore();
        try {
            while (isAlive) {
                while (remainingWorkLength > 0) {
                    byte[] b = {sourceReader.read()};
                    System.out.println(offset+"  "+number);
                        writer.seek(offset);
                        writer.write(b, 0, 1);

                    offset++;
                    remainingWorkLength--;
                    if (isPaused) {
                        semaphore.acquire(); // acquire the semaphore to wait for the pause condition to be released
                        System.out.println("if paused" + number);
                    }
                    if (remainingWorkLength == 0) {
                        tM.jobDone(this);
                    }
                }
                tM.checkIfEnd();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void pause() {
        isPaused = true;
    }
    public void unpause() {
        isPaused = false;
    }
    public void kill(){
        isAlive = false;
    }
    // setters getters
    public long getRemainingWorkLength() {
        return remainingWorkLength;
    }
    public Writer getWriter(){
        return writer;
    }
    public void setSourceReader(SourceReader sourceReader) {
        this.sourceReader = sourceReader;
    }
    public SourceProvider getSourceProvider() {
        return sourceProvider;
    }
    public void setRemainingWorkLength(long remainingWorkLength) {
        this.remainingWorkLength = remainingWorkLength;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }
}
