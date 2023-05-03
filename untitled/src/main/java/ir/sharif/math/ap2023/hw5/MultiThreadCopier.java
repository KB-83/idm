package ir.sharif.math.ap2023.hw5;

public class MultiThreadCopier {
    public static final long SAFE_MARGIN = 6;
    private SourceProvider sourceProvider;
    private ThreadsManager threadsManager;

    public MultiThreadCopier(SourceProvider sourceProvider, String dest, int workerCount) {
        this.sourceProvider = sourceProvider;
        // init  tM
        long size = sourceProvider.size();
        threadsManager = new ThreadsManager(sourceProvider,workerCount,dest,size);

    }
    public void start() {
        threadsManager.start();
    }
}
