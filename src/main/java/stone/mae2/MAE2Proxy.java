package stone.mae2;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import stone.mae2.items.DynamicStorageComponentItem;
import stone.mae2.util.TranslationHelper;

import java.util.stream.Stream;

public interface MAE2Proxy {
    public class Client implements MAE2Proxy {

        static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister
        .create(Registries.CREATIVE_MODE_TAB, MAE2.MODID);
        static RegistryObject<CreativeModeTab> CREATIVE_TAB;

        @Override
        public void init(IEventBus bus) {
            // the server always exists for clients, right?
            new MAE2Proxy.Server().init(bus);
            TABS.register(bus);

            if (MAE2Config.areExtraTiersEnabled)
            {
                bus.addListener((RegisterColorHandlersEvent.Item event) ->
                {
                    event.register((ItemStack stack, int tint) ->
                    {
                        return ((DynamicStorageComponentItem) stack.getItem())
                            .getColor();
                    }, (ItemLike[]) Stream.of(MAE2Items.STORAGE_COMPONENTS)
                        .flatMap(tiers -> Stream.of(tiers))
                        .<ItemLike>map(component -> component.get()).toArray());
                });
            }

            MAE2Proxy.Client.CREATIVE_TAB = MAE2Proxy.Client.TABS.register("main",
                () -> CreativeModeTab.builder()
                    .title(Component
                           .translatable(TranslationHelper.GUI.toKey("creativeTab")))
                    .icon(
                        () -> new ItemStack(MAE2Items.PATTERN_P2P_TUNNEL.get()))
                    .displayItems((params, output) ->
                    {
                        for (var entry : MAE2Items.ITEMS.getEntries())
                        {
                            output.accept(entry.get());
                        }
                    }).build());
        }

    }

    public class Server implements MAE2Proxy {

        @Override
        public void init(IEventBus bus) {
            MAE2Items.init(bus);
        }

    }

    public void init(IEventBus bus);
}
