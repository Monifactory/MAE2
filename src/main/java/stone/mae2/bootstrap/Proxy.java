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

import appeng.api.integrations.igtooltip.PartTooltips;
import appeng.api.networking.GridServices;
import appeng.client.render.crafting.CraftingCubeModel;
import appeng.client.render.model.GlassModel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.MAE2;
import stone.mae2.api.features.MultiP2PTunnelAttunement;
import stone.mae2.block.crafting.DynamicCraftingUnitType;
import stone.mae2.client.render.crafting.DynamicCraftingCubeModelProvider;
import stone.mae2.client.render.model.FaultyCardModel;
import stone.mae2.core.datagen.MAE2RecipeProvider;
import stone.mae2.hooks.BuiltInModelHooks;
import stone.mae2.integration.GregTechIntegration;
import stone.mae2.integration.MultiP2PStateDataProvider;
import stone.mae2.item.faulty.FaultyMemoryCardItem;
import stone.mae2.me.service.MultiP2PService;
import stone.mae2.parts.p2p.multi.MultiP2PTunnelPart;

public interface Proxy {
    public class Server implements Proxy {
        public void init(IEventBus bus) {
            MAE2Blocks.init(bus);
            MAE2Items.init(bus);
            MAE2Tags.init(bus);

            if (ModList.get().isLoaded("gtceu")) {
                GregTechIntegration.init(bus);
            }

            bus.addListener((FMLCommonSetupEvent event) -> {
                GridServices.register(MultiP2PService.class, MultiP2PService.class);
                MultiP2PTunnelAttunement.registerStockAttunements();
            });

            bus.addListener((GatherDataEvent event) -> {
                DataGenerator gen = event.getGenerator();
                DataGenerator.PackGenerator pack = gen.getVanillaPack(true);

                pack.addProvider(MAE2RecipeProvider::new);
            });

            PartTooltips.addServerData(MultiP2PTunnelPart.class, new MultiP2PStateDataProvider());
        }
    }

    public class Client implements Proxy {
        private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister
                .create(Registries.CREATIVE_MODE_TAB, MAE2.MODID);
        public static RegistryObject<CreativeModeTab> CREATIVE_TAB;

        public void init(IEventBus bus) {
            new Server().init(bus);
            TABS.register(bus);
            CREATIVE_TAB = TABS
                    .register("main", () -> CreativeModeTab.builder()
                            .title(Component
                                    .translatable("gui." + MAE2.MODID + ".creative_tab"))
                            .icon(
                                    () -> new ItemStack(MAE2Items.PATTERN_P2P_TUNNEL.get()))
                            .displayItems((params, output) -> {
                                for (var entry : MAE2Items.ITEMS.getEntries()) {
                                    output.accept(entry.get());
                                }
                            }).build());

            BuiltInModelHooks.addBuiltInModel(
                    MAE2.toKey("block/crafting/4x_accelerator_formed"),
                    new CraftingCubeModel(
                            new DynamicCraftingCubeModelProvider(DynamicCraftingUnitType.ACCELERATOR_4x)));

            BuiltInModelHooks.addBuiltInModel(
                    MAE2.toKey("block/crafting/16x_accelerator_formed"),
                    new CraftingCubeModel(
                            new DynamicCraftingCubeModelProvider(DynamicCraftingUnitType.ACCELERATOR_16x)));

            BuiltInModelHooks.addBuiltInModel(
                    MAE2.toKey("block/crafting/64x_accelerator_formed"),
                    new CraftingCubeModel(
                            new DynamicCraftingCubeModelProvider(DynamicCraftingUnitType.ACCELERATOR_64x)));

            BuiltInModelHooks
                    .addBuiltInModel(
                            MAE2.toKey("block/crafting/256x_accelerator_formed"),
                            new CraftingCubeModel(
                                    new DynamicCraftingCubeModelProvider(
                                            DynamicCraftingUnitType.ACCELERATOR_256x)));

            BuiltInModelHooks
                .addBuiltInModel(MAE2.toKey("item/faulty_card"), new FaultyCardModel());
            BuiltInModelHooks.addBuiltInModel(MAE2.toKey("block/cloud_chamber"), new GlassModel());
            

            PartTooltips.addBody(MultiP2PTunnelPart.class, new MultiP2PStateDataProvider());

            bus.addListener((RegisterColorHandlersEvent.Item event) -> {
                    event.register(FaultyMemoryCardItem::getTintColor, MAE2Items.FAULTY_MEMORY_CARD.get());
                });
            bus.addListener((FMLClientSetupEvent event) -> {
                event.enqueueWork(() -> {
                    ItemBlockRenderTypes
                    .setRenderLayer(MAE2Blocks.CLOUD_CHAMBER.get(), RenderType.cutout());
                });
            });
        }
    }

    public void init(IEventBus bus);
}
