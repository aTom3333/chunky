package se.llbit.chunky.world.minecraft1_13;

import se.llbit.chunky.world.ChunkPosition;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class LoadedRegion implements Region {
  private static final int CHUNK_X = 32;
  private static final int CHUNK_Z = 32;
  private static final int NUM_CHUNKS = CHUNK_X * CHUNK_Z;

  private final World world;
  private final ChunkPosition pos;
  private final Chunk[] chunks = new Chunk[NUM_CHUNKS];

  private final RandomAccessFile regionFile;

  public LoadedRegion(World world, ChunkPosition pos) throws IOException {
    this.world = world;
    this.pos = pos;
    regionFile = new RandomAccessFile(regionFileFromPosition(world, pos), "r");
    for(int i = 0; i < NUM_CHUNKS; ++i) {
      chunks[i] = EmptyChunk.instance;
    }
  }

  public static File regionFileFromPosition(World world, ChunkPosition pos) {
    return new File(world.getRegionDirectory(), String.format("r.%d.%d.mca", pos.x, pos.z));
  }

  // Should this be public?
  public void load() throws IOException {
    synchronized(regionFile) {
      regionFile.seek(0);
      for(int chunkZ = 0; chunkZ < CHUNK_Z; ++chunkZ) {
        for(int chunkX = 0; chunkX < CHUNK_X; ++chunkX) {
          ChunkPosition position = ChunkPosition.get((pos.x << 5) + chunkX, (pos.z << 5) + chunkZ);
          Chunk chunk = getChunk(position);
          int loc = regionFile.readInt();
          if (loc != 0) {
            if (chunk.isEmpty()) {
              // TODO Create Chunk
              //setChunk(pos, chunk);
            }
          } else {
            if (!chunk.isEmpty()) {
              // TODO Notify that the chunk has been deleted
              setChunk(position, EmptyChunk.instance);
            }
          }
        }
      }
    }
  }

  /**
   * @return Chunk at (x, z)
   */
  public Chunk getChunk(int x, int z) {
    return chunks[(x & 31) + (z & 31) * 32];
  }

  /**
   * @param pos Chunk position
   * @return Chunk at given position
   */
  public Chunk getChunk(ChunkPosition pos) {
    return chunks[(pos.x & 31) + (pos.z & 31) * 32];
  }

  /**
   * Set chunk at given position.
   */
  public void setChunk(ChunkPosition pos, Chunk chunk) {
    chunks[(pos.x & 31) + (pos.z & 31) * 32] = chunk;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }
}
