/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactoryImpl;
import org.terracotta.modules.ehcache.async.errorhandlers.LoggingErrorHandler;
import org.terracotta.modules.ehcache.async.exceptions.ProcessingBucketAlreadyStartedException;
import org.terracotta.modules.ehcache.async.scatterpolicies.HashCodeScatterPolicy;
import org.terracotta.modules.ehcache.async.scatterpolicies.ItemScatterPolicy;
import org.terracotta.modules.ehcache.async.scatterpolicies.SingleBucketScatterPolicy;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;

/**
 * An AsyncCoordinator allows work to be added and processed asynchronously in a fault-tolerant and high performance
 * fashion.
 */

public class AsyncCoordinatorImpl<E extends Serializable> implements AsyncCoordinator<E> {
  private static final Logger                          LOGGER    = LoggerFactory.getLogger(AsyncCoordinatorImpl.class
                                                                     .getName());
  private static final String                          HONOR_WORK_DELAY_FOR_PROCESSING_DEAD_NODES_PROP_NAME = "com.tc.async.honorWorkDelayForProcessingDeadNodes";
  // will be false by default
  private final boolean                                HONOR_WORK_DELAY_FOR_PROCESSING_DEAD_NODES = Boolean
                                                                                                                .getBoolean(HONOR_WORK_DELAY_FOR_PROCESSING_DEAD_NODES_PROP_NAME);
  private static final String                          BUCKET    = "bucket";
  private static final String                          DELIMITER = ToolkitInstanceFactoryImpl.DELIMITER;
  private final String                                 name;
  private final AsyncConfig                            config;

  /**
   * this ToolkitMap map contains keys based on asyncName-nodeId and value will be linked list of bucketNames (or name
   * of ToolkitList)
   */
  private final ToolkitMap<String, LinkedList<String>> listNamesMap;

  /**
   * lock for this coordinator based on SynchronousWrite
   */
  private final ToolkitLock                            coordinatorLock;
  private final List<ProcessingBucket<E>>              localBuckets;
  private final List<ProcessingBucket<E>>              deadBuckets;

  /**
   * status of this coordinator like STARTED, STOPPED etc
   */
  private volatile Status                              status;
  private ItemScatterPolicy<? super E>                 scatterPolicy;
  private ItemsFilter<E>                               filter;
  private final ClusterInfo                            cluster;
  private final String                                 asyncNameWithNodeId;
  private final Toolkit                                toolkit;
  private final ToolkitInstanceFactory                 toolkitInstanceFactory;
  private ItemProcessor<E>                             processor;
  private AsyncClusterListener                         listner;

  public AsyncCoordinatorImpl(String name, String asyncNameWithNodeId, AsyncConfig config,
                              ToolkitInstanceFactory toolkitInstanceFactory) {
    this.name = name;
    this.asyncNameWithNodeId = asyncNameWithNodeId;// async name with nodeId
    if (null == config) {
      this.config = DefaultAsyncConfig.getInstance();
    } else {
      this.config = config;
    }
    this.localBuckets = new ArrayList<ProcessingBucket<E>>();
    this.deadBuckets = new ArrayList<ProcessingBucket<E>>();
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    this.toolkit = toolkitInstanceFactory.getToolkit();
    this.listNamesMap = toolkitInstanceFactory.getOrCreateAsyncListNamesMap(name);
    ToolkitLockType lockType = config.isSynchronousWrite() ? ToolkitLockType.SYNCHRONOUS_WRITE : ToolkitLockType.WRITE;
    this.coordinatorLock = toolkit.getLock(asyncNameWithNodeId, lockType);
    this.cluster = toolkit.getClusterInfo();
  }

  @Override
  public void start(ItemProcessor<E> itemProcessor, int processingConcurrency, ItemScatterPolicy<? super E> policy) {
    if (null == itemProcessor) throw new IllegalArgumentException("processor can't be null");
    if (processingConcurrency < 1) throw new IllegalArgumentException("processingConcurrency needs to be at least 1");

    if (null == policy) {
      if (1 == processingConcurrency) {
        policy = new SingleBucketScatterPolicy();
      } else {
        policy = new HashCodeScatterPolicy();
      }
    }

    Lock lock = coordinatorLock;
    lock.lock();
    try {
      if (status == Status.STARTED) {
        LOGGER.warn("AsyncCoordinator " + name + " already started");
        return;
      }
      if (scatterPolicy != null) { throw new IllegalArgumentException(
                                                                      "scatterPolicy should have been null for AsyncCoordinator "
                                                                          + name); }
      this.scatterPolicy = policy;
      LinkedList<String> tmpLList = new LinkedList<String>();
      LinkedList<String> nameList = listNamesMap.putIfAbsent(asyncNameWithNodeId, tmpLList);
      if (nameList != null) { throw new IllegalArgumentException("nameList already populated for AsyncCoordinator "
                                                                 + name); }
      nameList = tmpLList;
      this.processor = itemProcessor;
      for (int i = 0; i < processingConcurrency; i++) {
        String bucketName = asyncNameWithNodeId + DELIMITER + BUCKET + DELIMITER + i;
        ToolkitList<E> toolkitList = toolkit.getList(bucketName);
        final ProcessingBucket<E> bucket = new ProcessingBucket<E>(bucketName, config, toolkitList, cluster, processor,
                                                                   LoggingErrorHandler.getInstance());
        bucket.setItemsFilter(filter);
        localBuckets.add(bucket);
        nameList.add(bucketName);
      }
      listNamesMap.put(asyncNameWithNodeId, nameList);
      listner = new AsyncClusterListener();
      cluster.addClusterListener(listner);
      for (ProcessingBucket<E> bucket : localBuckets) {
        startBucket(bucket, false);
      }
      status = Status.STARTED;
    } finally {
      lock.unlock();
    }

    processDeadNodes();

  }

  private void processDeadNodes() {
    // checking if there are any dead nodes and starting threads for those buckets also
    Set<String> deadNodes = determineDeadNodes();
    for (String otherNodeNameListKey : deadNodes) {
      processOtherNode(otherNodeNameListKey);
    }
  }

  private Callable<Boolean> removeOnDestroy(final List<ProcessingBucket<E>> list, final ProcessingBucket<E> bucket) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return list.remove(bucket);
      }
    };
  }

  private void startBucket(ProcessingBucket<E> bucket, boolean workingOnDeadBucket) {
    try {
      bucket.start(workingOnDeadBucket);
    } catch (ProcessingBucketAlreadyStartedException e) {
      stop();
      throw new IllegalStateException(bucket.getBucketName() + " already started for AsyncCoordinator " + name);
    }
  }

  @Override
  public void add(E item) {
    if (null == item) { return; }
    Lock lock = coordinatorLock;
    lock.lock();
    try {
      getStatus().checkRunning();
      final int index = scatterPolicy.selectBucket(localBuckets.size(), item);
      final ProcessingBucket bucket = localBuckets.get(index);
      bucket.add(item);
    } finally {
      lock.unlock();
    }
  }

  private Status getStatus() {
    return status != null ? status : Status.UNINITIALIZED;
  }

  @Override
  public void stop() {
    Lock lock = coordinatorLock;
    lock.lock();
    try {
      getStatus().checkRunning();
      status = Status.STOPPED;
      for (ProcessingBucket<E> bucket : localBuckets) {
        bucket.stop();
      }
      localBuckets.clear();
      List<ProcessingBucket<E>> deadBucketsCopy = new ArrayList<ProcessingBucket<E>>(deadBuckets);
      for (ProcessingBucket<E> bucket : deadBucketsCopy) {
        bucket.stop();
      }
      deadBuckets.clear();
      scatterPolicy = null;
      if (cluster != null) {
        cluster.removeClusterListener(listner);
      }
      listNamesMap.remove(asyncNameWithNodeId);
    } finally {
      lock.unlock();
    }

  }

  /**
   * Attach the specified {@code QuarantinedItemsFilter} to this coordinator.
   * <p>
   * A quarantined items filter allows scheduled work to be filtered (and possibly skipped) before being executed.
   * <p>
   * Assigning {@code null} as the quarantined filter causes any existing filter to be removed.
   * 
   * @param filter filter to be applied
   */
  @Override
  public void setOperationsFilter(ItemsFilter<E> filter) {
    Lock lock = coordinatorLock;
    lock.lock();
    try {
      this.filter = filter;
      if (localBuckets != null) {
        for (ProcessingBucket<E> bucket : localBuckets) {
          bucket.setItemsFilter(filter);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  private class AsyncClusterListener implements ClusterListener {
    @Override
    public void onClusterEvent(ClusterEvent event, ClusterInfo clusterInfo) {
      switch (event.getType()) {
        case NODE_LEFT:
          String otherNodeNameListKey = toolkitInstanceFactory.getAsyncNode(name, event.getNode().getId());
          processOtherNode(otherNodeNameListKey);
          break;
        default:
          break;
      }
    }

  }

  private void processOtherNode(String otherNodeNameListKey) {
    AsyncConfig deadNodeProcessingConfig = new AsyncConfigAdapter(config) {

      @Override
      public long getWorkDelay() {
        if (HONOR_WORK_DELAY_FOR_PROCESSING_DEAD_NODES) {
          return super.getWorkDelay();
        } else {
          return 0;
        }
      }

    };
    Lock lock = coordinatorLock;
    lock.lock();
    try {
      if (status == Status.STARTED) {
        LinkedList<String> oldNameList = listNamesMap.get(otherNodeNameListKey);
        LinkedList<String> newOwner = listNamesMap.get(asyncNameWithNodeId);
        if (oldNameList != null) {
          for (String bucketName : oldNameList) {
            ToolkitList<E> toolkitList = toolkit.getList(bucketName);
            ProcessingBucket<E> bucket = new ProcessingBucket<E>(bucketName, deadNodeProcessingConfig, toolkitList,
                                                                 cluster, processor, LoggingErrorHandler.getInstance());
            bucket.setDestroyCallback(removeOnDestroy(deadBuckets, bucket));
            bucket.setItemsFilter(filter);
            deadBuckets.add(bucket);
            newOwner.add(bucketName);
          }
          listNamesMap.remove(otherNodeNameListKey); // removing buckets from old node
          listNamesMap.put(asyncNameWithNodeId, newOwner); // transferring bucket ownership to new node
          for (ProcessingBucket<E> bucket : deadBuckets) {
            startBucket(bucket, true);
          }
        }
      }
    } finally {
      lock.unlock();
    }
  }

  private Set<String> determineDeadNodes() {
    Set<String> deadNodes = Collections.EMPTY_SET;
    // check if the all the known nodes still exist in the cluster
    if (cluster != null) {
      deadNodes = new HashSet<String>(listNamesMap.keySet());
      for (ClusterNode node : cluster.getClusterTopology().getNodes()) {
        deadNodes.remove(toolkitInstanceFactory.getAsyncNode(name, node.getId()));
      }
    }
    return deadNodes;
  }

  private static enum Status {
    UNINITIALIZED, STARTED {
      @Override
      final void checkRunning() {
        // All good!
      }
    },
    STOPPED;

    void checkRunning() {
      throw new IllegalStateException("AsyncCoordinator is " + this.name().toLowerCase() + "!");
    }
  }

  @Override
  public long getQueueSize() {
    Lock lock = coordinatorLock;
    lock.lock();
    try {
      status.checkRunning();
      long size = 0;
      for (ProcessingBucket<E> bucket : localBuckets) {
        size += bucket.getWaitCount();
      }
      return size;
    } finally {
      lock.unlock();
    }
  }

}
