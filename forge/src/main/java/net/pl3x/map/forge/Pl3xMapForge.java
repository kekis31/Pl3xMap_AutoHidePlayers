/*
 * MIT License
 *
 * Copyright (c) 2020-2023 William Blake Galbreath
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.pl3x.map.forge;

import cloud.commandframework.forge.CloudForgeEntrypoint;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.platform.AudienceProvider;
import net.kyori.adventure.platform.forge.ForgeServerAudiences;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.event.server.ServerLoadedEvent;
import net.pl3x.map.core.player.Player;
import net.pl3x.map.core.player.PlayerListener;
import net.pl3x.map.core.player.PlayerRegistry;
import net.pl3x.map.core.world.World;
import net.pl3x.map.forge.capability.HiddenCapability;
import net.pl3x.map.forge.command.ForgeCommandManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod("pl3xmap")
public class Pl3xMapForge extends Pl3xMap {
    private final PlayerListener playerListener = new PlayerListener();

    @SuppressWarnings("deprecation")
    private final RandomSource randomSource = RandomSource.createThreadSafe();

    private MinecraftServer server;
    private IModInfo modInfo;

    private ForgeServerAudiences adventure;

    private int tick;

    private ForgeNetwork network;

    @SuppressWarnings("InstantiationOfUtilityClass")
    public Pl3xMapForge() {
        super(false);

        new CloudForgeEntrypoint();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new HiddenCapability());

        try {
            new ForgeCommandManager();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(HiddenCapability.class);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.@NotNull ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && this.tick++ >= 20) {
            this.tick = 0;
            getScheduler().tick();
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.@NotNull PlayerLoggedInEvent event) {
        PlayerRegistry registry = Pl3xMap.api().getPlayerRegistry();
        UUID uuid = event.getEntity().getUUID();
        Player forgePlayer = registry.getOrDefault(uuid, () -> new ForgePlayer((ServerPlayer) event.getEntity()));
        this.playerListener.onJoin(forgePlayer);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.@NotNull PlayerLoggedOutEvent event) {
        PlayerRegistry registry = Pl3xMap.api().getPlayerRegistry();
        UUID uuid = event.getEntity().getUUID();
        Player forgePlayer = registry.unregister(uuid);
        if (forgePlayer != null) {
            this.playerListener.onQuit(forgePlayer);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (!isEnabled()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        String name = level.dimension().location().toString();
        Pl3xMap.api().getWorldRegistry().getOrDefault(name, () -> new ForgeWorld(level, name));
    }

    @SubscribeEvent
    public void onWorldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        String name = level.dimension().location().toString();
        Pl3xMap.api().getWorldRegistry().unregister(name);
    }

    @SubscribeEvent
    public void onServerStarted(@NotNull ServerStartedEvent event) {
        this.server = event.getServer();
        this.adventure = new ForgeServerAudiences(this.server);

        enable();

        this.network = new ForgeNetwork(this);
        this.network.register();
    }

    @SubscribeEvent
    public void onServerStopping(@NotNull ServerStoppingEvent event) {
        if (this.network != null) {
            this.network.unregister();
            this.network = null;
        }

        disable();

        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    @SubscribeEvent
    public void onServerLoad(ServerStartedEvent event) {
        Pl3xMap.api().getEventRegistry().callEvent(new ServerLoadedEvent());
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer) {
            getPlayerRegistry().getOrDefault(newPlayer.getUUID(), () -> new ForgePlayer(newPlayer)).setPlayer(newPlayer);
        }
    }

    public @NotNull IModInfo getModInfo() {
        if (this.modInfo == null) {
            this.modInfo = ModList.get().getModContainerById("pl3xmap").orElseThrow().getModInfo();
        }
        return this.modInfo;
    }

    @Override
    public @NotNull String getPlatform() {
        return this.server.getServerModName().toLowerCase(Locale.ROOT);
    }

    @Override
    public @NotNull String getVersion() {
        return getModInfo().getVersion().toString();
    }

    @Override
    public int getMaxPlayers() {
        return this.server.getMaxPlayers();
    }

    @Override
    public boolean getOnlineMode() {
        return this.server.usesAuthentication();
    }

    @Override
    public String getServerVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    @Override
    public @NotNull AudienceProvider adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure without a running server!");
        }
        return this.adventure;
    }

    @Override
    public @NotNull Path getMainDir() {
        return FMLPaths.GAMEDIR.get().resolve("config").resolve("pl3xmap");
    }

    @Override
    public @NotNull Path getJarPath() {
        return getModInfo().getOwningFile().getFile().getFilePath();
    }

    @Override
    public int getColorForPower(byte power) {
        return RedStoneWireBlock.getColorForPower(power);
    }

    @Override
    public net.pl3x.map.core.world.@Nullable Block getFlower(@NotNull World world, net.pl3x.map.core.world.@NotNull Biome biome, int blockX, int blockY, int blockZ) {
        // https://github.com/Draradech/FlowerMap (CC0-1.0 license)
        Biome nms = world.<ServerLevel>getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation(biome.getKey()));
        if (nms == null) {
            return null;
        }
        List<ConfiguredFeature<?, ?>> flowers = nms.getGenerationSettings().getFlowerFeatures();
        if (flowers.isEmpty()) {
            return null;
        }
        RandomPatchConfiguration config = (RandomPatchConfiguration) flowers.get(0).config();
        SimpleBlockConfiguration flower = (SimpleBlockConfiguration) config.feature().value().feature().value().config();
        Block block = flower.toPlace().getState(this.randomSource, new BlockPos(blockX, blockY, blockZ)).getBlock();
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
        return key == null ? null : getBlockRegistry().get(key.toString());
    }

    @Override
    protected void loadBlocks() {
        for (Map.Entry<ResourceKey<Block>, Block> entry : this.server.registryAccess().registryOrThrow(Registries.BLOCK).entrySet()) {
            String id = entry.getKey().location().toString();
            int color = entry.getValue().defaultMapColor().col;
            getBlockRegistry().register(id, color);
        }
        getBlockRegistry().saveToDisk();
    }

    @Override
    protected void loadWorlds() {
        this.server.getAllLevels().forEach(level -> {
            String name = level.dimension().location().toString();
            Pl3xMap.api().getWorldRegistry().getOrDefault(name, () -> new ForgeWorld(level, name));
        });
    }

    @Override
    protected void loadPlayers() {
        this.server.getPlayerList().getPlayers().forEach(player -> {
            UUID uuid = player.getUUID();
            getPlayerRegistry().getOrDefault(uuid, () -> new ForgePlayer(player));
        });
    }

    @Override
    public @NotNull World cloneWorld(@NotNull World world) {
        return new ForgeWorld(world.getLevel(), world.getName());
    }

    public @Nullable MinecraftServer getServer() {
        return this.server;
    }
}
