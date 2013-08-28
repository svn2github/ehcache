/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactoryImpl;
import org.terracotta.modules.ehcache.async.scatterpolicies.HashCodeScatterPolicy;
import org.terracotta.modules.ehcache.async.scatterpolicies.ItemScatterPolicy;
import org.terracotta.modules.ehcache.async.scatterpolicies.SingleBucketScatterPolicy;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.collections.ToolkitListInternal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * An AsyncCoordinator allows work to be added and processed asynchronously in a fault-tolerant and high performance
 * fashion.
 */

public class AsyncCoordinatorImpl<E extends Serializable> implements AsyncCoordinator<E> {
  private static final String             DEAD_NODES = "DEAD_NODES";
  private static final Logger             LOGGER     = LoggerFactory.getLogger(AsyncCoordinatorImpl.class.getName());
  private static final String             DELIMITER  = ToolkitInstanceFactoryImpl.DELIMITER;
  private static final String             NODE_ALIVE_TIMEOUT_PROPERTY_NAME = "ehcache.async.node.alive.timeout";
  private static final String             ALIVE_LOCK_SUFFIX                = "-alive-lock";
  /**
   * lock for this coordinator based on SynchronousWrite
   */
  private final ToolkitLock               commonAsyncLock;
  private final Lock                      nodeWriteLock;
  private final Lock                      nodeReadLock;
  private volatile Status                 status     = Status.UNINITIALIZED;
  private final long                      aliveTimeoutSec;
  private final List<ProcessingBucket<E>> localBuckets;
  private final List<ProcessingBucket<E>> deadBuckets;
  private final String                    name;
  private final AsyncConfig               config;
  private ItemScatterPolicy<? super E>    scatterPolicy;
  private ItemsFilter<E>                  filter;
  private final ClusterInfo               cluster;
  private volatile String                 nodeName;
  private final ToolkitInternal           toolkit;
  private ItemProcessor<E>                processor;
  private final AsyncClusterListener      listener;
  private final Callback                  asyncFactoryCallback;
  private final BucketManager             bucketManager;
  private volatile ClusterNode            currentNode;
  private volatile int                    concurrency = 1;
  private final LockHolder                lockHolder;

  public AsyncCoordinatorImpl(String fullAsyncName, AsyncConfig config, ToolkitInstanceFactory toolkitInstanceFactory,
                              Callback asyncFactoryCallback) {
    this.name = fullAsyncName; // contains CacheManager name and Cache name
    if (null == config) {
      this.config = config = DefaultAsyncConfig.getInstance();
    } else {
      this.config = config;
    }
    this.toolkit = (ToolkitInternal) toolkitInstanceFactory.getToolkit();
    this.aliveTimeoutSec = toolkit.getProperties().getLong(NODE_ALIVE_TIMEOUT_PROPERTY_NAME, 5L);
    this.cluster = toolkit.getClusterInfo();
    this.listener = new AsyncClusterListener();
    this.currentNode = cluster.getCurrentNode();
    this.nodeName = getAsyncNodeName(name, currentNode); // contains CacheManager name, Cache name and nodeId
    this.localBuckets = new ArrayList<ProcessingBucket<E>>();
    this.deadBuckets = new ArrayList<ProcessingBucket<E>>();
    this.bucketManager = new BucketManager(toolkitInstanceFactory);
    this.commonAsyncLock = toolkit.getLock(name);
    ReadWriteLock nodeLock = new ReentrantReadWriteLock();
    this.nodeWriteLock = nodeLock.writeLock();
    this.nodeReadLock = nodeLock.readLock();
    this.asyncFactoryCallback = asyncFactoryCallback;
    this.lockHolder = new LockHolder();
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

      if (status != Status.UNINITIALIZED) { throw new IllegalStateException(); }
      this.concurrency = processingConcurrency;
      this.scatterPolicy = getPolicy(policy, concurrency);
      this.processor = itemProcessor;
      cluster.addClusterListener(listener);
      startBuckets(concurrency);
      status = Status.STARTED;
    } finally {
      nodeWriteLock.unlock();
    }
    processDeadNodes();
  }

  private void processDeadNodes() {
    bucketManager.scanAndAddDeadNodes();
    processOneDeadNodeIfNecessary();
  }

  private void validateArgs(ItemProcessor<E> itemProcessor, int processingConcurrency) {
    if (null == itemProcessor) throw new IllegalArgumentException("processor can't be null");
    if (processingConcurrency < 1) throw new IllegalArgumentException("processingConcurrency needs to be at least 1");
  }

  private static <F extends Serializable> ItemScatterPolicy<? super F> getPolicy(ItemScatterPolicy<? super F> policy,
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

  private long startDeadBuckets(Set<String> oldListNames) {
    long totalItems = 0;
    for (String bucketName : oldListNames) {
      ProcessingBucket<E> bucket = createBucket(bucketName, this.config, true);
      deadBuckets.add(bucket);
      totalItems += bucket.getWaitCount();
      bucket.start();
    }
    return totalItems;
  }

  private String getAliveLockName(String node) {
    return node + ALIVE_LOCK_SUFFIX;
  }


  private boolean tryLockNodeAlive(String otherNodeName) {
    try {
      return toolkit.getLock(getAliveLockName(otherNodeName)).tryLock(aliveTimeoutSec, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private void startBuckets(int processingConcurrency) {
    lockHolder.hold(toolkit.getLock(getAliveLockName(nodeName)));

    Set<String> nameList = new HashSet();
    for (int i = 0; i < processingConcurrency; i++) {
      String bucketName = nodeName + DELIMITER + i;
      nameList.add(bucketName);
    }
    bucketManager.bucketsCreated(nameList);

    // then create the individual list
    for (String bucketName : nameList) {
      ProcessingBucket<E> bucket = createBucket(bucketName, this.config, false);
      localBuckets.add(bucket);
      bucket.start();
    }
  }

  private ProcessingBucket<E> createBucket(String bucketName, AsyncConfig processingConfig, boolean workingOnDeadBucket) {
    ToolkitListInternal<E> toolkitList = (ToolkitListInternal) toolkit.getList(bucketName, null);
    if (!workingOnDeadBucket && toolkitList.size() > 0) { throw new AssertionError(
                                                                                   "List created should not have size greater than 0"); }

    final ProcessingBucket<E> bucket = new ProcessingBucket<E>(bucketName, processingConfig, toolkitList, cluster,
                                                               processor, workingOnDeadBucket);
    bucket.setItemsFilter(filter);
    if (workingOnDeadBucket) {
      bucket.setCleanupCallback(cleanupDeadBucket(deadBuckets, bucket));
    }
    return bucket;
  }

  private Callback cleanupDeadBucket(final List<ProcessingBucket<E>> list, final ProcessingBucket<E> bucket) {
    return new Callback() {
      @Override
      public void callback() {
        nodeWriteLock.lock();
        try {
          bucket.destroy();
          list.remove(bucket);
          bucketManager.removeBucket(bucket.getBucketName());
        } finally {
          nodeWriteLock.unlock();
        }
        processOneDeadNodeIfNecessary();
      }
    };
  }

  // we do not take any clustered lock in this method. make sure this is always called from within a clustered lock.
  @Override
  public void add(E item) {
    if (null == item) { return; }
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
      stopBuckets(deadBuckets);

      cluster.removeClusterListener(listener);
      bucketManager.clear();
      asyncFactoryCallback.callback();
      lockHolder.release(toolkit.getLock(getAliveLockName(nodeName)));
    } finally {
      nodeWriteLock.unlock();
    }
  }

  private void stopBuckets(List<ProcessingBucket<E>> buckets) {
    for (ProcessingBucket<E> bucket : buckets) {
      bucket.stop();
    }
    buckets.clear();
  }

  private void stopNow() {
    debug("stopNow localBuckets " + localBuckets.size() + " | deadBuckets " + deadBuckets.size());
    nodeWriteLock.lock();
    try {
      stopBucketsNow(localBuckets);
      stopBucketsNow(deadBuckets);
    } finally {
      nodeWriteLock.unlock();
    }
  }

  private void nodeRejoined() {
    nodeWriteLock.lock();
    try {
      currentNode = cluster.getCurrentNode();
      nodeName = getAsyncNodeName(name, currentNode);
      debug("nodeRejoined currentNode " + currentNode + " nodeName " + nodeName);
      localBuckets.clear();
      deadBuckets.clear();
      lockHolder.reset();
      startBuckets(concurrency);
    } finally {
      nodeWriteLock.unlock();
    }
    processDeadNodes();
  }

  private void stopBucketsNow(List<ProcessingBucket<E>> buckets) {
    for (ProcessingBucket<E> bucket : buckets) {
      bucket.stopNow();
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
    nodeWriteLock.lock();
    try {
      this.filter = filter;
      for (ProcessingBucket<E> bucket : localBuckets) {
        bucket.setItemsFilter(filter);
      }
    } finally {
      nodeWriteLock.unlock();
    }
  }

  private class AsyncClusterListener implements ClusterListener {
    @Override
    public void onClusterEvent(ClusterEvent event) {
      debug("onClusterEvent " + event.getType() + " for " + event.getNode().getId() + " received at "
            + currentNode.getId());
      switch (event.getType()) {
        case NODE_LEFT:
          if (!event.getNode().equals(currentNode)) { // other node's NODE_LEFT received
            String leftNodeKey = getAsyncNodeName(name, event.getNode());
            commonAsyncLock.lock();
            try {
              bucketManager.addToDeadNodes(Collections.singleton(leftNodeKey));
            } finally {
              commonAsyncLock.unlock();
            }
            processOneDeadNodeIfNecessary();
          } else { // self NODE_LEFT received
            stopNow();
          }
          break;
        case NODE_REJOINED:
          nodeRejoined();
          break;
        default:
          break;
      }
    }
  }

  private void processOneDeadNodeIfNecessary() {
    nodeWriteLock.lock();
    try {
      if (status == Status.STARTED && deadBuckets.isEmpty()) {
        processOneDeadNode();
      } else {
        debug("skipped processOneDeadNode status " + status + " deadBuckets " + deadBuckets.size());
      }
    } finally {
      nodeWriteLock.unlock();
    }
  }

  private void processOneDeadNode() {
    Set<String> deadNodeBuckets = Collections.EMPTY_SET;
    commonAsyncLock.lock();
    try {
      deadNodeBuckets = bucketManager.transferBucketsFromDeadNode();
    } finally {
      commonAsyncLock.unlock();
    }
    if (!deadNodeBuckets.isEmpty()) {
      long totalItems = startDeadBuckets(deadNodeBuckets);
      debug("processOneDeadNode deadNodeBuckets " + deadNodeBuckets.size() + " totalItems " + totalItems + " at "
            + nodeName);
    }
  }

  private static enum Status {
    UNINITIALIZED, STARTED, STOPPED {
      @Override
      final void checkRunning() {
        throw new IllegalStateException("AsyncCoordinator is " + this.name().toLowerCase() + "!");
      }
    };

    void checkRunning() {
      // All good
    }
  }

  private void debug(String message) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(message);
    }
  }

  @Override
  public long getQueueSize() {
    long size = 0;
    nodeReadLock.lock();
    try {
      status.checkRunning();
      for (ProcessingBucket<E> bucket : localBuckets) {
        size += bucket.getWaitCount();
      }
      for (ProcessingBucket<E> bucket : deadBuckets) {
        size += bucket.getWaitCount();
      }
      return size;
    } finally {
      nodeReadLock.unlock();
    }
  }

  @Override
  public void destroy() {
    commonAsyncLock.lock();
    try {
      for (String bucketName : bucketManager.getAllBuckets()) {
        toolkit.getList(bucketName, null).destroy();
      }
      bucketManager.destroy();
    } finally {
      commonAsyncLock.unlock();
    }
  }

  public static interface Callback {
    void callback();
  }

  private static String getAsyncNodeName(String name, ClusterNode node) {
    String nodeId = node.getId();
    if (nodeId == null || nodeId.isEmpty()) { throw new AssertionError("nodeId cannot be " + nodeId); }

    return name + DELIMITER + node.getId();
  }



  private class BucketManager {
    /**
     * this ToolkitMap map contains keys based on asyncName|nodeId and value will a Set of bucketNames (or name of
     * ToolkitList)
     */
    private final ToolkitMap<String, Set<String>> nodeToBucketNames;

    public BucketManager(ToolkitInstanceFactory toolkitFactory) {
      this.nodeToBucketNames = toolkitFactory.getOrCreateAsyncListNamesMap(name);
      nodeToBucketNames.putIfAbsent(DEAD_NODES, new HashSet<String>());
    }

    private void bucketsCreated(Set<String> bucketNames) {
      Set<String> prev = nodeToBucketNames.put(nodeName, bucketNames);
      if (prev != null) { throw new AssertionError("previous value " + prev + " not null for " + nodeName); }
    }

    private void clear() {
      nodeToBucketNames.remove(nodeName);
    }

    private void removeBucket(String bucketName) {
      commonAsyncLock.lock();
      try {
        Set<String> buckets = nodeToBucketNames.get(nodeName);
        boolean removed = buckets.remove(bucketName);
        nodeToBucketNames.put(nodeName, buckets);
        debug("removeBucket " + bucketName + " " + removed + " remaining deadNodes "
              + nodeToBucketNames.get(DEAD_NODES));
      } finally {
        commonAsyncLock.unlock();
      }
    }

    private Set<String> transferBucketsFromDeadNode() {
      String deadNode = getOneDeadNode();
      while (deadNode != null) {
        Set<String> deadNodeBuckets = nodeToBucketNames.get(deadNode);
        if (deadNodeBuckets != null) {
          Set<String> newOwner = nodeToBucketNames.get(nodeName);
          newOwner.addAll(deadNodeBuckets);
          nodeToBucketNames.put(nodeName, newOwner); // transferring bucket ownership to new node
          nodeToBucketNames.remove(deadNode); // removing buckets from old node
          debug("transferBucketsFromDeadNode deadNode " + deadNode + " to node " + nodeName + " buckets " + newOwner
                + " remaining deadNodes " + nodeToBucketNames.get(DEAD_NODES));
          return deadNodeBuckets;
        }
        deadNode = getOneDeadNode();
      }
      return Collections.EMPTY_SET;
    }

    private String getOneDeadNode() {
      String deadNode = null;
      Set<String> deadNodes = nodeToBucketNames.get(DEAD_NODES);
      Iterator<String> itr = deadNodes.iterator();
      if (itr.hasNext()) {
        deadNode = itr.next();
        itr.remove();
        nodeToBucketNames.put(DEAD_NODES, deadNodes);
      }
      return deadNode;
    }

    private Set<String> getAllNodes() {
      Set<String> nodes = new HashSet<String>(nodeToBucketNames.keySet());
      nodes.remove(DEAD_NODES);
      return nodes;
    }

    private void addToDeadNodes(Collection<String> nodes) {
      if (!nodes.isEmpty()) {
        Set<String> allDeadNodes = nodeToBucketNames.get(DEAD_NODES);
        if (allDeadNodes.addAll(nodes)) {
          nodeToBucketNames.put(DEAD_NODES, allDeadNodes);
          debug(nodeName + " addToDeadNodes deadNodes " + nodes + " allDeadNodes " + allDeadNodes);
        }
      }
    }

    /**
     * checks if there are any dead nodes and add them into DEAD_NODES set
     */
    private void scanAndAddDeadNodes() {
      commonAsyncLock.lock();
      try {
        // check if the all the known nodes still exist in the cluster
        Set<String> nodesFromMap = getAllNodes();
        Set<String> clusterNodes = getClusterNodes();
        nodesFromMap.removeAll(clusterNodes);
        Iterator<String> itr = nodesFromMap.iterator();
        while (itr.hasNext()) {
          String deadNode = itr.next();
          if (!tryLockNodeAlive(deadNode)) {
            // if not able to grab the lock, means its not a deadNode so remove
            itr.remove();
          }
        }
        addToDeadNodes(nodesFromMap);
      } finally {
        commonAsyncLock.unlock();
      }
    }

    private Set<String> getClusterNodes() {
      Set<String> nodes = new HashSet<String>();
      for (ClusterNode node : cluster.getNodes()) {
        nodes.add(getAsyncNodeName(name, node));
      }
      return nodes;
    }

    private Set<String> getAllBuckets() {
      Set<String> buckets = new HashSet<String>();
      for (String node : getAllNodes()) {
        buckets.addAll(nodeToBucketNames.get(node));
      }
      return buckets;
    }

    void destroy() {
      nodeToBucketNames.destroy();
    }
  }

}
