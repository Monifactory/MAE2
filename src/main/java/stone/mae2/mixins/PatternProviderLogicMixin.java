/*
 * Copyright (C) 2024-2025 AE2 Enthusiast
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
package stone.mae2.mixins;

import appeng.api.networking.IManagedGridNode;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import stone.mae2.parts.p2p.PatternP2PTunnelLogic;

@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderLogicMixin extends PatternProviderLogic {
  public PatternProviderLogicMixin(IManagedGridNode mainNode,
    PatternProviderLogicHost host) {
    super(mainNode, host);
  }

  @Inject(method = "pushPattern", at = @At("rearrangeRoundRobin"))
  public void onPushProcessing() {
    PatternP2PTunnelLogic.isBlocking = this.isBlocking();
  }
}
