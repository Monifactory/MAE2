/*
 * Copyright (C) 2024 AE2 Enthusiast
 *
 * This file is part of MAE2.
 *
 * MAE2 is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * MAE2 is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see https://www.gnu.org/licenses/.
 */
package stone.mae2.bootstrap;

import appeng.api.features.P2PTunnelAttunement;
import appeng.api.parts.PartModels;
import appeng.block.crafting.CraftingBlockItem;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import net.minecraft.Util;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.MAE2;
import stone.mae2.item.faulty.FaultyMemoryCardItem;
import stone.mae2.parts.p2p.PatternP2PTunnelPart;
import stone.mae2.parts.p2p.multi.FEMultiP2PPart;
import stone.mae2.parts.p2p.multi.FluidMultiP2PPart;
import stone.mae2.parts.p2p.multi.ItemMultiP2PPart;
import stone.mae2.parts.p2p.multi.PatternMultiP2PPart;
import stone.mae2.parts.p2p.multi.RedstoneMultiP2PPart;

public abstract class MAE2Items {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister
            .create(ForgeRegistries.ITEMS, MAE2.MODID);

    public static RegistryObject<PartItem<PatternP2PTunnelPart>> PATTERN_P2P_TUNNEL;
    
    public static RegistryObject<PartItem<PatternMultiP2PPart>> PATTERN_MULTI_P2P_TUNNEL;
    public static RegistryObject<PartItem<RedstoneMultiP2PPart>> REDSTONE_MULTI_P2P_TUNNEL;
    public static RegistryObject<PartItem<FEMultiP2PPart>> FE_MULTI_P2P_TUNNEL;
    public static RegistryObject<PartItem<FluidMultiP2PPart>> FLUID_MULTI_P2P_TUNNEL;
    public static RegistryObject<PartItem<ItemMultiP2PPart>> ITEM_MULTI_P2P_TUNNEL;

    public static RegistryObject<CraftingBlockItem> ACCELERATOR_4x;
    public static RegistryObject<CraftingBlockItem> ACCELERATOR_16x;
    public static RegistryObject<CraftingBlockItem> ACCELERATOR_64x;
    public static RegistryObject<CraftingBlockItem> ACCELERATOR_256x;

    public static RegistryObject<FaultyMemoryCardItem> FAULTY_MEMORY_CARD;

    public static RegistryObject<BlockItem> CLOUD_CHAMBER;
    public static RegistryObject<BlockItem> TRAIL_SOURCE;

    public static void init(IEventBus bus) {
        register();
        ITEMS.register(bus);

        bus.addListener((FMLCommonSetupEvent event) -> {
            P2PTunnelAttunement
                    .registerAttunementTag(PATTERN_P2P_TUNNEL.get());
        });
    }

    @SuppressWarnings("unchecked")
    public static void register() {
        PATTERN_P2P_TUNNEL = Util.make(() -> {
            PartModels.registerModels(
                    PartModelsHelper.createModels(PatternP2PTunnelPart.class));
            return ITEMS.register("pattern_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(),
                            PatternP2PTunnelPart.class, PatternP2PTunnelPart::new));
        });
        
        PATTERN_MULTI_P2P_TUNNEL = Util.make(() -> {
            PartModels.registerModels(
                    PartModelsHelper.createModels(PatternMultiP2PPart.class));
            return ITEMS.register("pattern_multi_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(), PatternMultiP2PPart.class, PatternMultiP2PPart::new));
        });
        REDSTONE_MULTI_P2P_TUNNEL = Util.make(() -> {
            PartModels.registerModels(
                    PartModelsHelper.createModels(RedstoneMultiP2PPart.class));
            return ITEMS.register("redstone_multi_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(),
                            RedstoneMultiP2PPart.class, RedstoneMultiP2PPart::new));
        });
        FE_MULTI_P2P_TUNNEL = Util.make(() -> {
            PartModels.registerModels(
                    PartModelsHelper.createModels(FEMultiP2PPart.class));
            return ITEMS.register("fe_multi_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(),
                            FEMultiP2PPart.class, FEMultiP2PPart::new));
        });
        FLUID_MULTI_P2P_TUNNEL = Util.make(() -> {
            PartModels.registerModels(
                    PartModelsHelper.createModels(FluidMultiP2PPart.class));
            return ITEMS.register("fluid_multi_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(),
                            FluidMultiP2PPart.class, FluidMultiP2PPart::new));
        });
        ITEM_MULTI_P2P_TUNNEL = Util.make(() -> {
            PartModels.registerModels(
                    PartModelsHelper.createModels(ItemMultiP2PPart.class));
            return ITEMS.register("item_multi_p2p_tunnel",
                    () -> new PartItem<>(new Item.Properties(),
                            ItemMultiP2PPart.class, ItemMultiP2PPart::new));
        });
        
        FAULTY_MEMORY_CARD = ITEMS.register("faulty_card", FaultyMemoryCardItem::new);
    }

}
