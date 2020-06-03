package se.llbit.chunky.world.cubicchunks;

import se.llbit.chunky.world.ChunkPosition;
import se.llbit.chunky.world.PlayerEntityData;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipOutputStream;

public class World extends se.llbit.chunky.world.World {
  public World(String levelName, File worldDirectory, int dimension,
               Set<PlayerEntityData> playerEntities, boolean haveSpawnPos, long seed, long timestamp) {
    super(levelName, worldDirectory, dimension, playerEntities, haveSpawnPos, seed, timestamp);
  }

  @Override
  protected void appendRegionToZip(ZipOutputStream zout, File regionDirectory, ChunkPosition regionPos, String regionZipFileName, Set<ChunkPosition> chunks) throws IOException {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Region createRegion(ChunkPosition pos) {
    return new Region(pos, this);
  }


  public File getRegion2dDirectory() {
    return new File(worldDirectory, "region2d");
  }

  public File getRegion3dDirectory() {
    return new File(worldDirectory, "region3d");
  }

  @Override
  public boolean regionExists(ChunkPosition pos) {
    File regiond2dFile = new File(getRegion2dDirectory(), Region.getRegion2dFile(pos));
    File regiond3dFile = new File(getRegion3dDirectory(), Region.getRegion3dFile(pos));
    return regiond2dFile.exists() && regiond3dFile.exists();
  }
}
