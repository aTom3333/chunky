package se.llbit.chunky.world.minecraft1_13;

import se.llbit.chunky.world.ChunkPosition;
import se.llbit.chunky.world.WorldView;

import java.io.*;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class LoadedRegion implements Region {
  private static final int CHUNK_X = 32;
  private static final int CHUNK_Z = 32;
  private static final int NUM_CHUNKS = CHUNK_X * CHUNK_Z;
  private static final int SECTOR_SIZE = 4096;

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
              chunk = new ExistingChunk(position, this);
              setChunk(position, chunk);
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

  public void preLoadChunks(WorldView view) {
    int minChunkX = (int)Math.floor((float)view.minX / 16);
    int maxChunkX = (int)Math.ceil((float)view.maxX / 16);
    int minChunkZ = (int)Math.floor((float)view.minZ / 16);
    int maxChunkZ = (int)Math.ceil((float)view.maxZ / 16);

    for(int chunkZ = 0; chunkZ < CHUNK_Z; ++chunkZ) {
      for(int chunkX = 0; chunkX < CHUNK_X; ++chunkX) {
        ChunkPosition position = ChunkPosition.get((pos.x << 5) + chunkX, (pos.z << 5) + chunkZ);
        if(position.x < minChunkX || position.x > maxChunkX || position.z < minChunkZ || position.z > maxChunkZ)
          continue;

        Chunk chunk = getChunk(position);
        if(chunk.isEmpty())
          continue;

        ((ExistingChunk)chunk).preLoad();
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

  @Override
  public Iterator<Chunk> iterator() {
    return new Iterator<Chunk>() {
      int index = 0;

      @Override
      public boolean hasNext() {
        return index < NUM_CHUNKS;
      }

      @Override
      public Chunk next() {
        Chunk chunk = chunks[index];
        ++index;
        return chunk;
      }
    };
  }

  public World getWorld() {
    return world;
  }

  public InputStream getChunkInputStream(ChunkPosition pos) throws IOException {
    int index = (pos.x & 31) * 32 + (pos.z & 31);
    synchronized(regionFile) {
      regionFile.seek(index * 4);
      int location = regionFile.readInt();

      int numSectors = location & 0xFF;
      int sectorOffset = location >> 8;
      regionFile.seek(sectorOffset * SECTOR_SIZE);
      int chunkSize = regionFile.readInt();
      if (chunkSize > numSectors * SECTOR_SIZE) {
        System.err.println("Error: chunk length does not fit in allocated sectors!");
        return null;
      }

      byte type = regionFile.readByte();
      if (type != 1 && type != 2) {
        System.err.println("Error: unknown chunk data compression method: " + type + "!");
        return null;
      }
      byte[] buf = new byte[chunkSize - 1];
      regionFile.read(buf);
      ByteArrayInputStream in = new ByteArrayInputStream(buf);
      if (type == 1) {
        return new GZIPInputStream(in);
      } else {
        return new InflaterInputStream(in);
      }
    }
  }
}
