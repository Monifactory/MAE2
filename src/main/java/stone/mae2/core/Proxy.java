package stone.mae2.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import stone.mae2.MAE2;

public interface Proxy {
    public class Server implements Proxy {
        public void init(IEventBus bus) {
            MAE2Blocks.init(bus);
            MAE2Items.init(bus);
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
                    .displayItems((params, output) ->
                    {
                        for (var entry : MAE2Items.ITEMS.getEntries())
                        {
                            output.accept(entry.get());
                        }
                    }).build());
        }
    }

    public void init(IEventBus bus);
}
