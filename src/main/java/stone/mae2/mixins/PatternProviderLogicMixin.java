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

import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import stone.mae2.parts.p2p.PatternP2PTunnelLogic;

import java.util.Set;

@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderLogicMixin {

  @Inject(method = "pushPattern(LIPatternDetails;[LKeyCounter;)Z", at = @At("HEAD"))
  public void onPush(CallbackInfoReturnable<Boolean> cir) {
    PatternP2PTunnelLogic.isBlocking = this.isBlocking();
  }

  @Inject(method = "updatePatterns", at = @At("TAIL"))
  public void onPatternUpdate(CallbackInfo ci) {
    BlockEntity be = host.getBlockEntity();
    Level level = be.getLevel();

    for (Direction direction : this.getActiveSides()) {
      var adjPos = be.getBlockPos().relative(direction);
      var adjBe = level.getBlockEntity(adjPos);
      var adjBeSide = direction.getOpposite();

      var craftingMachine = ICraftingMachine
        .of(level, adjPos, adjBeSide, adjBe);
      if (craftingMachine instanceof PatternP2PTunnelLogic p2pLogic) {
        p2pLogic.refreshInputs();
      }
    }
  }

  @Shadow
  @Final
  private PatternProviderLogicHost host;

  @Shadow
  public abstract boolean isBlocking();

  @Shadow
  private Set<Direction> getActiveSides() { return null; }
}
