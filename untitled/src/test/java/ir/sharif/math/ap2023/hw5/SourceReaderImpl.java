package ir.sharif.math.ap2023.hw5;

import java.util.function.Function;

public class SourceReaderImpl implements SourceReader {
    private final long offset;
    private int current;
    private final MultiThreadCopierTest test;
    private final Thread thread;
    private final Function<Long, Long> getSleepTime;


    SourceReaderImpl(long offset, MultiThreadCopierTest test, Thread thread, Function<Long, Long> getSleepTime) {
        this.offset = offset;
        this.test = test;
        this.thread = thread;
        this.getSleepTime = getSleepTime;
        this.current = -1;
    }

    @Override
    public byte read() {
        if (!Thread.currentThread().equals(thread)) {
            test.setFail("multi thread read from reader. reader: "
                    + Thread.currentThread().getName() + " expected: " + thread.getName());
        }
        this.current++;
        long sleep = getSleepTime.apply(offset + current);
        if (sleep > 0)
            test.sleep(sleep);
        return test.get(offset + current);
    }
}
