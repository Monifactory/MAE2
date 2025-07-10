/*
 * Copyright (C) 2024 AE2 Enthusiast
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
package stone.mae2.integration;

import appeng.api.integrations.igtooltip.TooltipBuilder;
import appeng.api.integrations.igtooltip.TooltipContext;
import appeng.api.integrations.igtooltip.providers.BodyProvider;
import appeng.api.integrations.igtooltip.providers.ServerDataProvider;
import appeng.core.localization.InGameTooltip;
import appeng.util.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import stone.mae2.parts.p2p.multi.MultiP2PTunnel;
import stone.mae2.util.TransHelper;

/**
 * Provides information about a P2P tunnel to WAILA.
 */
@SuppressWarnings("rawtypes")
public final class MultiP2PStateDataProvider implements
  BodyProvider<MultiP2PTunnel.Part>, ServerDataProvider<MultiP2PTunnel.Part> {
  private static final byte STATE_UNLINKED = 0;
  private static final byte STATE_OUTPUT = 1;
  private static final byte STATE_INPUT = 2;
  public static final String TAG_P2P_STATE = "p2pState";
  public static final String TAG_P2P_LINKS = "p2pLinks";
  public static final String TAG_P2P_FREQUENCY = "p2pFrequency";
  public static final String TAG_P2P_FREQUENCY_NAME = "p2pFrequencyName";

  @Override
  public void buildTooltip(MultiP2PTunnel.Part object, TooltipContext context,
    TooltipBuilder tooltip) {
    var serverData = context.serverData();

    if (serverData.contains(TAG_P2P_STATE, Tag.TAG_BYTE)) {
      var state = serverData.getByte(TAG_P2P_STATE);
      var links = serverData.getInt(TAG_P2P_LINKS);

      switch (state) {
      case STATE_UNLINKED:
        tooltip.addLine(InGameTooltip.P2PUnlinked.text());
        break;
      case STATE_OUTPUT:
      case STATE_INPUT:
        tooltip.addLine(getLinkText(state == STATE_INPUT, links));
      }

      var freq = serverData.getShort(TAG_P2P_FREQUENCY);

      // Show the frequency and name of the frequency if it exists
      var freqTooltip = Platform.p2p().toHexString(freq);
      if (serverData.contains(TAG_P2P_FREQUENCY_NAME, Tag.TAG_STRING)) {
        var freqName = serverData.getString(TAG_P2P_FREQUENCY_NAME);
        freqTooltip = freqName + " (" + freqTooltip + ")";
      }

      tooltip.addLine(InGameTooltip.P2PFrequency.text(freqTooltip));
    }
  }

  @Override
  public void provideServerData(Player player, MultiP2PTunnel.Part part,
    CompoundTag serverData) {
    if (!part.isPowered()) { return; }

    // Frequency
    serverData.putShort(TAG_P2P_FREQUENCY, part.getFrequency());

    // The default state
    byte state = STATE_UNLINKED;
    MultiP2PTunnel tunnel = part.getTunnel();
    if (tunnel != null) {
      if (!part.isOutput()) {
        var outputCount = tunnel.getOutputs().size();
        if (outputCount > 0) {
          // Only set it to INPUT if we know there are any outputs
          state = STATE_INPUT;
          serverData.putInt(TAG_P2P_LINKS, outputCount);
        }
      } else {
        var inputCount = tunnel.getInputs().size();
        if (inputCount > 0) {
          state = STATE_OUTPUT;
          serverData.putInt(TAG_P2P_LINKS, inputCount);
          // TODO naming frequencies?? maybe as part of a better memory card
        }
      }
      if (tunnel.getCustomName() != null) {
        serverData
          .putString(TAG_P2P_FREQUENCY_NAME,
            tunnel.getCustomName().getString());
      }
    }

    serverData.putByte(TAG_P2P_STATE, state);
  }

  private static Component getLinkText(boolean isInput, int links) {
    if (isInput) {
      if (links > 1)
        return InGameTooltip.P2PInputManyOutputs.text(links);
      else
        return InGameTooltip.P2PInputOneOutput.text();
    } else {
      if (links > 1)
        // Why is i18n so difficult? I hate this, but I don't know how to get it
        // to work off a lang file
        return Component
          .translatableWithFallback(
            TransHelper.WAILA.toKey("P2POutputManyInputs"),
            "Linked (Output Side) - %s Inputs", links);
      else
        return InGameTooltip.P2POutput.text();

    }
  }

}
