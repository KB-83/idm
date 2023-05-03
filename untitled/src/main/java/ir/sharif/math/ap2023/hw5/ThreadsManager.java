package ir.sharif.math.ap2023.hw5;

import java.io.IOException;
import java.util.concurrent.Semaphore;

public class ThreadsManager {
    private final DThread[] threads;
    private final int workerCount;
    private final long size;
    private final String dest;
    private final SourceProvider sourceProvider;
    private final Semaphore semaphore;
    public ThreadsManager(SourceProvider sourceProvider,
                          int workerCount,String dest,long size) {
        this.semaphore = new Semaphore(workerCount);
        threads = new DThread[workerCount];
        // dependencies
        this.sourceProvider = sourceProvider;
        this.size = size;
        this.dest = dest;
        this.workerCount = workerCount;
        initThreads();
    }
    private void initThreads() {
        long eachThreadLoad = size / workerCount;
        int lastThreadExtraWork = (int) (size - (eachThreadLoad * workerCount));
        for (int i =0; i< workerCount - 1; i++) {
            threads [i] = new DThread(this,dest, (i*eachThreadLoad),eachThreadLoad,sourceProvider,i);
        }
        threads[workerCount - 1] = new DThread(this,dest,(workerCount - 1)* eachThreadLoad,
                eachThreadLoad + lastThreadExtraWork, sourceProvider,workerCount - 1);

    }
    public void start() {
        for (DThread thread:threads) {
            thread.start();
        }
    }
    public DThread mostLoad() {
        DThread mostLoad = threads[0];
        for (DThread thread:threads) {
            if(thread.getRemainingWorkLength() > mostLoad.getRemainingWorkLength()) {
                mostLoad = thread;
            }
        }
        return  mostLoad;
    }
    public synchronized void jobDone(DThread preThread) throws InterruptedException {
        setThreadsPause(true);
        DThread targetThread = mostLoad();
        long totalSize = targetThread.getRemainingWorkLength();
        if(totalSize >= 6) {
            long newThreadSize = totalSize/2;
            long preTSize = totalSize - newThreadSize;
            long threadOffset = targetThread.getOffset() + preTSize;
            // target thread new settings
            targetThread.setRemainingWorkLength(preTSize);
            // preThread new settings
            preThread.setOffset(threadOffset);
            preThread.setRemainingWorkLength(newThreadSize);
            preThread.setSourceReader (preThread.getSourceProvider().connect(threadOffset));
        }
        checkIfEnd();
        setThreadsPause(false);
//        System.out.println(Thread.currentThread());
    }
    private void setThreadsPause(boolean isPaused) {
        if (isPaused){
            for (DThread thread:threads){
                thread.pause();
            }
        }

        else {
            for (DThread thread:threads){
                thread.unpause();
            }
            semaphore.release();
        }
    }
    public void checkIfEnd(){
        for (DThread thread:threads) {
            if (thread.getRemainingWorkLength() > 0) {
                return;
            }
        }
        //closing threads setting
        for (DThread thread:threads) {
//            thread.semaphore.release();
            thread.kill();
            try {
                thread.getWriter().close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
    // getters
    public long getSize() {
        return size;
    }
    public Semaphore getSemaphore() {
        return semaphore;
    }
}
