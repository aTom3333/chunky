package se.llbit.chunky.world.minecraft1_13;

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.world.*;
import se.llbit.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * this class implements the world interface to deal
 * with vanilla minecraft world version 1.13 and higher, aka the Anvil format
 */
public class World implements WorldInterface {

  private File worldDirectory;

  private int dimension = 0; // TODO Make this not hardcoded

  /**
   * This world uses Region and Chunk to split the world in smaller bits
   */
  private RegionManager regionManager = new RegionManager();

  private final Set<PlayerEntityData> playersData = new HashSet<>();

  private Selection selection;

  private WorldView currentView;

  public World(File worldDirectory) {
    this.worldDirectory = worldDirectory;
    // TODO Make an empty selection
  }

  @Override
  public PreLoadingJob nextPreLoadingJob() {
    return () -> {
      ChunkPosition regionToLoad = regionManager.getNextRegionToLoad();
      if(regionToLoad == null) {
        // No more region to load, load chunks
        Region regionToPreLoadChunks = regionManager.getNextRegionToPreloadChunks();
        if(regionToPreLoadChunks == null)
          return;
        if(regionToPreLoadChunks.isEmpty())
          return;
        ((LoadedRegion)regionToPreLoadChunks).preLoadChunks(currentView); // Taking the view here might not be thread safe
      } else {
        File regionFile = LoadedRegion.regionFileFromPosition(this, regionToLoad);
        if(!regionFile.exists()) {
          regionManager.regionLoaded(regionToLoad, EmptyRegion.instance);
        } else {
          try {
            LoadedRegion region = new LoadedRegion(this, regionToLoad);
            region.load();
            regionManager.regionLoaded(regionToLoad, region);
          } catch(IOException e) {
            Log.warn("Can't load region");
          }
        }
      }
    };
  }

  @Override
  public void viewChanged(WorldView newView) {
    currentView = newView;
    updateRegionToPreload();
  }

  @Override
  public void setSelection(Selection selection) {
    this.selection = selection;
  }

  @Override
  public void loadSelection(Scene scene) {

  }

  @Override
  public MapRenderingJob nextMapRenderingJob() {
    return null;
  }


  private void updateRegionToPreload() {
    regionManager.cancelPreload();

    int minRegionX = (int)Math.floor((float)currentView.minX / (16*32));
    int maxRegionX = (int)Math.ceil((float)currentView.maxX / (16*32));
    int minRegionZ = (int)Math.floor((float)currentView.minZ / (16*32));
    int maxRegionZ = (int)Math.ceil((float)currentView.maxZ / (16*32));

    for(int regionX = minRegionX; regionX <= maxRegionX; ++regionX) {
      for(int regionZ = minRegionZ; regionZ <= maxRegionZ; ++regionZ) {
        regionManager.preloadRegion(ChunkPosition.get(regionX, regionZ));
      }
    }
  }

  public File getRegionDirectory() {
    if(dimension == 0) {
      return new File(worldDirectory, "region");
    }
    return new File(worldDirectory, String.format("DIM%d/region", dimension));
  }
}
