/* Copyright (c) 2012-2016 Jesper Öqvist <jesper@llbit.se>
 *
 * This file is part of Chunky.
 *
 * Chunky is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Chunky is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Chunky.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.llbit.chunky.world;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import se.llbit.log.Log;

/**
 * Abstract region representation. Tracks loaded chunks and their timestamps.
 *
 * <p>If an error occurs it will usually be reported to STDERR instead of using
 * the logging framework, because the error dialogs can be so many for a
 * single corrupted region. Corrupted chunks are illustrated by a black square
 * with a red X and red outline in the map view.
 *
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public abstract class Region implements Iterable<Chunk> {

  /**
   * Region X chunk width
   */
  public static final int CHUNKS_X = 32;

  /**
   * Region Z chunk width
   */
  public static final int CHUNKS_Z = 32;

  protected static final int NUM_CHUNKS = CHUNKS_X * CHUNKS_Z;

  protected final ChunkPosition position;
  protected final World world;
  protected long regionFileTime = 0;
  protected final int[] chunkTimestamps = new int[NUM_CHUNKS];

  /**
   * Create new region
   *
   * @param pos   the region position
   */
  public Region(ChunkPosition pos, World world) {
    this.world = world;
    position = pos;
  }

  public abstract Chunk getChunk(ChunkPosition pos);
  public abstract void setChunk(ChunkPosition pos, Chunk chunk);

  /**
   * Delete a chunk.
   */
  public synchronized void deleteChunk(ChunkPosition chunkPos) {
    deleteChunkFromRegion(chunkPos);
    Chunk chunk = getChunk(chunkPos);
    if (!chunk.isEmpty()) {
      chunk.reset();
      setChunk(chunkPos, EmptyChunk.INSTANCE);
      world.chunkDeleted(chunkPos);
    }
  }

  /**
   * Parse the region file to discover chunks.
   */
  public abstract void parse();

  /**
   * @return <code>true</code> if this is an empty or non-existent region
   */
  public boolean isEmpty() {
    return false;
  }

  /**
   * @return The region position
   */
  public final ChunkPosition getPosition() {
    return position;
  }

  @Override public String toString() {
    return "Region " + position.toString();
  }

  /**
   * @param pos A region position
   * @return The region file name corresponding to the given region position
   */
  public static String getFileName(ChunkPosition pos) {
    return String.format("r.%d.%d.mca", pos.x, pos.z);
  }

  /**
   * Opens an input stream for the given chunk.
   *
   * @param chunkPos chunk position for the chunk to read
   * @return Chunk data source. The InputStream of the data source is
   * {@code null} if the chunk could not be read.
   */
  public abstract ChunkDataSource getChunkData(ChunkPosition chunkPos);


  /**
   * Delete the chunk from the region file.
   */
  public abstract void deleteChunkFromRegion(ChunkPosition chunkPos);

  public abstract boolean hasChanged();

  /**
   * @return {@code true} if the chunk has changed since the timestamp
   */
  public boolean chunkChangedSince(ChunkPosition chunkPos, int timestamp) {
    return timestamp != chunkTimestamps[(chunkPos.x & 31) + (chunkPos.z & 31) * 32];
  }

  @Override public  abstract Iterator<Chunk> iterator();
}
