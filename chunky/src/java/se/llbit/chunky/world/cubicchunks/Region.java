package se.llbit.chunky.world.cubicchunks;

import se.llbit.chunky.world.Chunk;
import se.llbit.chunky.world.ChunkDataSource;
import se.llbit.chunky.world.ChunkPosition;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.Tag;

import java.io.*;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class Region extends se.llbit.chunky.world.Region {
  protected final String region2dFileName;
  protected final String region3dFileName;

  public Region(ChunkPosition pos, World world) {
    super(pos, world);
    region2dFileName = getRegion2dFile(pos);
    region3dFileName = getRegion3dFile(pos);
  }

  public static String getRegion2dFile(ChunkPosition pos) {
    return String.format("%d.%d.2dr", pos.x, pos.z);
  }

  public static String getRegion3dFile(ChunkPosition pos) {
    return String.format("%d.%d.%d.3dr", pos.x, pos.y, pos.z);
  }

  private World getWorld() {
    return (World)world;
  }

  private static int SECTOR_SIZE = 512;
  private static int HEADER_SIZE = 4096;

  @Override
  public synchronized void parse() {
    File region2dFile = new File(getWorld().getRegion2dDirectory(), region2dFileName);
    File region3dFile = new File(getWorld().getRegion3dDirectory(), region3dFileName);
    if(!region2dFile.isFile() || !region3dFile.isFile())
      return;
    long modtime = Math.max(region2dFile.lastModified(), region3dFile.lastModified());
    if (regionFileTime == modtime) {
      return;
    }
    regionFileTime = modtime;
    try (RandomAccessFile file = new RandomAccessFile(region2dFile, "r")) {
      long length = file.length();
      if (length < HEADER_SIZE) {
        System.err.println("Missing header in region file!");
        return;
      }
      // TODO Do that elsewhere
      int[] locations = new int[32*32];
      for(int x = 0; x < 32; ++x) {
        for(int z = 0; z < 32; ++z) {
          locations[32*x+z] = file.readInt();
        }
      }
      for(int x = 0; x < 32; ++x) {
        for(int z = 0; z < 32; ++z) {
          if(locations[x*32+z] == 0)
            continue;
          System.out.printf("Column at %d,%d\n", x, z);
          long offset = (long)(locations[x*32+z] >> 8) * SECTOR_SIZE;
          long size = (long)(locations[x*32+z] & 0xFF) * SECTOR_SIZE;
          file.seek(offset);
          int lengthByte = file.readInt();
          byte[] buf = new byte[lengthByte];
          file.read(buf);
          InputStream in = new ByteArrayInputStream(buf);
            in = new GZIPInputStream(in);
          Tag parsedData = NamedTag.read(new DataInputStream(in));
          System.out.println(parsedData.dumpTree());
        }
      }

    } catch (IOException e) {
      System.err.println("Failed to read region: " + e.getMessage());
    }
  }

  protected void getColumnData(ChunkPosition pos) {

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

  @Override
  public Chunk getChunk(ChunkPosition pos) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public void setChunk(ChunkPosition pos, Chunk chunk) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  public Iterator<Chunk> iterator() {
    throw new RuntimeException("Not implemented");
  }
}
