package se.llbit.chunky.block;

import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.resources.Texture;
import se.llbit.math.Ray;

public class SnowCovered extends MinecraftBlock {
  private final Block block;

  public SnowCovered(Block block) {
    super(block.name, Texture.snowBlock);
    this.block = block;
    localIntersect = true;
  }

  @Override public boolean intersect(Ray ray, Scene scene) {
    ray.t = Double.POSITIVE_INFINITY;
    boolean hit = block.intersect(ray, scene);
    if (hit) {
      if (ray.n.y == 0) {
        Texture.snowSide.getColor(ray);
      } else if (ray.n.y > 0) {
        Texture.snowBlock.getColor(ray);
      }
    }
    return hit;
  }
}
