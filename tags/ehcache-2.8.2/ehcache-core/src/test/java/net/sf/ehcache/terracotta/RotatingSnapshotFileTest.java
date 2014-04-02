package net.sf.ehcache.terracotta;

import net.sf.ehcache.DiskStorePathManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class RotatingSnapshotFileTest {

    @Rule
    public final TemporaryFolder directory = new TemporaryFolder();
    
    @Test
    public void testSuccessfulWrite() throws IOException {
        RotatingSnapshotFile snapshotFile = new RotatingSnapshotFile(new DiskStorePathManager(directory.getRoot().getAbsolutePath()), "test");
        Set<Object> keys = populateWithValues(snapshotFile, 100);
        assertThat(snapshotFile.currentSnapshotFile().exists(), is(true));
        assertThat(snapshotFile.tempSnapshotFile().exists(), is(false));
        assertThat(snapshotFile.newSnapshotFile().exists(), is(false));
        assertThat(snapshotFile.currentSnapshotFile().length(), greaterThan(0L));
        assertThat(snapshotFile.readAll(), equalTo(keys));
    }

    @Test
    public void testStopsOnThreadInterrupted() throws IOException, InterruptedException {
        final RotatingSnapshotFile file = new RotatingSnapshotFile(new DiskStorePathManager(directory.getRoot().getAbsolutePath()), "killMe");
        final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
        Set<Object> keys = populateWithValues(file, 100);
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        assertThat(file.newSnapshotFile().exists(), is(false));

        final CountDownLatch latch = new CountDownLatch(100);
        Thread writerThread = new Thread() {

            int valuesAskedWhileInterrupted = 0;

            @Override
            public void run() {
                try {
                    file.writeAll(new Iterable<String>() {
                        public Iterator<String> iterator() {
                            return new Iterator<String>() {
                                public boolean hasNext() {
                                    if (isInterrupted() && ++valuesAskedWhileInterrupted > 1) {
                                        throwable.set(new AssertionError("We shouldn't be asked for more values by now!"));
                                        return false;
                                    }
                                    return true;
                                }

                                public String next() {
                                    latch.countDown();
                                    return UUID.randomUUID().toString();
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
        latch.await();
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(true));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        file.setShutdownOnThreadInterrupted(true);
        writerThread.interrupt();
        writerThread.join();
        assertThat(throwable.get(), nullValue());
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(true));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        assertThat(file.readAll(), equalTo(keys));
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(false));
        assertThat(file.tempSnapshotFile().exists(), is(false));
    }

    @Test
    public void testFinishesOnThreadInterrupted() throws IOException, InterruptedException {
        final RotatingSnapshotFile file = new RotatingSnapshotFile(new DiskStorePathManager(directory.getRoot().getAbsolutePath()), "killMe");
        final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
        Set<Object> keys = populateWithValues(file, 100);
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        assertThat(file.newSnapshotFile().exists(), is(false));

        final CountDownLatch latch = new CountDownLatch(100);
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
                                    return !isInterrupted() || valuesToDispenseOnceInterrupted > 0;
                                }

                                public Object next() {
                                    if (isInterrupted()) {
                                        return keyPrefix + (valuesToDispenseOnceInterrupted--);
                                    } else {
                                        latch.countDown();
                                        return UUID.randomUUID();
                                    }
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
        latch.await();
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(true));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        writerThread.interrupt();
        writerThread.join();
        assertThat(throwable.get(), nullValue());
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(false));
        assertThat(file.tempSnapshotFile().exists(), is(false));
        final Set<Object> values = file.readAll();
        for (Object key : keys) {
            assertThat(values, not(hasItem(key)));
        }
        for (int i = 1; i <= keyAmount; i++) {
            assertThat(values, hasItem(keyPrefix + i));
        }
        assertThat(values, not(hasItem(keyPrefix + 0)));
        assertThat(values, hasSize(greaterThanOrEqualTo(keyAmount)));
        assertThat(file.currentSnapshotFile().exists(), is(true));
        assertThat(file.newSnapshotFile().exists(), is(false));
        assertThat(file.tempSnapshotFile().exists(), is(false));
    }

    private Set<Object> populateWithValues(RotatingSnapshotFile snapshotFile, int amount) throws IOException {
        Set<Object> keys = new HashSet<Object>();
        for (int i = 0; i < amount; i++) {
            keys.add("SomeKey that contains something " + i);
        }

        snapshotFile.writeAll(keys);
        return keys;
    }
}
