package se.llbit.chunky.world.minecraft1_13;

import se.llbit.chunky.world.ChunkPosition;
import se.llbit.chunky.world.PreLoadingJob;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RegionManager {
  /**
   * Those 3 fields must be protected by the lock
   */
  private final Map<ChunkPosition, Region> regionPreloaded = new HashMap<>();
  private final Set<ChunkPosition> regionsPreloading = new HashSet<>();
  private final Queue<ChunkPosition> regionToPreload = new ArrayDeque<>();

  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  /**
   * This queue does not need to be protected by the lock
   */
  private final BlockingQueue<Region> regionToPreLoadChunks = new LinkedBlockingQueue<>();

  public Region getRegion(ChunkPosition pos) {
    Region region = null;
    lock.readLock().lock();
    try {
      region = regionPreloaded.getOrDefault(pos, EmptyRegion.instance);
    } finally {
      lock.readLock().unlock();
    }
    return region;
  }

  public void cancelPreload() {
    // Cancel the preloading of region that are not already being preloaded
    lock.writeLock().lock();
    try {
      regionToPreload.clear();
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void preloadRegion(ChunkPosition pos) {
    // Add pos to regionToPreload if doesn't already exist either in
    // regionPreloaded or regionPreloading
    boolean needToAdd = true;
    lock.readLock().lock();
    try {
      needToAdd = !regionToPreload.contains(pos);
      if(needToAdd)
        needToAdd = !regionsPreloading.contains(pos);
    } finally {
      lock.readLock().unlock();
    }

    if(!needToAdd)
      return;

    lock.writeLock().lock();
    try {
      regionToPreload.add(pos);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public ChunkPosition getNextRegionToLoad() {
    boolean needToLoadRegion = true;
    ChunkPosition regionPos = null;
    lock.writeLock().lock();
    try {
      needToLoadRegion = !regionToPreload.isEmpty();
      if(needToLoadRegion) {
        regionPos = regionToPreload.poll();
        regionsPreloading.add(regionPos);
      }
    } finally {
      lock.writeLock().unlock();
    }
    return regionPos;
  }

  public void regionLoaded(ChunkPosition pos, Region region) {
    lock.writeLock().lock();
    try {
      regionsPreloading.remove(pos);
      regionPreloaded.put(pos, region);
    } finally {
      lock.writeLock().unlock();
    }
    try {
      regionToPreLoadChunks.put(region);
    } catch(InterruptedException ignored) {
    }
  }

  /**
   * Get the next region for which we want to preload the chunks
   */
  public Region getNextRegionToPreloadChunks() {
    try {
      return regionToPreLoadChunks.poll(200, TimeUnit.MILLISECONDS);
    } catch(InterruptedException ignored) {
      return null;
    }
  }
}
