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

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import stone.mae2.MAE2;
import stone.mae2.util.TransHelper;

@EventBusSubscriber(modid = MAE2.MODID, bus = EventBusSubscriber.Bus.MOD)
public record MAE2Config(Parts parts) {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // parts
    private static final ForgeConfigSpec.BooleanValue EU_P2P = BUILDER
        .push("Parts")
        .comment("Whether the EU P2P is enabled")
        .worldRestart()
        .translation(TransHelper.CONFIG.toKey("euP2P"))
        .define("euP2P", true);

    public static final IConfigSpec<?> SPEC = BUILDER.build();
    
    @SubscribeEvent
        static void onReload(final ModConfigEvent.Reloading event) {
        onReload();
    }

    // load in things that don't require a restart here (ie client only things or behavioral things)
    public static void onReload() {
        // this record thing sounded neat but idk now
        // just make the entire config anew here, this a once in a while thing really
        MAE2.CONFIG = new MAE2Config(new Parts(EU_P2P.get()));
    }

    // load in things that require a restart here (ie item registation or p2p attunements)
    public static void onLoad() {
        onReload();
    }

    @SubscribeEvent
    static void onload(final ModConfigEvent.Loading event) {
        onLoad();
    }

    public record Parts(boolean isEUP2PEnabled) {}
}
