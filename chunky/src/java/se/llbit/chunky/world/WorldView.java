package se.llbit.chunky.world;

public class WorldView {
  public final int minX, maxX, minY, maxY, minZ, maxZ;

  public WorldView(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
    this.minX = minX;
    this.maxX = maxX;
    this.minY = minY;
    this.maxY = maxY;
    this.minZ = minZ;
    this.maxZ = maxZ;
  }
}
