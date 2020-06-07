package se.llbit.chunky.world.minecraft1_13;

import se.llbit.chunky.block.Air;
import se.llbit.chunky.block.Block;
import se.llbit.chunky.block.Lava;
import se.llbit.chunky.block.Water;
import se.llbit.chunky.chunk.BlockPalette;
import se.llbit.chunky.entity.ArmorStand;
import se.llbit.chunky.entity.Entity;
import se.llbit.chunky.entity.PaintingEntity;
import se.llbit.chunky.world.ChunkPosition;
import se.llbit.chunky.world.Heightmap;
import se.llbit.chunky.world.World;
import se.llbit.math.Octree;
import se.llbit.math.Vector3;
import se.llbit.math.Vector3i;
import se.llbit.nbt.CompoundTag;
import se.llbit.nbt.ListTag;
import se.llbit.nbt.Tag;

import java.util.Collection;
import java.util.LinkedList;

public class Chunk extends se.llbit.chunky.world.Chunk {
  public Chunk(ChunkPosition pos, World world) {
    super(pos, world);
  }

  @Override
  public void addChunkToScene(Octree worldOctree, Octree waterOctree, BlockPalette palette,
                              Vector3i origin, int yClipMin, int yClipMax,
                              Collection<Entity> entities, Collection<Entity> actors,
                              Heightmap biomeIdMap) {
    Collection<CompoundTag> tileEntities = new LinkedList<>();
    Collection<CompoundTag> chunkEntities = new LinkedList<>();
    int[] blocks = new int[se.llbit.chunky.world.Chunk.X_MAX * se.llbit.chunky.world.Chunk.Y_MAX * se.llbit.chunky.world.Chunk.Z_MAX];
    byte[] biomes = new byte[se.llbit.chunky.world.Chunk.X_MAX * se.llbit.chunky.world.Chunk.Z_MAX];

    int yMin = Math.max(0, yClipMin);
    int yMax = Math.min(256, yClipMax);

    getBlockData(blocks, biomes, tileEntities, chunkEntities, palette);

    int wx0 = position.x * 16; // Start of this chunk in world coordinates.
    int wz0 = position.z * 16;
    for(int cz = 0; cz < 16; ++cz) {
      int wz = cz + wz0;
      for(int cx = 0; cx < 16; ++cx) {
        int wx = cx + wx0;
        int biomeId = 0xFF & biomes[se.llbit.chunky.world.Chunk.chunkXZIndex(cx, cz)];
        biomeIdMap.set(biomeId, wx, wz);
      }
    }

    // Load entities from the chunk:
    for(CompoundTag tag : chunkEntities) {
      Tag posTag = tag.get("Pos");
      if(posTag.isList()) {
        ListTag pos = posTag.asList();
        double x = pos.get(0).doubleValue();
        double y = pos.get(1).doubleValue();
        double z = pos.get(2).doubleValue();

        if(y >= yClipMin && y <= yClipMax) {
          String id = tag.get("id").stringValue("");
          if(id.equals("minecraft:painting") || id.equals("Painting")) {
            // Before 1.12 paintings had id=Painting.
            // After 1.12 paintings had id=minecraft:painting.
            float yaw = tag.get("Rotation").get(0).floatValue();
            entities.add(
                    new PaintingEntity(new Vector3(x, y, z), tag.get("Motive").stringValue(), yaw));
          } else if(id.equals("minecraft:armor_stand")) {
            actors.add(new ArmorStand(new Vector3(x, y, z), tag));
          }
        }
      }
    }

    for(int cy = yMin; cy < yMax; ++cy) {
      for(int cz = 0; cz < 16; ++cz) {
        int z = cz + position.z * 16 - origin.z;
        for(int cx = 0; cx < 16; ++cx) {
          int x = cx + position.x * 16 - origin.x;
          int index = se.llbit.chunky.world.Chunk.chunkIndex(cx, cy, cz);
          Octree.Node octNode = new Octree.Node(blocks[index]);
          Block block = palette.get(blocks[index]);

          if(block.isEntity()) {
            Vector3 pos = new Vector3(cx + position.x * 16, cy, cz + position.z * 16);
            entities.add(block.toEntity(pos));
            if(block.waterlogged) {
              block = palette.water;
              octNode = new Octree.Node(palette.waterId);
            } else {
              block = Air.INSTANCE;
              octNode = new Octree.Node(palette.airId);
            }
          }

          if(block.isWaterFilled()) {
            Octree.Node waterNode = new Octree.Node(palette.waterId);
            if(cy + 1 < yMax) {
              int above = se.llbit.chunky.world.Chunk.chunkIndex(cx, cy + 1, cz);
              Block aboveBlock = palette.get(blocks[above]);
              if(aboveBlock.isWaterFilled() || aboveBlock.solid) {
                waterNode = new Octree.DataNode(palette.waterId, 1 << Water.FULL_BLOCK);
              }
            }
            waterOctree.set(waterNode, x, cy - origin.y, z);
            if(block.isWater()) {
              // Move plain water blocks to the water octree.
              octNode = new Octree.Node(palette.airId);
            }
          } else if(cy + 1 < yMax && block instanceof Lava) {
            int above = se.llbit.chunky.world.Chunk.chunkIndex(cx, cy + 1, cz);
            Block aboveBlock = palette.get(blocks[above]);
            if(aboveBlock instanceof Lava) {
              octNode = new Octree.DataNode(blocks[index], 1 << Water.FULL_BLOCK);
            }
          }
          worldOctree.set(octNode, x, cy - origin.y, z);
        }
      }
    }

    // Block entities are also called "tile entities". These are extra bits of metadata
    // about certain blocks or entities.
    // Block entities are loaded after the base block data so that metadata can be updated.
    for(CompoundTag entityTag : tileEntities) {
      int y = entityTag.get("y").intValue(0);
      if(y >= yClipMin && y <= yClipMax) {
        int x = entityTag.get("x").intValue(0) - wx0; // Chunk-local coordinates.
        int z = entityTag.get("z").intValue(0) - wz0;
        int index = se.llbit.chunky.world.Chunk.chunkIndex(x, y, z);
        Block block = palette.get(blocks[index]);
        // Metadata is the old block data (to be replaced in future Minecraft versions?).
        Vector3 position = new Vector3(x + wx0, y, z + wz0);
        if(block.isBlockEntity()) {
          entities.add(block.toBlockEntity(position, entityTag));
        }
            /*
            switch (block) {
              case Block.HEAD_ID:
                entities.add(new SkullEntity(position, entityTag, metadata));
                break;
              case Block.WALL_BANNER_ID: {
                entities.add(new WallBanner(position, metadata, entityTag));
                break;
              }
            }
            */
      }
    }
  }
}
