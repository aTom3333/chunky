package se.llbit.chunky.world;


import se.llbit.math.Vector3i;

import java.util.Set;

/**
 * Represents a selection of block to be loaded from the world and rendered
 */
public class Selection {
  /**
   * For now, it will use chunk position to reduce the amount of changes
   * TODO This will change in future version to remove the notion of chunk from the public interface of the world
   */
  private Set<ChunkPosition> chunkPositions;
  private int yMin;
  private int yMax;

  public Selection(Set<ChunkPosition> chunkPositions, int yMin, int yMax) {
    this.chunkPositions = chunkPositions;
    this.yMin = yMin;
    this.yMax = yMax;
  }

  /**
   * Checks whether a block is in the selection
   * @param x x coordinate of the block to check
   * @param y y coordinate of the block to check
   * @param z z coordinate of the block to check
   * @return true if the block is in the selection, false otherwise
   */
  public boolean isInSelection(int x, int y, int z) {
    int cx = x >> 4;
    int cz = z >> 4;
    return y >= yMin && y <= yMax && chunkPositions.contains(ChunkPosition.get(cx, cz));
  }

  /**
   * Checks whether a block is in the selection
   * @param pos The coordinates of the block to check
   * @return true if the block is in the selection, false otherwise
   */
  public boolean isInSelection(Vector3i pos) {
    return isInSelection(pos.x, pos.y, pos.z);
  }
}
