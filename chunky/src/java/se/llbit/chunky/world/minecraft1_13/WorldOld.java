package se.llbit.chunky.world.minecraft1_13;

import se.llbit.chunky.world.ChunkPosition;
import se.llbit.chunky.world.PlayerEntityData;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WorldOld extends se.llbit.chunky.world.World {
    public WorldOld(String levelName, File worldDirectory, int dimension,
                    Set<PlayerEntityData> playerEntities, boolean haveSpawnPos, long seed, long timestamp) {
        super(levelName, worldDirectory, dimension, playerEntities, haveSpawnPos, seed, timestamp);
    }

    @Override
    public RegionOld createRegion(ChunkPosition pos) {
        return new RegionOld(pos, this);
    }

    @Override
    protected void appendRegionToZip(ZipOutputStream zout, File regionDirectory,
                                   ChunkPosition regionPos, String regionZipFileName, Set<ChunkPosition> chunks)
            throws IOException {

        zout.putNextEntry(new ZipEntry(regionZipFileName));
        RegionOld.writeRegion(regionDirectory, regionPos, new DataOutputStream(zout), chunks);
        zout.closeEntry();
    }
}
