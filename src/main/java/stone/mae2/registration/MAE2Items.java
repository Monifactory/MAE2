package stone.mae2.registration;

import appeng.api.features.P2PTunnelAttunement;
import appeng.api.parts.PartModels;
import appeng.block.AEBaseBlockItem;
import appeng.items.parts.PartItem;
import appeng.items.parts.PartModelsHelper;
import appeng.items.storage.BasicStorageCell;
import net.minecraft.Util;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.MAE2;
import stone.mae2.items.DynamicStorageComponentItem;
import stone.mae2.parts.PatternP2PTunnelPart;

import java.awt.Color;

public abstract class MAE2Items {
    static final DeferredRegister<Item> ITEMS = DeferredRegister
        .create(ForgeRegistries.ITEMS, MAE2.MODID);

    public static RegistryObject<PartItem<PatternP2PTunnelPart>> PATTERN_P2P_TUNNEL;
    public static RegistryObject<DynamicStorageComponentItem>[][] STORAGE_COMPONENTS;
    public static RegistryObject<BasicStorageCell>[][] STORAGE_CELLS;

    public static RegistryObject<AEBaseBlockItem>[] EXTRA_CRAFTING_COPROCESSORS;

    public static void init(IEventBus bus) {
        register();
        ITEMS.register(bus);


        bus.addListener((FMLCommonSetupEvent event) ->
        {
            if (MAE2Config.isInterfaceP2PEnabled)
            {
                P2PTunnelAttunement
                    .registerAttunementTag(PATTERN_P2P_TUNNEL.get());
            }
        });


    }

    @SuppressWarnings("unchecked")
    public static void register() {
        // always registers pattern p2p for the creative tab's icon
        // TODO figure something out for that
        PATTERN_P2P_TUNNEL = Util.make(() ->
        {
            PartModels.registerModels(
                PartModelsHelper.createModels(PatternP2PTunnelPart.class));
            return ITEMS.register("pattern_p2p_tunnel",
                () -> new PartItem<>(new Item.Properties(),
                    PatternP2PTunnelPart.class, PatternP2PTunnelPart::new));
        });
        
        if (MAE2Config.areExtraTiersEnabled)
        {
            STORAGE_COMPONENTS = new RegistryObject[MAE2Config.extraStorageTiers][5];
            STORAGE_CELLS = new RegistryObject[MAE2Config.extraStorageTiers][5];

            for (int i = 0; i < MAE2Config.extraStorageTiers; i++)
            {
                for (int j = 0; j < 5; j++) {
                    final int tier = i + 1;
                    final int subTier = j;
                    STORAGE_COMPONENTS[i][j] = ITEMS
                        .register("storage_component" + tier + j,
                            () -> new DynamicStorageComponentItem(new Item.Properties(),
                                                                  tier, subTier));
                }
            }

            DynamicStorageComponentItem.colorSupplier = (prefix) ->
            Color.HSBtoRGB(prefix / MAE2Config.extraStorageTiers, 1, 1);
        }



    }


}
