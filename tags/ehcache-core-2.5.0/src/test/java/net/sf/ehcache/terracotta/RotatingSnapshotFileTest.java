package net.sf.ehcache.terracotta;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class RotatingSnapshotFileTest {

    private static final String DIRECTORY = "./dumps";
    private RotatingSnapshotFile snapshotFile;
    private File cleanMeUp;

    @Before
    public void setup() {
        snapshotFile = new RotatingSnapshotFile(DIRECTORY, "test");
    }

    @Test
    public void testSuccessfulWrite() throws IOException {

        Set<String> keys = populateWithValues(snapshotFile, 10000);
        assertThat(snapshotFile.currentSnapshotFile().exists(), is(true));
        assertThat(snapshotFile.tempSnapshotFile().exists(), is(false));
        assertThat(snapshotFile.newSnapshotFile().exists(), is(false));
        assertThat(snapshotFile.currentSnapshotFile().length() > 0, is(true));

        final Set<String> snapshot = snapshotFile.readAll();
        assertThat(snapshot.size(), equalTo(keys.size()));
        for (String key : snapshot) {
            keys.remove(key);
        }
        assertThat(keys.isEmpty(), is(true));
    }

    @Test
    public void testStopsOnThreadInterrupted() throws IOException, InterruptedException {

        final RotatingSnapshotFile file = new RotatingSnapshotFile(DIRECTORY, "killMe");
        final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
        Set<String> keys = populateWithValues(file, 10000);
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        assertThat(file.newSnapshotFile().exists(), is(false));

        Thread writerThread = new Thread() {

            int valuesAskedWhileInterrupted = 0;

            @Override
            public void run() {
                try {
                    file.writeAll(new Iterable<Object>() {
                        public Iterator<Object> iterator() {
                            return new Iterator<Object>() {
                                public boolean hasNext() {
                                    if (isInterrupted() && ++valuesAskedWhileInterrupted > 1) {
                                        throwable.set(new AssertionError("We shouldn't be asked for more values by now!"));
                                        return false;
                                    }
                                    return true;
                                }

                                public Object next() {
                                    return UUID.randomUUID();
                                }

                                public void remove() {
                                    // Just don't do anything...
                                }
                            };
                        }
                    });
                } catch (Throwable e) {
                    throwable.set(e);
                }
            }
        };

        writerThread.start();
        Thread.sleep(500);
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(true));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        file.setShutdownOnThreadInterrupted(true);
        writerThread.interrupt();
        writerThread.join();
        assertThat(throwable.get(), CoreMatchers.<Object>nullValue());
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(true));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        final Set<Object> values = file.readAll();
        assertThat(values.size(), equalTo(keys.size()));
        for (Object key : values) {
            keys.remove(key);
        }
        assertThat(keys.isEmpty(), is(true));
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(false));
        assertThat(file.tempSnapshotFile().exists(), is(false));

        cleanMeUp = file.currentSnapshotFile();
    }

    @Test
    public void testFinishesOnThreadInterrupted() throws IOException, InterruptedException {

        final RotatingSnapshotFile file = new RotatingSnapshotFile(DIRECTORY, "killMe");
        final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
        Set<String> keys = populateWithValues(file, 10000);
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        assertThat(file.newSnapshotFile().exists(), is(false));

        final int keyAmount = 100;
        final String keyPrefix = "Finish with value ";
        Thread writerThread = new Thread() {

            int valuesToDispenseOnceInterrupted = keyAmount;

            @Override
            public void run() {
                try {
                    file.writeAll(new Iterable<Object>() {
                        public Iterator<Object> iterator() {
                            return new Iterator<Object>() {
                                public boolean hasNext() {
                                    return !isInterrupted() || valuesToDispenseOnceInterrupted-- > 0;
                                }

                                public Object next() {
                                    if (isInterrupted()) {
                                        return keyPrefix + valuesToDispenseOnceInterrupted;
                                    }
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                        interrupt();
                                    }
                                    return UUID.randomUUID();
                                }

                                public void remove() {
                                    // Just don't do anything...
                                }
                            };
                        }
                    });
                } catch (Throwable e) {
                    throwable.set(e);
                }
            }
        };

        writerThread.start();
        Thread.sleep(500);
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(true));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        writerThread.interrupt();
        writerThread.join();
        assertThat(throwable.get(), CoreMatchers.<Object>nullValue());
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(false));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        final Set<Object> values = file.readAll();
        for (Object key : keys) {
            assertThat(values.contains(key), is(false));
        }
        for (int i = 0; i < keyAmount; i++) {
            final String key = keyPrefix + i;
            assertThat(key + " is missing!", values.contains(key), is(true));
        }
        assertThat(values.contains(keyPrefix + keyAmount), is(false));
        assertThat(values.size() > keyAmount, is(true));
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(false));
        assertThat(file.tempSnapshotFile().exists(), is(false));

        cleanMeUp = file.currentSnapshotFile();
    }

    private Set<String> populateWithValues(RotatingSnapshotFile snapshotFile, int amount) throws IOException {
        Set<String> keys = new HashSet<String>();
        for (int i = 0; i < amount; i++) {
            keys.add("SomeKey that contains something " + i);
        }

        snapshotFile.writeAll(keys);
        return keys;
    }

    @After
    public void cleanUp() {
        if (cleanMeUp != null) {
            deleteSilently(cleanMeUp);
        }

        if (snapshotFile != null) {
            deleteSilently(snapshotFile.currentSnapshotFile());
            deleteSilently(snapshotFile.newSnapshotFile());
            deleteSilently(snapshotFile.tempSnapshotFile());
            deleteSilently(new File(DIRECTORY));
        }
    }

    private void deleteSilently(final File file) {
        try {
            if (file.exists()) {
                file.delete();
            }
        } catch (Throwable e) {
            System.err.println("Error deleting file " + file.getAbsolutePath() + ": " + e.getMessage());
        }
    }

}
