package se.llbit.chunky.world.minecraft1_13;

import se.llbit.chunky.map.AbstractLayer;
import se.llbit.chunky.map.IconLayer;
import se.llbit.chunky.world.ChunkPosition;
import se.llbit.nbt.ErrorTag;
import se.llbit.nbt.NamedTag;
import se.llbit.nbt.Tag;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExistingChunk implements Chunk {
  private final ChunkPosition pos;
  private final LoadedRegion region;
  private AbstractLayer surface = IconLayer.UNKNOWN;
  private AbstractLayer biome = IconLayer.UNKNOWN;


  public ExistingChunk(ChunkPosition pos, LoadedRegion region) {
    this.pos = pos;
    this.region = region;
  }

  public void preLoad() {
    // TODO Not reload chunk everytime
    //    if (!shouldReloadChunk()) {
//      return;
//    }

    Set<String> request = new HashSet<>();
    request.add(se.llbit.chunky.world.Chunk.LEVEL_SECTIONS);
    request.add(se.llbit.chunky.world.Chunk.LEVEL_BIOMES);
    request.add(se.llbit.chunky.world.Chunk.LEVEL_HEIGHTMAP);
    Map<String, Tag> data = getChunkData(request);

    // TODO Next
//    loadSurface(data);
//    if (surface == IconLayer.MC_1_12) {
//      biomes = IconLayer.MC_1_12;
//    } else {
//      loadBiomes(data);
//    }
    // TODO Notify chunk update
    //region.getWorld().chunkUpdated(position);
  }

  private Map<String, Tag> getChunkData(Set<String> request) {
    InputStream input = null;
    try {
      input = region.getChunkInputStream(pos);
    } catch(IOException ignored) {
    }
    if (input != null) {
      try (DataInputStream in = new DataInputStream(input)) {
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

  @Override
  public boolean isEmpty() {
    return false;
  }
}
