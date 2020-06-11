package se.llbit.chunky.world.minecraft1_13;

import se.llbit.chunky.block.Air;
import se.llbit.chunky.block.Block;
import se.llbit.chunky.block.Lava;
import se.llbit.chunky.block.Water;
import se.llbit.chunky.chunk.BlockPalette;
import se.llbit.chunky.entity.ArmorStand;
import se.llbit.chunky.entity.Entity;
import se.llbit.chunky.entity.PaintingEntity;
import se.llbit.chunky.map.BiomeLayer;
import se.llbit.chunky.map.IconLayer;
import se.llbit.chunky.map.SurfaceLayer;
import se.llbit.chunky.world.*;
import se.llbit.chunky.world.Region;
import se.llbit.chunky.world.World;
import se.llbit.math.Octree;
import se.llbit.math.QuickMath;
import se.llbit.math.Vector3;
import se.llbit.math.Vector3i;
import se.llbit.nbt.*;
import se.llbit.util.BitBuffer;
import se.llbit.util.NotNull;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class ChunkOld extends se.llbit.chunky.world.Chunk {

  private static final String LEVEL_ENTITIES = ".Level.Entities";
  private static final String LEVEL_TILEENTITIES = ".Level.TileEntities";

  public ChunkOld(ChunkPosition pos, World world) {
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


  /**
   * Parse the chunk from the region file and render the current
   * layer, surface and cave maps.
   */
  @Override
  public synchronized void loadChunk() {
    if (!shouldReloadChunk()) {
      return;
    }

    Set<String> request = new HashSet<>();
    request.add(se.llbit.chunky.world.Chunk.LEVEL_SECTIONS);
    request.add(se.llbit.chunky.world.Chunk.LEVEL_BIOMES);
    request.add(se.llbit.chunky.world.Chunk.LEVEL_HEIGHTMAP);
    Map<String, Tag> data = getChunkData(request);

    surfaceTimestamp = dataTimestamp;
    version = chunkVersion(data);
    loadSurface(data);
    biomesTimestamp = dataTimestamp;
    if (surface == IconLayer.MC_1_12) {
      biomes = IconLayer.MC_1_12;
    } else {
      loadBiomes(data);
    }
    world.chunkUpdated(position);
  }

  /**
   * @param request fresh request set
   * @return loaded data, or null if something went wrong
   */
  private Map<String, Tag> getChunkData(Set<String> request) {
    Region region = world.getRegion(position.getRegionPosition());
    ChunkDataSource data = region.getChunkData(position);
    dataTimestamp = data.timestamp;
    if (data.inputStream != null) {
      try (DataInputStream in = data.inputStream) {
        Map<String, Tag> result = NamedTag.quickParse(in, request);
        for (String key : request) {
          if (!result.containsKey(key)) {
            result.put(key, new ErrorTag(""));
          }
        }
        return result;
      } catch (IOException e) {
        // Ignored.
      }
    }
    return null;
  }

  private int[] extractHeightmapData(@NotNull Map<String, Tag> data) {
    Tag heightmapTag = data.get(LEVEL_HEIGHTMAP);
    if (heightmapTag.isIntArray(X_MAX * Z_MAX)) {
      return heightmapTag.intArray();
    } else {
      int[] fallback = new int[X_MAX * Z_MAX];
      for (int i = 0; i < fallback.length; ++i) {
        fallback[i] = Y_MAX - 1;
      }
      return fallback;
    }
  }

  private static void loadBlockData(@NotNull Map<String, Tag> data, @NotNull int[] blocks,
                                    BlockPalette blockPalette) {
    Tag sections = data.get(LEVEL_SECTIONS);
    if (sections.isList()) {
      for (SpecificTag section : sections.asList()) {
        Tag yTag = section.get("Y");
        int yOffset = yTag.byteValue() & 0xFF;

        if (section.get("Palette").isList()) {
          ListTag palette = section.get("Palette").asList();
          // Bits per block:
          int bpb = 4;
          if (palette.size() > 16) {
            bpb = QuickMath.log2(QuickMath.nextPow2(palette.size()));
            //bpb = QuickMath.log2(palette.size());
          }

          int dataSize = (4096 * bpb) / 64;
          Tag blockStates = section.get("BlockStates");

          if (blockStates.isLongArray(dataSize)) {
            // since 20w17a, block states are aligned to 64-bit boundaries, so there are 64 % bpb
            // unused bits per block state; if so, the array is longer than the expected data size
            boolean isAligned = blockStates.longArray().length > dataSize;

            int[] subpalette = new int[palette.size()];
            int paletteIndex = 0;
            for (Tag item : palette.asList()) {
              subpalette[paletteIndex] = blockPalette.put(item);
              paletteIndex += 1;
            }
            BitBuffer buffer = new BitBuffer(blockStates.longArray(), bpb, isAligned);
            int offset = SECTION_BYTES * yOffset;
            for (int i = 0; i < SECTION_BYTES; ++i) {
              int b0 = buffer.read();
              if (b0 < subpalette.length) {
                blocks[offset] = subpalette[b0];
              }
              offset += 1;
            }
          }
        } else {
          //Log.error(">>> WIP <<< Old chunk format temp disabled.");
          /*Tag blocksTag = section.get("Blocks");
          if (blocksTag.isByteArray(SECTION_BYTES)) {
            System.arraycopy(blocksTag.byteArray(), 0, blocks, SECTION_BYTES * yOffset,
                SECTION_BYTES);
          }
          Tag dataTag = section.get("Data");
          if (dataTag.isByteArray(SECTION_HALF_NIBBLES)) {
            System.arraycopy(dataTag.byteArray(), 0, blockData, SECTION_HALF_NIBBLES * yOffset,
                SECTION_HALF_NIBBLES);
          }*/
        }
      }
    }
  }

  private void queueTopography() {
    for (int x = -1; x <= 1; ++x) {
      for (int z = -1; z <= 1; ++z) {
        ChunkPosition pos = ChunkPosition.get(position.x + x, position.z + z);
        se.llbit.chunky.world.Chunk chunk = world.getChunk(pos);
        if (!chunk.isEmpty()) {
          world.chunkTopographyUpdated(chunk);
        }
      }
    }
  }


  private void loadSurface(Map<String, Tag> data) {
    if (data == null) {
      surface = IconLayer.CORRUPT;
      return;
    }

    Heightmap heightmap = world.heightmap();
    Tag sections = data.get(LEVEL_SECTIONS);
    if (sections.isList()) {
      int[] heightmapData = extractHeightmapData(data);
      byte[] biomeData = new byte[X_MAX * Z_MAX];
      extractBiomeData(data.get(LEVEL_BIOMES), biomeData);
      int[] blockData = new int[CHUNK_BYTES];
      if (version.equals("1.13")) {
        BlockPalette palette = new BlockPalette();
        loadBlockData(data, blockData, palette);
        updateHeightmap(heightmap, position, blockData, heightmapData, palette);
        surface = new SurfaceLayer(world.currentDimension(), blockData, biomeData, palette);
        queueTopography();
      } else if (version.equals("1.12")) {
        surface = IconLayer.MC_1_12;
      }
    } else {
      surface = IconLayer.CORRUPT;
    }
  }

  private void loadBiomes(Map<String, Tag> data) {
    if (data == null) {
      biomes = IconLayer.CORRUPT;
    } else {
      byte[] biomeData = new byte[X_MAX * Z_MAX];
      extractBiomeData(data.get(LEVEL_BIOMES), biomeData);
      biomes = new BiomeLayer(biomeData);
    }
  }

  /**
   * Extracts biome IDs from chunk data into the second argument.
   *
   * @param biomesTag the .Level.Biomes NBT tag to load data from.
   * @param output a byte array of length 16x16.
   */
  private void extractBiomeData(@NotNull Tag biomesTag, byte[] output) {
    if (biomesTag.isByteArray(X_MAX * Z_MAX)) {
      System.arraycopy(biomesTag.byteArray(), 0, output, 0, X_MAX * Z_MAX);
    } else if (biomesTag.isIntArray(X_MAX * Z_MAX)) {
      // Since Minecraft 1.13, biome IDs are stored in an int vector.
      // TODO(llbit): do we need to use ints to store biome IDs for Minecraft 1.13+?
      int[] data = biomesTag.intArray();
      for (int i = 0; i < X_MAX * Z_MAX; ++i) {
        output[i] = (byte) data[i];
      }
    }
  }



  /**
   * Load the block data for this chunk.
   *
   * @param blocks block order: y, z, x.
   */
  public synchronized void getBlockData(int[] blocks, byte[] biomes,
                                        Collection<CompoundTag> tileEntities, Collection<CompoundTag> entities,
                                        BlockPalette blockPalette) {

    for (int i = 0; i < CHUNK_BYTES; ++i) {
      blocks[i] = blockPalette.airId;
    }

    for (int i = 0; i < X_MAX * Z_MAX; ++i) {
      biomes[i] = 0;
    }

    Set<String> request = new HashSet<>();
    request.add(LEVEL_SECTIONS);
    request.add(LEVEL_BIOMES);
    request.add(LEVEL_ENTITIES);
    request.add(LEVEL_TILEENTITIES);
    Map<String, Tag> data = getChunkData(request);
    // TODO: improve error handling here.
    if (data == null) {
      return;
    }
    Tag sections = data.get(LEVEL_SECTIONS);
    Tag biomesTag = data.get(LEVEL_BIOMES);
    Tag entitiesTag = data.get(LEVEL_ENTITIES);
    Tag tileEntitiesTag = data.get(LEVEL_TILEENTITIES);
    if (biomesTag.isByteArray(X_MAX * Z_MAX) || biomesTag.isIntArray(X_MAX * Z_MAX)) {
      extractBiomeData(biomesTag, biomes);
    }
    if (sections.isList() && tileEntitiesTag.isList()
            && entitiesTag.isList()) {
      loadBlockData(data, blocks, blockPalette);
      ListTag list = (ListTag) entitiesTag;
      for (SpecificTag tag : list) {
        if (tag.isCompoundTag())
          entities.add((CompoundTag) tag);
      }
      list = (ListTag) tileEntitiesTag;
      for (SpecificTag tag : list) {
        if (tag.isCompoundTag())
          tileEntities.add((CompoundTag) tag);
      }
    }
  }

  /** Detect Minecraft version that generated the chunk. */
  private static String chunkVersion(@NotNull Map<String, Tag> data) {
    Tag sections = data.get(LEVEL_SECTIONS);
    String version = "?";
    if (sections.isList()) {
      version = "1.13";
      for (SpecificTag section : sections.asList()) {
        if (!section.get("Palette").isList()) {
          if (!version.equals("?") && section.get("Blocks").isByteArray(SECTION_BYTES)) {
            version = "1.12";
          }
        }
      }
    }
    return version;
  }
}
