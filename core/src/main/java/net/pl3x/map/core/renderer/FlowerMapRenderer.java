package net.pl3x.map.core.renderer;

import java.util.HashMap;
import java.util.Map;
import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.renderer.task.RegionScanTask;
import net.pl3x.map.core.util.Colors;
import net.pl3x.map.core.world.Biome;
import net.pl3x.map.core.world.Block;
import net.pl3x.map.core.world.Blocks;
import net.pl3x.map.core.world.Chunk;
import net.pl3x.map.core.world.Region;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FlowerMapRenderer extends Renderer {
    private final Map<Block, Integer> colorMap = new HashMap<>();

    public FlowerMapRenderer(@NonNull RegionScanTask task, @NonNull Builder builder) {
        super(task, builder);
        this.colorMap.put(Blocks.DANDELION, 0xFFFF00);
        this.colorMap.put(Blocks.POPPY, 0xFF0000);
        this.colorMap.put(Blocks.ALLIUM, 0x9900FF);
        this.colorMap.put(Blocks.AZURE_BLUET, 0xFFFDDD);
        this.colorMap.put(Blocks.RED_TULIP, 0xFF4D62);
        this.colorMap.put(Blocks.ORANGE_TULIP, 0xFFB55A);
        this.colorMap.put(Blocks.WHITE_TULIP, 0xDDFFFF);
        this.colorMap.put(Blocks.PINK_TULIP, 0xF5B4FF);
        this.colorMap.put(Blocks.OXEYE_DAISY, 0xFFEEDD);
        this.colorMap.put(Blocks.CORNFLOWER, 0x4100FF);
        this.colorMap.put(Blocks.LILY_OF_THE_VALLEY, 0xFFFFFF);
        this.colorMap.put(Blocks.BLUE_ORCHID, 0x00BFFF);
    }

    @Override
    public void scanBlock(@NonNull Region region, @NonNull Chunk chunk, Chunk.@NonNull BlockData data, int blockX, int blockZ) {
        int pixelColor = 0x7F7F7F;

        Biome biome = data.getBiome(region, blockX, blockZ);

        Block flower = Pl3xMap.api().getFlower(region.getWorld(), biome, blockX, data.getBlockY(), blockZ);
        if (flower != null) {
            pixelColor = (0xFF << 24) | (this.colorMap.getOrDefault(flower, pixelColor) & 0xFFFFFF);
        }

        // work out the heightmap
        pixelColor = Colors.blend(getHeightmap().getColor(region, blockX, blockZ), pixelColor);

        // fluid stuff
        if (data.getFluidState() != null) {
            if (getWorld().getConfig().RENDER_TRANSLUCENT_FLUIDS) {
                int fluidColor = fancyFluids(region, biome, data.getFluidState(), blockX, blockZ, (data.getFluidY() - data.getBlockY()) * 0.025F);
                pixelColor = Colors.blend(fluidColor, pixelColor);
            } else {
                pixelColor = Colors.getWaterColor(region, biome, blockX, blockZ);
            }
        }

        // draw color data to image
        getTileImage().setPixel(blockX, blockZ, pixelColor);
    }
}
