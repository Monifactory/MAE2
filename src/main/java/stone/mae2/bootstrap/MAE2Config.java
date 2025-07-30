/*
 * Copyright (C) 2024-2025 AE2 Enthusiast
 *
 * This file is part of MAE2.
 *
 * MAE2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * MAE2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */
package stone.mae2.bootstrap;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import stone.mae2.MAE2;
import stone.mae2.bootstrap.MAE2Config.TickRates.TickRate;
import stone.mae2.util.TransHelper;

@EventBusSubscriber(modid = MAE2.MODID, bus = EventBusSubscriber.Bus.MOD)
public record MAE2Config(Parts parts) {

  @SubscribeEvent
  static void onReload(final ModConfigEvent.Reloading event) {
    onReload();
  }

  // load in things that don't require a restart here (ie client only things or
  // behavioral things)
  public static void onReload() {
    // this record thing sounded neat but idk now
    // just make the entire config anew here, this a once in a while thing
    // really
    MAE2.CONFIG = new MAE2Config(
      new Parts(EU_P2P.get(), EU_P2P_NERF.get(), EU_P2P_NERF_FACTOR.get(),
        new TickRates(new TickRate(FE_MIN_RATE.get(), FE_MAX_RATE.get()),
          new TickRate(EU_MIN_RATE.get(), EU_MAX_RATE.get()),
          new TickRate(PATTERN_MIN_RATE.get(), PATTERN_MAX_RATE.get()))));
  }

  // load in things that require a restart here (ie item registation or p2p
  // attunements)
  public static void onLoad() {
    onReload();
  }

  @SubscribeEvent
  static void onload(final ModConfigEvent.Loading event) {
    onLoad();
  }

  public record Parts(boolean isEUP2PEnabled, boolean isEUP2PNerfed,
    double EUP2PNerfFactor, TickRates rates) {}

  public record TickRates(TickRate FEMultiP2PTunnel, TickRate EUMultiP2PTunnel,
    TickRate PatternP2PTunnel) {
    public record TickRate(int minRate, int maxRate) {}
  }

  // parts
  private static final ForgeConfigSpec.BooleanValue EU_P2P;
  private static final ForgeConfigSpec.BooleanValue EU_P2P_NERF;
  private static final ForgeConfigSpec.DoubleValue EU_P2P_NERF_FACTOR;

  // tick rates
  private static final ForgeConfigSpec.IntValue FE_MIN_RATE;
  private static final ForgeConfigSpec.IntValue FE_MAX_RATE;

  private static final ForgeConfigSpec.IntValue EU_MIN_RATE;
  private static final ForgeConfigSpec.IntValue EU_MAX_RATE;

  private static final ForgeConfigSpec.IntValue PATTERN_MIN_RATE;
  private static final ForgeConfigSpec.IntValue PATTERN_MAX_RATE;

  public static final IConfigSpec<?> SPEC;

  static {
    ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

    // parts
    builder.push("Parts");
    EU_P2P = builder
      .comment("Whether the EU P2P is enabled, !!Requires Game Restart!!")
      .worldRestart()
      .translation(TransHelper.CONFIG.toKey("euP2P"))
      .define("euP2P", true);

    EU_P2P_NERF = builder
      .comment(
        "Enable/Disable nerf to EU p2p. Nerf penalizes higher energy transfer rates across the entire ME network with higher taxes, but in such a way that stepping up voltage reduces tax. Also prevents EU p2ps from getting channels though an ME p2p")
      .translation(TransHelper.CONFIG.toKey("euP2PNerf"))
      .define("euP2PNerf", false);

    EU_P2P_NERF_FACTOR = builder
      .comment(
        "A factor used in calculating the nerfed EU p2p's tax. Higher means the tax is higher. Linearly affects tax")
      .worldRestart()
      .translation(TransHelper.CONFIG.toKey("euP2PNerfFactor"))
      .defineInRange("euP2PNerfFactor", 0.05, 0, Double.MAX_VALUE);

    // tick rates
    builder.push("Tick Rates");
    FE_MIN_RATE = builder
      .comment("Min tick rate for FE Multi P2P Tunnels")
      .translation(TransHelper.CONFIG.toKey("feMinRate"))
      .defineInRange("feMinRate", 1, 1, Integer.MAX_VALUE);
    FE_MAX_RATE = builder
      .comment("Max tick rate for FE Multi P2P Tunnels")
      .translation(TransHelper.CONFIG.toKey("feMaxRate"))
      .defineInRange("feMaxRate", 1, 1, Integer.MAX_VALUE);

    EU_MIN_RATE = builder
      .comment("Min tick rate for EU Multi P2P Tunnels")
      .translation(TransHelper.CONFIG.toKey("euMinRate"))
      .defineInRange("euMinRate", 1, 1, Integer.MAX_VALUE);
    EU_MAX_RATE = builder
      .comment("Max tick rate for EU Multi P2P Tunnels")
      .translation(TransHelper.CONFIG.toKey("euMaxRate"))
      .defineInRange("euMaxRate", 1, 1, Integer.MAX_VALUE);

    PATTERN_MIN_RATE = builder
      .comment("Minimum tick rate for Pattern (Multi) P2P Tunnels")
      .translation(TransHelper.CONFIG.toKey("patternMinRate"))
      .defineInRange("patternMinRate", 5, 1, Integer.MAX_VALUE);
    PATTERN_MAX_RATE = builder
      .comment("Max tick rate for Pattern (Multi) P2P Tunnels")
      .translation(TransHelper.CONFIG.toKey("patternMaxRate"))
      .defineInRange("patternMaxRate", 120, 1, Integer.MAX_VALUE);
    builder.pop();
    builder.pop();

    SPEC = builder.build();
  }
}
