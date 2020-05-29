package se.llbit.chunky.world.cubicchunks;

import se.llbit.chunky.world.Chunk;
import se.llbit.chunky.world.ChunkDataSource;
import se.llbit.chunky.world.ChunkPosition;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Region extends se.llbit.chunky.world.Region {
  protected final String region2dFile;
  protected final String region3dFile;

  public Region(ChunkPosition pos, World world) {
    super(pos, world);
    region2dFile = String.format("");
    region3dFile = String.format("");
  }

  @Override
  public synchronized void parse() {
      throw new RuntimeException("Not implemented");
  }

  @Override
  public ChunkDataSource getChunkData(ChunkPosition chunkPos) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void deleteChunkFromRegion(ChunkPosition chunkPos) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public boolean hasChanged() {
    throw new RuntimeException("Not implemented");
  }
}
