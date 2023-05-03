package ir.sharif.math.ap2023.hw5;

import org.junit.*;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MultiThreadCopierTest {
    private static final long TIME_SAFE_MARGIN = 50;
    private static final long SLEEP1 = 200;
    private static final long SLEEP2 = 100;
    private static final Path FILE_PATH = Paths.get("a.result");

    @Rule
    public final AllRule allRule;
    private Set<Thread> threadSet2;
    private boolean fail;
    private Set<String> failMessages;

    public MultiThreadCopierTest() {
        allRule = new AllRule(this);
    }

    @Before
    public void setUp() {
        deleteFile();
        this.threadSet2 = new CopyOnWriteArraySet<>();
        Thread.setDefaultUncaughtExceptionHandler(this::uncaughtException);
        threadSet2.addAll(getAllThreads());
        this.fail = false;
        this.failMessages = new TreeSet<>();
    }

    private void deleteFile() {
        try {
            Files.deleteIfExists(FILE_PATH);
        } catch (IOException ignore) {
        }
    }

    @Test(timeout = 3000)
    @Repeat(10)
    public void oneWorker() {
        long startTime = System.currentTimeMillis();
        int workerCount = 1;
        SourceProviderImpl sourceProvider = new SourceProviderImpl(100, this, l -> 0L);
        MultiThreadCopier copier = new MultiThreadCopier(sourceProvider, FILE_PATH.toString(), workerCount);
        copier.start();
        long endTime = System.currentTimeMillis();
        assertTime(startTime, endTime);
        sleep(SLEEP1);
        assertFinalResult(100);
        assertEquals("use more thread than worker count", workerCount, sourceProvider.getThreadMap().size());
    }

    @Test(timeout = 3000)
    @Repeat(10)
    public void multiWorker() {
        long startTime = System.currentTimeMillis();
        int workerCount = 8;
        SourceProviderImpl sourceProvider = new SourceProviderImpl(100, this, l -> 0L);
        MultiThreadCopier copier = new MultiThreadCopier(sourceProvider, FILE_PATH.toString(), workerCount);
        copier.start();
        long endTime = System.currentTimeMillis();
        assertTime(startTime, endTime);
        sleep(SLEEP1);
        assertFinalResult(100);
        assertEquals("use more thread than worker count", workerCount, sourceProvider.getThreadMap().size());
    }

    @Test(timeout = 3000)
    @Repeat(10)
    public void multiWorkerTimeCheck() {
        long startTime = System.currentTimeMillis();
        int workerCount = 8;
        int batch = 6;
        SourceProviderImpl sourceProvider = new SourceProviderImpl(workerCount * batch, this, l -> SLEEP2);
        MultiThreadCopier copier = new MultiThreadCopier(sourceProvider, FILE_PATH.toString(), workerCount);
        copier.start();
        long endTime = System.currentTimeMillis();
        assertTime(startTime, endTime);
        sleep(SLEEP2 * batch + TIME_SAFE_MARGIN);
        assertFinalResult(workerCount * batch);
        assertEquals("use more thread than worker count", workerCount, sourceProvider.getThreadMap().size());
    }

    @Test(timeout = 3000)
    @Repeat(10)
    public void multiWorkerOffsetCheck() {
        long startTime = System.currentTimeMillis();
        int workerCount = 8;
        long batch = 6;
        SourceProviderImpl sourceProvider = new SourceProviderImpl(workerCount * batch, this, l -> SLEEP2);
        MultiThreadCopier copier = new MultiThreadCopier(sourceProvider, FILE_PATH.toString(), workerCount);
        copier.start();
        long endTime = System.currentTimeMillis();
        assertTime(startTime, endTime);
        sleep(SLEEP2 * batch + TIME_SAFE_MARGIN);
        assertFinalResult(workerCount * batch);
        assertEquals("use more thread than worker count", workerCount, sourceProvider.getThreadMap().size());
        Map<Thread, List<Long>> threadListMap = sourceProvider.getThreadMap();
        Set<Long> offsets = new TreeSet<>();
        for (Map.Entry<Thread, List<Long>> entry : threadListMap.entrySet()) {
            assertEquals(1, entry.getValue().size());
            long offset = entry.getValue().get(0);
            offsets.add(offset);
        }
        Long[] offsetsArray = offsets.toArray(new Long[0]);
        for (int i = 0; i < offsetsArray.length; i++) {
            assertEquals("check offset", i * batch, (long) offsetsArray[i]);
        }
    }

    @Test(timeout = 12000 )
    @Repeat(5)
    public void multiOffsetCheck() {
        long startTime = System.currentTimeMillis();
        int workerCount = 3;
        long batch = 80;
        SourceProviderImpl sourceProvider = new SourceProviderImpl(workerCount * batch, this, l -> {
            if (l >= (workerCount - 1) * batch && l < workerCount * batch - batch / 4) {
                return SLEEP2;
            } else {
                return SLEEP2 / 2;
            }
        });
        MultiThreadCopier copier = new MultiThreadCopier(sourceProvider, FILE_PATH.toString(), workerCount);
        copier.start();
        long endTime = System.currentTimeMillis();
        assertTime(startTime, endTime);
        sleep(SLEEP2 * batch + TIME_SAFE_MARGIN);
        assertFinalResult(workerCount * batch);
        assertEquals("use more thread than worker count", workerCount, sourceProvider.getThreadMap().size());
        Map<Thread, List<Long>> threadListMap = sourceProvider.getThreadMap();
        List<List<Long>> offsets = new ArrayList<>();
        for (Map.Entry<Thread, List<Long>> entry : threadListMap.entrySet()) {
            assertFalse(entry.getValue().isEmpty());
            offsets.add(entry.getValue());
        }
        offsets.sort(Comparator.comparing(longs -> longs.get(0)));
        assertTrue("worker 1 must has 2 offsets actual:" + offsets.get(0).size(),
                1 < offsets.get(0).size());
        assertTrue("worker 2 must has 2 offsets actual:" + offsets.get(1).size(),
                1 < offsets.get(1).size());
        long min = Math.min(offsets.get(0).get(1), offsets.get(1).get(1));
        long max = Math.max(offsets.get(0).get(1), offsets.get(1).get(1));
        if (min + max < 440) {
            assertEquals("in case 1:incorrect second offset for max", 220, max, 2);
            assertEquals("in case 1:incorrect second offset for min", 210, min, 2);
        } else {
            assertEquals("in case 2:incorrect second offset for max", 230, max, 2);
            assertEquals("in case 2:incorrect second offset for min", 220, min, 2);
        }
    }

    @Test(timeout = 3000)
    @Repeat(10)
    public void checkFileInProgress() {
        long startTime = System.currentTimeMillis();
        int workerCount = 4;
        int batch = 10;
        SourceProviderImpl sourceProvider = new SourceProviderImpl(workerCount * batch, this, l -> SLEEP1);
        MultiThreadCopier copier = new MultiThreadCopier(sourceProvider, FILE_PATH.toString(), workerCount);
        copier.start();
        long endTime = System.currentTimeMillis();
        assertTime(startTime, endTime);
        sleep(SLEEP1 * (batch / 2) + TIME_SAFE_MARGIN);
        assertInProgressResult(batch, workerCount);
        sleep(SLEEP1 * (batch / 2) + TIME_SAFE_MARGIN);
        assertFinalResult(workerCount * batch);
        assertEquals("use more thread than worker count", workerCount, sourceProvider.getThreadMap().size());
    }

    private void assertInProgressResult(int batch, int workerCount) {
        try {
            byte[] content = Files.readAllBytes(FILE_PATH);
            assertEquals("invalid result file length", batch * workerCount, content.length);
            for (int i = 0; i < workerCount; i++) {
                for (int j = i * batch; j < i * batch + (batch / 2); j++) {
                    assertEquals("invalid result file index: " + j, get(j), content[j]);
                }
                for (int j = i * batch + (batch / 2); j < (i + 1) * batch; j++) {
                    assertEquals("invalid result file index: " + j, 0, content[j]);
                }
            }

        } catch (IOException e) {
            fail();
        }
    }

    private void assertFinalResult(long length) {
        try {
            byte[] content = Files.readAllBytes(FILE_PATH);
            assertEquals("invalid result file length", length, content.length);
            for (int i = 0; i < content.length; i++) {
                assertEquals("invalid result file index: " + i, get(i), content[i]);
            }
        } catch (IOException e) {
            fail();
        }
    }

    @SuppressWarnings("deprecation")
    @After
    public void tearDown() {
        sleep(50);
        deleteFile();
        System.gc();
        for (Thread thread : getAllThreads()) {
            if (thread != null && !threadSet2.contains(thread)) {
                this.setFail("not stopped thread: " + thread.getName());
                thread.stop();
            }
        }
        sleep(30);
        System.gc();
    }


    public boolean isFail() {
        return fail;
    }

    public String getFailMessage() {
        return String.join(" ", failMessages);
    }

    private void assertTime(long start, long end) {
        assertTrue(end - start <= MultiThreadCopierTest.TIME_SAFE_MARGIN);
    }

    private Set<Thread> getAllThreads() {
        return Thread.getAllStackTraces().keySet();
    }

    private void uncaughtException(Thread thread, Throwable throwable) {
        if (!threadSet2.contains(thread) && throwable instanceof Exception) {
            throwable.printStackTrace();
            this.setFail("exception in thread: " + thread.getName());
        }
    }

    void sleep(long sleepTime) {
        long startTime = System.currentTimeMillis();
        long sleepLeft = sleepTime;
        while (sleepLeft > 0) {
            try {
                Thread.sleep(sleepLeft);
            } catch (InterruptedException ignore) {
            }
            sleepLeft = startTime + sleepTime - System.currentTimeMillis();
        }
    }

    public byte get(long i) {
        return (byte) ((i % 26) + 97);
    }

    public void setFail(String failMessage) {
        this.failMessages.add(failMessage);
        this.fail = true;
    }
}