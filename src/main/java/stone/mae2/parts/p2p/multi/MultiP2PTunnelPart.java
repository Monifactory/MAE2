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
package stone.mae2.parts.p2p.multi;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnits;
import appeng.api.features.P2PTunnelAttunement;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.networking.GridFlags;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.util.AECableType;
import appeng.client.render.cablebus.P2PTunnelFrequencyModelData;
import appeng.core.AEConfig;
import appeng.parts.AEBasePart;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import stone.mae2.api.features.MultiP2PTunnelAttunement;
import stone.mae2.me.service.MultiP2PService;

import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

public abstract class MultiP2PTunnelPart<T extends MultiP2PTunnelPart<T>> extends AEBasePart {
    private static final String CONFIG_NBT_TYPE = "p2pType";
    private static final String CONFIG_NBT_FREQ = "p2pFreq";

    private boolean output;
    private short freq;

    public MultiP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem);
        this.getMainNode().setIdlePowerUsage(this.getPowerDrainPerTick());
        this.getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    protected float getPowerDrainPerTick() {
        return 1.0f;
    }

    public List<T> getInputs() {
        return getInputStream().toList();
    }

    public Stream<T> getInputStream() {
        if (this.getMainNode().isOnline()) {
            var grid = getMainNode().getGrid();
            if (grid != null) {
                return grid.getService(MultiP2PService.class).getInputs(this.getFrequency(), this.getClass());
            }
        }
        return Stream.empty();
    }

    public List<T> getOutputs() {
        return getOutputStream().toList();
    }

    public Stream<T> getOutputStream() {
        if (this.getMainNode().isOnline()) {
            var grid = getMainNode().getGrid();
            if (grid != null) {
                return grid.getService(MultiP2PService.class).getOutputs(this.getFrequency(), this.getClass());
            }
        }
        return Stream.empty();
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(5, 5, 12, 11, 11, 13);
        bch.addBox(3, 3, 13, 13, 13, 14);
        bch.addBox(2, 2, 14, 14, 14, 16);
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        this.setOutput(data.getBoolean("output"));
        this.freq = data.getShort("freq");
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        data.putBoolean("output", this.isOutput());
        data.putShort("freq", this.getFrequency());
    }

    @Override
    public boolean readFromStream(FriendlyByteBuf data) {
        final boolean c = super.readFromStream(data);
        final short oldf = this.freq;
        this.freq = data.readShort();
        return c || oldf != this.freq;
    }

    @Override
    public void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeShort(this.getFrequency());
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1;
    }

    @Override
    public boolean useStandardMemoryCard() {
        return false;
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (isClientSide()) {
            return true;
        }

        if (hand == InteractionHand.OFF_HAND) {
            return false;
        }

        ItemStack is;
        boolean settingOutput;
        if (player.getMainHandItem().isEmpty()) {
            is = player.getOffhandItem();
            settingOutput = false;
        } else {
            is = player.getMainHandItem();
            settingOutput = true;
        }

        // Prefer restoring from memory card
        if (!is.isEmpty() && is.getItem() instanceof IMemoryCard mc) {
            var configData = mc.getData(is);

            // Change the actual tunnel type and import settings when the encoded type is a P2P
            IPartItem<?> partItem = IPartItem.byId(new ResourceLocation(configData.getString(CONFIG_NBT_TYPE)));
            if (partItem != null && MultiP2PTunnelPart.class.isAssignableFrom(partItem.getPartClass())) {
                IPart newBus = this;
                if (newBus.getPartItem() != partItem) {
                    newBus = this.getHost().replacePart(partItem, this.getSide(), player, hand);
                }
                if (newBus instanceof MultiP2PTunnelPart<?> newTunnel) {
                    newTunnel.importSettings(SettingsFrom.MEMORY_CARD, configData, player, settingOutput);
                }
                mc.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                return true;
            }
            mc.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
            return false;
        }

        // Attunement via held item replaces the tunnel part with the desired target part type
        if (P2PTunnelAttunement.getTunnelPartByTriggerItem(is)
                .getItem() instanceof IPartItem singleType) {
            IPartItem<? extends MultiP2PTunnelPart<?>> newType = MultiP2PTunnelAttunement.getMultiPartBySinglePart(singleType);
            if (newType != null && newType != getPartItem()) {
                boolean oldOutput = isOutput();
                short myFreq = getFrequency();

                // If we were able to replace the tunnel part, copy over frequency/output state
                MultiP2PTunnelPart<?> tunnel = getHost().replacePart(newType, getSide(), player, hand);
                //if (tunnel instanceof MultiP2PTunnelPart newTunnel) {
                    tunnel.setOutput(oldOutput);
                    tunnel.onTunnelNetworkChange();

                    tunnel.getMainNode().ifPresent(grid -> {
                        grid.getService(MultiP2PService.class).updateFreq(tunnel, myFreq);
                    });
                    //}

                Platform.notifyBlocksOfNeighbors(getLevel(), getBlockEntity().getBlockPos());
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onPartShiftActivate(Player player, InteractionHand hand, Vec3 pos) {
        final ItemStack is = player.getInventory().getSelected();
        if (!is.isEmpty() && is.getItem() instanceof IMemoryCard mc) {
            if (isClientSide()) {
                return true;
            }

            final CompoundTag data = mc.getData(is);
            final short storedFrequency = data.getShort("freq");

            short newFreq = this.getFrequency();
            final boolean wasOutput = this.isOutput();
            this.setOutput(false);

            final boolean needsNewFrequency = wasOutput || this.getFrequency() == 0 || storedFrequency == newFreq;

            var grid = getMainNode().getGrid();
            if (grid != null) {
                MultiP2PService p2p = grid.getService(MultiP2PService.class);
                if (needsNewFrequency) {
                    newFreq = p2p.newFrequency();
                }

                p2p.updateFreq(this, newFreq);
            }

            this.onTunnelConfigChange();

            var type = getPartItem().asItem().getDescriptionId();

            exportSettings(SettingsFrom.MEMORY_CARD, data);

            mc.setMemoryCardContents(is, type, data);
            if (needsNewFrequency) {
                mc.notifyUser(player, MemoryCardMessages.SETTINGS_RESET);
            } else {
                mc.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
            }
            return true;
        }
        return false;
    }

    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player, boolean settingOutput) {
        super.importSettings(mode, input, player);

        if (input.contains(CONFIG_NBT_FREQ, Tag.TAG_SHORT)) {
            short freq = input.getShort(CONFIG_NBT_FREQ);

            // Only make this an output, if it's not already on the frequency.
            // Otherwise, the tunnel input may be made unusable by accidentally loading it with its own settings
            if (freq != this.freq) {
                setOutput(settingOutput);
                var grid = getMainNode().getGrid();
                if (grid != null) {
                    grid.getService(MultiP2PService.class).updateFreq(this, freq);
                } else {
                    setFrequency(freq); // Remember it for when we actually join the grid
                    onTunnelNetworkChange();
                }

            }
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        super.exportSettings(mode, output);

        // Save the tunnel type
        if (mode == SettingsFrom.MEMORY_CARD) {
            output.putString(CONFIG_NBT_TYPE, IPartItem.getId(getPartItem()).toString());

            if (freq != 0) {
                output.putShort(CONFIG_NBT_FREQ, freq);

                var colors = Platform.p2p().toColors(freq);
                var colorCode = new int[] { colors[0].ordinal(), colors[0].ordinal(), colors[1].ordinal(),
                        colors[1].ordinal(), colors[2].ordinal(), colors[2].ordinal(), colors[3].ordinal(),
                        colors[3].ordinal(), };
                output.putIntArray(IMemoryCard.NBT_COLOR_CODE, colorCode);
            }
        }
    }

    public void onTunnelConfigChange() {
    }

    public void onTunnelNetworkChange() {

    }

    protected void queueTunnelDrain(PowerUnits unit, double f) {
        final double ae_to_tax = unit.convertTo(PowerUnits.AE, f * AEConfig.TUNNEL_POWER_LOSS);

        getMainNode().ifPresent(grid -> {
            grid.getEnergyService().extractAEPower(ae_to_tax, Actionable.MODULATE, PowerMultiplier.ONE);
        });
    }

    public short getFrequency() {
        return this.freq;
    }

    public void setFrequency(short freq) {
        final short oldf = this.freq;
        this.freq = freq;
        if (oldf != this.freq) {
            this.getHost().markForSave();
            this.getHost().markForUpdate();
        }
    }

    public boolean isOutput() {
        return this.output;
    }

    void setOutput(boolean output) {
        this.output = output;
        this.getHost().markForSave();
    }

    @Override
    public ModelData getModelData() {
        long ret = Short.toUnsignedLong(this.getFrequency());

        if (this.isActive() && this.isPowered()) {
            ret |= 0x10000L;
        }

        return ModelData.builder()
                .with(P2PTunnelFrequencyModelData.FREQUENCY, ret)
                .build();
    }
}
