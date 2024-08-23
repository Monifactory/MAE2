package stone.mae2.core;

import appeng.api.networking.GridServices;
import appeng.client.render.crafting.CraftingCubeModel;
import appeng.core.AppEng;
import appeng.hooks.BuiltInModelHooks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.MAE2;
import stone.mae2.api.features.MultiP2PTunnelAttunement;
import stone.mae2.block.crafting.DynamicCraftingUnitType;
import stone.mae2.client.render.crafting.DynamicCraftingCubeModelProvider;
import stone.mae2.core.datagen.MAE2RecipeProvider;
import stone.mae2.integration.GregTechIntegration;
import stone.mae2.me.service.MultiP2PService;

public interface Proxy {
    public class Server implements Proxy {
        public void init(IEventBus bus) {
            MAE2Blocks.init(bus);
            MAE2Items.init(bus);
            
            if (ModList.get().isLoaded("gtceu"))
            {
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

            // have to use AE2's id because their model loader checks for the id
            BuiltInModelHooks.addBuiltInModel(
                    new ResourceLocation(AppEng.MOD_ID, "block/crafting/4x_accelerator_formed"),
                    new CraftingCubeModel(
                            new DynamicCraftingCubeModelProvider(DynamicCraftingUnitType.ACCELERATOR_4x)));

            BuiltInModelHooks.addBuiltInModel(
                    new ResourceLocation(AppEng.MOD_ID, "block/crafting/16x_accelerator_formed"),
                    new CraftingCubeModel(
                            new DynamicCraftingCubeModelProvider(DynamicCraftingUnitType.ACCELERATOR_16x)));

            BuiltInModelHooks.addBuiltInModel(
                    new ResourceLocation(AppEng.MOD_ID, "block/crafting/64x_accelerator_formed"),
                    new CraftingCubeModel(
                            new DynamicCraftingCubeModelProvider(DynamicCraftingUnitType.ACCELERATOR_64x)));

            BuiltInModelHooks.addBuiltInModel(
                    new ResourceLocation(AppEng.MOD_ID, "block/crafting/256x_accelerator_formed"),
                    new CraftingCubeModel(new DynamicCraftingCubeModelProvider(
                            DynamicCraftingUnitType.ACCELERATOR_256x)));
        }
    }

    public void init(IEventBus bus);
}
