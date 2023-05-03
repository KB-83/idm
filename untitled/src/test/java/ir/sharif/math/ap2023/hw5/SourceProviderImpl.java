package ir.sharif.math.ap2023.hw5;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class SourceProviderImpl implements SourceProvider {
    private final long size;
    private final MultiThreadCopierTest test;
    private final Function<Long, Long> getSleepTime;
    private final Map<Thread, List<Long>> threadMap;

    SourceProviderImpl(long size, MultiThreadCopierTest test, Function<Long, Long> getSleepTime) {
        this.size = size;
        this.test = test;
        this.getSleepTime = getSleepTime;
        this.threadMap = new ConcurrentHashMap<>();
    }

    @Override
    public SourceReader connect(long offset) {
        Thread thread = Thread.currentThread();
        List<Long> offsets = this.threadMap.getOrDefault(thread, new CopyOnWriteArrayList<>());
        offsets.add(offset);
        this.threadMap.put(thread, offsets);
        return new SourceReaderImpl(offset, test, Thread.currentThread(), getSleepTime);
    }

    public Map<Thread, List<Long>> getThreadMap() {
        return threadMap;
    }

    @Override
    public long size() {
        return this.size;
    }
}
