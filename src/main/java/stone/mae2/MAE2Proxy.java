package stone.mae2;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import stone.mae2.items.DynamicStorageComponentItem;

import java.util.stream.Stream;

public interface MAE2Proxy {
    public class Client implements MAE2Proxy {

        @Override
        public void init(IEventBus bus) {
            // the server always exists for clients, right?
            new MAE2Proxy.Server().init(bus);

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

            MAE2Items.CREATIVE_TAB = MAE2Items.TABS.register("main",
                () -> CreativeModeTab.builder()
                    .title(Component
                        .translatable("gui." + MAE2.MODID + ".creative_tab"))
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
