package stone.mae2.core;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.ClientTickingBlockEntity;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.crafting.CraftingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import java.util.stream.Stream;

public abstract class MAE2BlockEntities {

    public static BlockEntityType<CraftingBlockEntity> DENSE_ACCELERATOR;
    public static BlockEntityType<CraftingBlockEntity> MAX_STORAGE;
    public static BlockEntityType<CraftingBlockEntity> MAX_ACCELERATOR;

    public static void init(IEventBus bus) {
        if (MAE2Config.areDenseCoprocessersEnabled)
        {
            bus.addListener((RegisterEvent event) ->
            {
                event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES,
                    helper ->
                    {
                        DENSE_ACCELERATOR = BlockEntityType.Builder.of(
                            (BlockPos pos,
                                BlockState state) -> new CraftingBlockEntity(
                                    DENSE_ACCELERATOR, pos, state),
                            Stream.of(MAE2Blocks.DENSE_ACCELERATORS)
                                .map((registryObject) -> registryObject.get())
                                .toArray(Block[]::new))
                            .build(null);
                        helper.register("dense_accelerator", DENSE_ACCELERATOR);

                        BlockEntityTicker<CraftingBlockEntity> serverTicker = null;
                        if (ServerTickingBlockEntity.class
                            .isAssignableFrom(CraftingBlockEntity.class))
                        {
                            serverTicker = (level, pos, state, entity) ->
                            {
                                ((ServerTickingBlockEntity) entity)
                                    .serverTick();
                            };
                        }
                        BlockEntityTicker<CraftingBlockEntity> clientTicker = null;
                        if (ClientTickingBlockEntity.class
                            .isAssignableFrom(CraftingBlockEntity.class))
                        {
                            clientTicker = (level, pos, state, entity) ->
                            {
                                ((ClientTickingBlockEntity) entity)
                                    .clientTick();
                            };
                        }

                        for (var block : MAE2Blocks.DENSE_ACCELERATORS)
                        {
                            AEBaseEntityBlock<CraftingBlockEntity> baseBlock = (AEBaseEntityBlock<CraftingBlockEntity>) block
                                .get();
                            baseBlock.setBlockEntity(CraftingBlockEntity.class,
                                DENSE_ACCELERATOR, clientTicker, serverTicker);
                        }
                    });
            });
        }

        if (MAE2Config.isMAXTierEnabled)
        {
            bus.addListener((RegisterEvent event) ->
            {
                event.register(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES,
                    helper ->
                    {
                        {
                        MAX_STORAGE = BlockEntityType.Builder.of(
                            (BlockPos pos,
                                BlockState state) -> new CraftingBlockEntity(
                                    MAX_STORAGE, pos, state),
                            MAE2Blocks.MAX_STORAGE.get())
                            .build(null);
                        helper.register("max_storage", MAX_STORAGE);
                            AEBaseBlockEntity.registerBlockEntityItem(
                                MAX_STORAGE,
                                MAE2Items.MAX_STORAGE.get());

                        BlockEntityTicker<CraftingBlockEntity> serverTicker = null;
                        if (ServerTickingBlockEntity.class
                            .isAssignableFrom(CraftingBlockEntity.class))
                        {
                            serverTicker = (level, pos, state, entity) ->
                            {
                                ((ServerTickingBlockEntity) entity)
                                    .serverTick();
                            };
                        }
                        BlockEntityTicker<CraftingBlockEntity> clientTicker = null;
                        if (ClientTickingBlockEntity.class
                            .isAssignableFrom(CraftingBlockEntity.class))
                        {
                            clientTicker = (level, pos, state, entity) ->
                            {
                                ((ClientTickingBlockEntity) entity)
                                    .clientTick();
                            };
                        }

                        AEBaseEntityBlock<CraftingBlockEntity> baseBlock = MAE2Blocks.MAX_STORAGE
                                .get();
                            baseBlock.setBlockEntity(CraftingBlockEntity.class,
                            MAX_STORAGE, clientTicker, serverTicker);

                        }
                        MAX_ACCELERATOR = BlockEntityType.Builder.of(
                            (BlockPos pos,
                                BlockState state) -> new CraftingBlockEntity(
                                    MAX_ACCELERATOR, pos, state),
                            MAE2Blocks.MAX_ACCELERATOR.get()).build(null);
                        helper.register("max_accelerator", MAX_ACCELERATOR);
                        AEBaseBlockEntity.registerBlockEntityItem(
                            MAX_ACCELERATOR, MAE2Items.MAX_ACCELERATOR.get());

                        BlockEntityTicker<CraftingBlockEntity> serverTicker = null;
                        if (ServerTickingBlockEntity.class
                            .isAssignableFrom(CraftingBlockEntity.class))
                        {
                            serverTicker = (level, pos, state, entity) ->
                            {
                                ((ServerTickingBlockEntity) entity)
                                    .serverTick();
                            };
                        }
                        BlockEntityTicker<CraftingBlockEntity> clientTicker = null;
                        if (ClientTickingBlockEntity.class
                            .isAssignableFrom(CraftingBlockEntity.class))
                        {
                            clientTicker = (level, pos, state, entity) ->
                            {
                                ((ClientTickingBlockEntity) entity)
                                    .clientTick();
                            };
                        }
                        AEBaseEntityBlock<CraftingBlockEntity> baseBlock = MAE2Blocks.MAX_ACCELERATOR
                            .get();
                        baseBlock.setBlockEntity(CraftingBlockEntity.class,
                            MAX_ACCELERATOR, clientTicker, serverTicker);
                    });
            });
        }
    }



}
