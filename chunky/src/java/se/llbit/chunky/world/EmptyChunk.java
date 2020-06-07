/* Copyright (c) 2010-2014 Jesper Öqvist <jesper@llbit.se>
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

import se.llbit.chunky.block.Air;
import se.llbit.chunky.block.Block;
import se.llbit.chunky.block.Lava;
import se.llbit.chunky.block.Water;
import se.llbit.chunky.chunk.BlockPalette;
import se.llbit.chunky.entity.ArmorStand;
import se.llbit.chunky.entity.Entity;
import se.llbit.chunky.entity.PaintingEntity;
import se.llbit.chunky.map.IconLayer;
import se.llbit.chunky.map.MapTile;
import se.llbit.math.Octree;
import se.llbit.math.Vector3;
import se.llbit.math.Vector3i;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.Tag;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Empty or non-existent chunk.
 *
 * @author Jesper Öqvist <jesper@llbit.se>
 */
public class EmptyChunk extends Chunk {

  /**
   * Singleton instance
   */
  public static final EmptyChunk INSTANCE = new EmptyChunk();

  private static final int COLOR = 0xFFFFFFFF;

  @Override public boolean isEmpty() {
    return true;
  }

  private EmptyChunk() {
    super(ChunkPosition.get(0, 0), EmptyWorld.INSTANCE);
    surface = IconLayer.CORRUPT;
  }

  @Override public synchronized void getBlockData(int[] blocks, byte[] biomes,
      Collection<CompoundTag> tileEntities, Collection<CompoundTag> entities,
      BlockPalette palette) {
    for (int i = 0; i < X_MAX * Y_MAX * Z_MAX; ++i) {
      blocks[i] = 0;
    }

    for (int i = 0; i < X_MAX * Z_MAX; ++i) {
      biomes[i] = 0;
    }
  }

  @Override public void renderSurface(MapTile tile) {
    renderEmpty(tile);
  }

  @Override public void renderBiomes(MapTile tile) {
    renderEmpty(tile);
  }

  @Override public int biomeColor() {
    return 0xFFEEEEEE;
  }

  private void renderEmpty(MapTile tile) {
    int[] pixels = new int[tile.tileWidth * tile.tileWidth];
    for (int z = 0; z < tile.tileWidth; ++z) {
      for (int x = 0; x < tile.tileWidth; ++x) {
        if (x == z || x - tile.tileWidth / 2 == z || x + tile.tileWidth / 2 == z) {
          pixels[z * tile.tileWidth + x] = 0xFF000000;
        } else {
          pixels[z * tile.tileWidth + x] = COLOR;
        }
      }
    }
    tile.setPixels(pixels);
  }

  @Override public synchronized void reset() {
    // Do nothing.
  }

  @Override public synchronized void loadChunk() {
    // Do nothing.
  }

  @Override public String toString() {
    return "Chunk: [empty]";
  }

  @Override
  public void addChunkToScene(Octree worldOctree, Octree waterOctree, BlockPalette palette, Vector3i origin, int yClipMin, int yClipMax, Collection<Entity> entities, Collection<Entity> actors, Heightmap biomeIdMap) {

  }
}
