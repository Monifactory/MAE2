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

import appeng.api.config.Actionable;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderReturnInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import stone.mae2.MAE2;
import stone.mae2.parts.p2p.PatternP2PPartLogic.PatternP2PPartLogicHost;
import stone.mae2.parts.p2p.PatternP2PTunnelLogic;

import java.util.List;
import java.util.Set;

@Mixin(value = PatternProviderLogic.class, remap = false)
public abstract class PatternProviderLogicMixin {
  @Shadow
  @Final
  private PatternProviderLogicHost host;
  @Shadow
  @Final
  private IManagedGridNode mainNode;
  @Shadow
  private Direction sendDirection;
  @Shadow
  @Final
  private List<GenericStack> sendList;
  @Shadow
  @Final
  private PatternProviderReturnInventory returnInv;
  @Shadow
  @Final
  private IActionSource actionSource;

  @Shadow
  public abstract boolean isBlocking();

  @Shadow
  private Set<Direction> getActiveSides() { return null; }

  @Inject(method = "pushPattern", at = @At("HEAD"))
  public void onPushStart(CallbackInfoReturnable<Boolean> cir) {
    PatternP2PTunnelLogic.isBlocking = this.isBlocking();
  }

  // reset blocking mode in-case there's other providers that work with p2ps,
  // but weren't mixed into
  @Inject(method = "pushPattern", at = @At("TAIL"))
  public void onPushEnd(CallbackInfoReturnable<Boolean> cir) {
    PatternP2PTunnelLogic.isBlocking = false;
  }

  @Inject(method = "updatePatterns", at = @At("TAIL"))
  public void onPatternUpdate(CallbackInfo ci) {
    BlockEntity be = host.getBlockEntity();
    Level level = be.getLevel();

    for (var direction : this.getActiveSides()) {
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

  BlockPos sendPos;

  /**
   * Reads the NBT data
   * 
   * Migrates pattern providers saved with MAE2 1.2. Migrates providers saved
   * under MAE2 [1.3,2.0)
   * 
   * @param tag
   * @param ci
   */
  @Inject(method = "readFromNBT", at = @At("TAIL"))
  private void onReadFromNBT(CompoundTag tag, CallbackInfo ci) {
    if (tag.contains("sendPos"))
    // send pos only exists if MAE2 existed before
    {
      Tag sendPosTag = tag.get("sendPos");

      if (sendPosTag instanceof NumericTag numericTag) {
        MAE2.LOGGER.debug("Detected a Pattern Provider from MAE2 [1.3,2.0)!");
        sendPos = BlockPos.of(numericTag.getAsLong());
      } else if (sendPosTag instanceof CompoundTag compoundTag) {
        MAE2.LOGGER.debug("Detected Pattern Provider from MAE2 1.2.0!");
        sendPos = BlockPos.of(compoundTag.getLong("mae2Pos"));
        this.sendDirection = Direction
          .from3DDataValue(compoundTag.getByte("mae2Direction"));
      } else {
        sendPos = null;
      }
    }
  }

  @Inject(method = "onMainNodeStateChanged", at = @At(value = "INVOKE", target = "ifPresent"))
  public void onMainNodeStateChanged(CallbackInfo ci) {
    if (sendPos != null) {
      MAE2.LOGGER.debug("Migrating the old Pattern Provider!");
      // attempt to pass the send list off to the p2p output
      BlockEntity maybeBE = host
        .getBlockEntity()
        .getLevel()
        .getBlockEntity(sendPos.relative(sendDirection.getOpposite()));

      if (maybeBE != null && maybeBE instanceof IPartHost host) {
        IPart maybeP2P = host.getPart(sendDirection);
        if (maybeP2P != null
          && maybeP2P instanceof PatternP2PPartLogicHost p2p) {
          for (GenericStack stack : sendList) {
            p2p.addToSendList(stack.what(), stack.amount());
          }
          sendList.clear();
        }
      }

      if (!sendList.isEmpty()) {
        MAE2.LOGGER
          .warn(
            "Couldn't perfectly migrate provider @ {}, attempting to return unsent stacks back to their network instead! (this will create split patterns, most likely breaking the attached machine)",
            host.getBlockEntity().getBlockPos());
        // attempt to return the send list back to the network instead
        for (GenericStack stack : sendList) {
          returnInv
            .insert(stack.what(), stack.amount(), Actionable.MODULATE,
              actionSource);
        }
      }
      // clear send list as last resort, items should've been passed off
      // before hand though
      this.sendPos = null;
      this.sendDirection = null;
      this.sendList.clear();
    }

  }
}