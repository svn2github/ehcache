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
import org.terracotta.toolkit.collections.ToolkitStore;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitLockType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * An AsyncCoordinator allows work to be added and processed asynchronously in a fault-tolerant and high performance
 * fashion.
 */

public class AsyncCoordinatorImpl<E extends Serializable> implements AsyncCoordinator<E> {
  private static final Logger             LOGGER                                               = LoggerFactory
                                                                                                   .getLogger(AsyncCoordinatorImpl.class
                                                                                                       .getName());
  private static final String             HONOR_WORK_DELAY_FOR_PROCESSING_DEAD_NODES_PROP_NAME = "com.tc.async.honorWorkDelayForProcessingDeadNodes";
  // will be false by default
  private static final boolean            HONOR_WORK_DELAY_FOR_PROCESSING_DEAD_NODES           = Boolean
                                                                                                   .getBoolean(HONOR_WORK_DELAY_FOR_PROCESSING_DEAD_NODES_PROP_NAME);
  private static final String             BUCKET                                               = "bucket";
  private static final String             DELIMITER                                            = ToolkitInstanceFactoryImpl.DELIMITER;
  private final String                    name;
  private final AsyncConfig               config;

  private final BucketMetaInfoHandler<E>  bucketMetaInfoHandler;

  /**
   * lock for this coordinator based on SynchronousWrite
   */
  private final ToolkitLock               commonAsyncLock;
  private final ToolkitLock               nodeWriteLock;
  private final ToolkitLock               nodeReadLock;

  private final List<ProcessingBucket<E>> localBuckets;
  private final List<ProcessingBucket<E>> deadBuckets;

  /**
   * status of this coordinator like STARTED, STOPPED etc
   */
  private volatile Status                 status                                               = Status.UNINITIALIZED;
  private ItemScatterPolicy<? super E>    scatterPolicy;
  private ItemsFilter<E>                  filter;
  private final ClusterInfo               cluster;
  private final String                    nodeName;
  private final Toolkit                   toolkit;
  private final ToolkitInstanceFactory    toolkitInstanceFactory;
  private ItemProcessor<E>                processor;
  private final AsyncClusterListener      listener;
  private final StopCallable              stopCallable;

  public AsyncCoordinatorImpl(String name, AsyncConfig config, ToolkitInstanceFactory toolkitInstanceFactory,
                              StopCallable stopCallable) {
    this.name = name;
    this.toolkitInstanceFactory = toolkitInstanceFactory;
    if (null == config) {
      this.config = config = DefaultAsyncConfig.getInstance();
    } else {
      this.config = config;
    }
    this.toolkit = toolkitInstanceFactory.getToolkit();
    this.cluster = toolkit.getClusterInfo();
    this.listener = new AsyncClusterListener();

    this.nodeName = getAsyncNodeName(name, cluster.getCurrentNode());
    this.localBuckets = new ArrayList<ProcessingBucket<E>>();
    this.deadBuckets = new ArrayList<ProcessingBucket<E>>();
    this.bucketMetaInfoHandler = new BucketMetaInfoHandler<E>(nodeName,
                                                              toolkitInstanceFactory.getOrCreateAsyncListNamesMap(name));
    ToolkitLockType lockType = config.isSynchronousWrite() ? ToolkitLockType.SYNCHRONOUS_WRITE : ToolkitLockType.WRITE;
    this.commonAsyncLock = toolkit.getLock(name, ToolkitLockType.WRITE);
    this.nodeWriteLock = toolkit.getLock(nodeName, lockType);
    this.nodeReadLock = toolkit.getLock(nodeName, ToolkitLockType.READ);
    this.stopCallable = stopCallable;
  }

  @Override
  public void start(ItemProcessor<E> itemProcessor, int processingConcurrency, ItemScatterPolicy<? super E> policy) {
    validateArgs(itemProcessor, processingConcurrency);

    nodeWriteLock.lock();
    try {
      if (status == Status.STARTED) {
        LOGGER.warn("AsyncCoordinator " + name + " already started");
        return;
      }

      this.scatterPolicy = getPolicy(policy, processingConcurrency);
      this.processor = itemProcessor;
      cluster.addClusterListener(listener);

      startBuckets(processingConcurrency);
      status = Status.STARTED;
    } finally {
      nodeWriteLock.unlock();
    }

    processDeadNodes();

  }

  private void validateArgs(ItemProcessor<E> itemProcessor, int processingConcurrency) {
    if (null == itemProcessor) throw new IllegalArgumentException("processor can't be null");
    if (processingConcurrency < 1) throw new IllegalArgumentException("processingConcurrency needs to be at least 1");
  }

  private static <E> ItemScatterPolicy<? super E> getPolicy(ItemScatterPolicy<? super E> policy,
                                                            int processingConcurrency) {
    if (null == policy) {
      if (1 == processingConcurrency) {
        policy = new SingleBucketScatterPolicy();
      } else {
        policy = new HashCodeScatterPolicy();
      }
    }

    return policy;
  }

  private void startBuckets(int processingConcurrency) {
    // add meta info first
    Set<String> nameList = new HashSet();
    for (int i = 0; i < processingConcurrency; i++) {
      String bucketName = nodeName + DELIMITER + BUCKET + DELIMITER + i;
      nameList.add(bucketName);
    }
    bucketMetaInfoHandler.bucketsCreated(nameList);

    // then create the individual list
    for (String bucketName : nameList) {
      ProcessingBucket<E> bucket = createBucket(bucketName, this.config, false);
      localBuckets.add(bucket);
    }

    for (ProcessingBucket<E> bucket : localBuckets) {
      startBucket(bucket, false);
    }
  }

  private ProcessingBucket<E> createBucket(String bucketName, AsyncConfig processingConfig, boolean setDestroyCallback) {
    ToolkitList<E> toolkitList = toolkit.getList(bucketName);
    if (toolkitList.size() > 0) { throw new AssertionError("List created should not have size greater than 0"); }

    final ProcessingBucket<E> bucket = new ProcessingBucket<E>(bucketName, processingConfig, toolkitList, cluster,
                                                               processor, LoggingErrorHandler.getInstance());
    bucket.setItemsFilter(filter);
    if (setDestroyCallback) {
      bucket.setDestroyCallback(removeOnDestroy(deadBuckets, bucket));
    }

    return bucket;
  }

  private void processDeadNodes() {
    // checking if there are any dead nodes and starting threads for those buckets also
    Set<String> deadNodes = bucketMetaInfoHandler.deadNodesWithListsToProcess(name, cluster, toolkitInstanceFactory);
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
    // TODO: make sure this is in sync write txn after atomic toolkit
    nodeWriteLock.lock();
    try {
      status.checkRunning();
      addtoBucket(item);
    } finally {
      nodeWriteLock.unlock();
    }
  }

  private void addtoBucket(E item) {
    final int index = scatterPolicy.selectBucket(localBuckets.size(), item);
    final ProcessingBucket bucket = localBuckets.get(index);
    bucket.add(item);
  }

  @Override
  public void stop() {
    nodeWriteLock.lock();
    try {
      status.checkRunning();
      status = Status.STOPPED;

      stopBuckets(localBuckets);
      stopDeadBuckets();

      cluster.removeClusterListener(listener);

      bucketMetaInfoHandler.clear();
      stopCallable.stop();
    } finally {
      nodeWriteLock.unlock();
    }
  }

  private void stopDeadBuckets() {
    commonAsyncLock.lock();
    try {
      stopBuckets(deadBuckets);
    } finally {
      commonAsyncLock.unlock();
    }
  }

  private void stopBuckets(List<ProcessingBucket<E>> buckets) {
    for (ProcessingBucket<E> bucket : buckets) {
      bucket.stop();
    }
    buckets.clear();
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
    nodeWriteLock.lock();
    try {
      this.filter = filter;
      if (localBuckets != null) {
        for (ProcessingBucket<E> bucket : localBuckets) {
          bucket.setItemsFilter(filter);
        }
      }
    } finally {
      nodeWriteLock.unlock();
    }
  }

  private class AsyncClusterListener implements ClusterListener {
    @Override
    public void onClusterEvent(ClusterEvent event, ClusterInfo clusterInfo) {
      switch (event.getType()) {
        case NODE_LEFT:
          String otherNodeNameListKey = getAsyncNodeName(name, event.getNode());
          processOtherNode(otherNodeNameListKey);
          break;
        default:
          break;
      }
    }

  }

  private void processOtherNode(String deadNode) {
    commonAsyncLock.lock();
    try {
      if (status == Status.STARTED) {
        Collection<String> oldListNames = bucketMetaInfoHandler.transferAllListsFromNode(deadNode);
        AsyncConfig deadNodeConfig = createDeadNodeConfig();
        startProcessingDeadNodeBuckets(deadNodeConfig, oldListNames);
      }
    } finally {
      commonAsyncLock.unlock();
    }
  }

  private AsyncConfig createDeadNodeConfig() {
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
    return deadNodeProcessingConfig;
  }

  private void startProcessingDeadNodeBuckets(AsyncConfig deadNodeProcessingConfig, Collection<String> oldListNames) {
    List<ProcessingBucket<E>> newDeadBuckets = new ArrayList<ProcessingBucket<E>>();
    if (oldListNames != null) {
      for (String bucketName : oldListNames) {
        ProcessingBucket<E> bucket = createBucket(bucketName, deadNodeProcessingConfig, true);
        newDeadBuckets.add(bucket);
        startBucket(bucket, true);
      }
      deadBuckets.addAll(newDeadBuckets);
    }
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
    nodeReadLock.lock();
    try {
      status.checkRunning();
      long size = 0;
      for (ProcessingBucket<E> bucket : localBuckets) {
        size += bucket.getWaitCount();
      }
      return size;
    } finally {
      nodeReadLock.unlock();
    }
  }

  public static interface StopCallable {
    void stop();
  }

  private static String getAsyncNodeName(String name, ClusterNode node) {
    return name + DELIMITER + node.getId();
  }

  private static class BucketMetaInfoHandler<E extends Serializable> {
    /**
     * this ToolkitMap map contains keys based on asyncName-nodeId and value will be linked list of bucketNames (or name
     * of ToolkitList)
     */
    private final ToolkitStore<String, Set<String>> nodeToListNamesMap;
    private final String                            nodeName;

    public BucketMetaInfoHandler(String nodeName, ToolkitStore<String, Set<String>> nodeToListNamesMap) {
      this.nodeName = nodeName;
      this.nodeToListNamesMap = nodeToListNamesMap;
    }

    public void bucketsCreated(Collection<String> bucketNames) {
      Set<String> set = getSetForThisNode();
      set.addAll(bucketNames);
      this.nodeToListNamesMap.put(nodeName, set);
    }

    private Set<String> getSetForThisNode() {
      Set<String> set = this.nodeToListNamesMap.get(nodeName);
      if (set == null) {
        set = addSetForNode();
      }
      return set;
    }

    private Set<String> addSetForNode() {
      Set<String> tmpList = new HashSet<String>();
      Set<String> rvList = this.nodeToListNamesMap.putIfAbsent(nodeName, tmpList);
      return rvList == null ? tmpList : rvList;
    }

    public void clear() {
      this.nodeToListNamesMap.remove(nodeName);
    }

    public Collection<String> transferAllListsFromNode(String node) {
      Set<String> oldNameList = nodeToListNamesMap.get(node);

      if (oldNameList != null) {
        Set<String> newOwner = getSetForThisNode();

        newOwner.addAll(oldNameList);
        nodeToListNamesMap.put(nodeName, newOwner); // transferring bucket ownership to new node
        nodeToListNamesMap.remove(node); // removing buckets from old node
      }
      return oldNameList;
    }

    private Set<String> deadNodesWithListsToProcess(String name, ClusterInfo cluster,
                                                    ToolkitInstanceFactory toolkitInstanceFactory) {
      // check if the all the known nodes still exist in the cluster
      Set<String> deadNodes = new HashSet<String>(nodeToListNamesMap.keySet());
      for (ClusterNode node : cluster.getClusterTopology().getNodes()) {
        deadNodes.remove(getAsyncNodeName(name, node));
      }
      return deadNodes;
    }

  }

}
