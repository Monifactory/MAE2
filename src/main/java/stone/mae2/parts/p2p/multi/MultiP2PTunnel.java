package stone.mae2.parts.p2p.multi;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnits;
import appeng.api.features.P2PTunnelAttunement;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEKeyType;
import appeng.api.util.AECableType;
import appeng.client.render.cablebus.P2PTunnelFrequencyModelData;
import appeng.core.AEConfig;
import appeng.parts.AEBasePart;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import stone.mae2.api.features.MultiP2PTunnelAttunement;
import stone.mae2.me.service.MultiP2PService;

import java.util.Set;

/*
 * I'm deeply sorry for anyone who has to read and understand this code. I can't
 * think of a better way to do this (though I'm sure I'll think of something
 * perfect once I'm done). On the bright it's actually pretty nice once you get
 * to the final concrete class implementations, so just don't think about this
 * too hard and you'll be fine.
 */
public abstract class MultiP2PTunnel<T extends MultiP2PTunnel<T, L, P>, L extends MultiP2PTunnel<T, L, P>.Logic, P extends MultiP2PTunnel.Part<T, L, P>> {
  public abstract L createLogic(P part);

  /**
   * Save any data associated with the part to the given tag
   * 
   * @param part
   * @return data to be associated with the part, null if nothing
   */
  public CompoundTag saveNodeData(P part) {
    if (this.customName != null) {
      CompoundTag data = new CompoundTag();
      data
        .putString("customName", Component.Serializer.toJson(this.customName));
      return data;
    } else
      return null;
  }

  protected final ReferenceSet<L> inputs = new ReferenceOpenHashSet<>();
  protected final ReferenceSet<L> outputs = new ReferenceOpenHashSet<>();

  private final short freq;
  private final IGrid grid;

  private Component customName;

  public MultiP2PTunnel(short freq, IGrid grid) {
    this.freq = freq;
    this.grid = grid;
  }

  /**
   * Adds a {@link MultiP2PTunnelPart} to this {@link MultiP2PTunnel},
   * input/ouput is automatically decided
   * 
   * @param tag
   *
   * @return logic associated with this part
   */
  public L addTunnel(P part) {
    L logic = this.createLogic(part);
    if (part.isOutput()) {
      outputs.add(logic);
    } else {
      if (part.hasCustomName()) { this.customName = part.getCustomName(); }
      inputs.add(this.createLogic(part));
    }
    this.updateTunnels(part.isOutput(), false);
    return logic;
  }

  public L addTunnel(P part, Tag savedData) { return addTunnel(part); }

  /**
   * Removes a {@link MultiP2PTunnelPart} from this {@link MultiP2PTunnel},
   * input/output is automatically decided
   *
   * @return if the part was already in the tunnel
   */
  public boolean removeTunnel(P part) {
    if (part.isOutput()) {
      this.updateTunnels(true, false);
      return outputs.remove(part.getLogic());
    } else {
      this.updateTunnels(false, false);
      return inputs.remove(part.getLogic());
    }
  }

  public Set<L> getInputs() { return this.inputs; }

  public Set<L> getOutputs() { return this.outputs; }

  public boolean hasCustomName() { return this.customName != null; }

  public Component getCustomName() { return this.customName; }

  protected void updateTunnels(boolean updateOutputs, boolean configChange) {
    if (updateOutputs) {
      for (L logic : this.outputs) {
        if (configChange) { logic.onTunnelConfigChange(); }
        logic.onTunnelNetworkChange();
      }
    } else {
      for (L logic : this.inputs) {
        if (configChange) { logic.onTunnelConfigChange(); }
        logic.onTunnelNetworkChange();
      }
    }
  }

  public void updateTunnel(L part, boolean settingOutput, short freq) {
    if (this.freq == freq) {
      if (settingOutput) {
        this.outputs.add(part);
        this.inputs.remove(part);
      } else {
        this.outputs.remove(part);
        this.inputs.add(part);
      }
    } else {
      this.outputs.remove(part);
      this.inputs.remove(part);
    }
  }

  protected void deductEnergyCost(double energyTransported,
    PowerUnits typeTransported) {
    var costFactor = AEConfig.instance().getP2PTunnelEnergyTax();
    if (costFactor <= 0) { return; }

    var tax = typeTransported
      .convertTo(PowerUnits.AE, energyTransported * costFactor);
    this.grid
      .getEnergyService()
      .extractAEPower(tax, Actionable.MODULATE, PowerMultiplier.CONFIG);
  }

  protected void deductTransportCost(long amountTransported,
    AEKeyType typeTransported) {
    var costFactor = AEConfig.instance().getP2PTunnelTransportTax();
    if (costFactor <= 0) { return; }

    double operations = amountTransported
      / (double) typeTransported.getAmountPerOperation();
    double tax = operations * costFactor;
    this.grid
      .getEnergyService()
      .extractAEPower(tax, Actionable.MODULATE, PowerMultiplier.CONFIG);
  }

  public abstract class Logic {
    protected final P part;

    public Logic(P part) { this.part = part; }

    protected void onTunnelConfigChange() {}

    protected void onTunnelNetworkChange() {}
  }

  public static abstract class Part<T extends MultiP2PTunnel<T, L, P>, L extends MultiP2PTunnel<T, L, P>.Logic, P extends Part<T, L, P>>
    extends AEBasePart {
    private static final String CONFIG_NBT_TYPE = "p2pType";
    private static final String CONFIG_NBT_FREQ = "p2pFreq";

    private boolean output;
    private short freq;
    protected L logic;

    public Part(IPartItem<?> partItem) {
      super(partItem);
      this.getMainNode().setIdlePowerUsage(this.getPowerDrainPerTick());
      this.getMainNode().setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    abstract public T createTunnel(short freq);

    abstract public Class<T> getTunnelClass();

    public L getLogic() { return this.logic; }

    protected final L setLogic(L logic) { return this.logic = logic; }

    protected float getPowerDrainPerTick() { return 1.0f; }

    public T getTunnel() {
      if (this.getMainNode().isOnline()) {
        var grid = getMainNode().getGrid();
        if (grid != null) {
          return (T) grid.getService(MultiP2PService.class).getTunnel(this);
        }
      }

      return null;
    }

    public void setCustomName(Component name) {

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
    public float getCableConnectionLength(AECableType cable) { return 1; }

    @Override
    public boolean useStandardMemoryCard() { return false; }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand,
      Vec3 pos) {
      if (isClientSide()) { return true; }

      if (hand == InteractionHand.OFF_HAND) { return false; }

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

        // Change the actual tunnel type and import settings when the encoded
        // type is a
        // P2P
        IPartItem<?> partItem = IPartItem
          .byId(new ResourceLocation(configData.getString(CONFIG_NBT_TYPE)));
        if (partItem != null && MultiP2PTunnel.Part.class
          .isAssignableFrom(partItem.getPartClass())) {
          IPart newBus = this;
          if (newBus.getPartItem() != partItem) {
            newBus = this
              .getHost()
              .replacePart(partItem, this.getSide(), player, hand);
          }
          if (newBus instanceof Part<?, ?, ?> newTunnel) {
            newTunnel
              .importSettings(SettingsFrom.MEMORY_CARD, configData, player,
                settingOutput);
          }
          mc.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
          return true;
        }
        mc.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
        return false;
      } else
      // Attunement via held item replaces the tunnel part with the desired
      // target
      // part type
      if (P2PTunnelAttunement
        .getTunnelPartByTriggerItem(is)
        .getItem() instanceof IPartItem singleType) {
        IPartItem<? extends Part<?, ?, ?>> newType = MultiP2PTunnelAttunement
          .getMultiPartBySinglePart(singleType);
        if (newType != null && newType != getPartItem()) {
          boolean oldOutput = isOutput();
          short myFreq = getFrequency();

          // If we were able to replace the tunnel part, copy over
          // frequency/output state
          Part<?, ?, ?> part = getHost()
            .replacePart(newType, getSide(), player, hand);
          part.setFrequency(myFreq);
          part.setOutput(oldOutput);
          this.getTunnel().removeTunnel((P) this);
          // setting the freq/output automagically sets up the tunnel
          Platform
            .notifyBlocksOfNeighbors(getLevel(),
              getBlockEntity().getBlockPos());
          return true;
        }
      }

      return false;
    }

    @Override
    public boolean onPartShiftActivate(Player player, InteractionHand hand,
      Vec3 pos) {
      final ItemStack is = player.getInventory().getSelected();
      if (!is.isEmpty() && is.getItem() instanceof IMemoryCard mc) {
        if (isClientSide()) { return true; }

        final CompoundTag data = mc.getData(is);
        final short storedFrequency = data.getShort("freq");

        short newFreq = this.getFrequency();
        final boolean wasOutput = this.isOutput();
        this.setOutput(false);

        final boolean needsNewFrequency = wasOutput || this.getFrequency() == 0
          || storedFrequency == newFreq;

        var grid = getMainNode().getGrid();
        if (grid != null) {
          MultiP2PService p2p = grid.getService(MultiP2PService.class);
          if (needsNewFrequency) {
            newFreq = p2p.newFrequency(this.getTunnelClass());
            this.setFrequency(newFreq);
          }
        }

        this.logic.onTunnelConfigChange();

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

    public void importSettings(SettingsFrom mode, CompoundTag input,
      @Nullable Player player, boolean settingOutput) {
      super.importSettings(mode, input, player);

      if (input.contains(CONFIG_NBT_FREQ, Tag.TAG_SHORT)) {
        short freq = input.getShort(CONFIG_NBT_FREQ);

        // Only make this an output, if it's not already on the frequency.
        // Otherwise, the tunnel input may be made unusable by accidentally
        // loading it
        // with its own settings
        if (freq != this.freq || !settingOutput) {
          setOutput(settingOutput);
          setFrequency(freq);
          this.logic.onTunnelNetworkChange();
        }
      }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
      super.exportSettings(mode, output);

      // Save the tunnel type
      if (mode == SettingsFrom.MEMORY_CARD) {
        output
          .putString(CONFIG_NBT_TYPE,
            IPartItem.getId(getPartItem()).toString());

        if (freq != 0) {
          output.putShort(CONFIG_NBT_FREQ, freq);

          var colors = Platform.p2p().toColors(freq);
          var colorCode = new int[] { colors[0].ordinal(), colors[0].ordinal(),
            colors[1].ordinal(), colors[1].ordinal(), colors[2].ordinal(),
            colors[2].ordinal(), colors[3].ordinal(), colors[3].ordinal(), };
          output.putIntArray(IMemoryCard.NBT_COLOR_CODE, colorCode);
        }
      }
    }

    public short getFrequency() { return this.freq; }

    public void setFrequency(short freq) {
      if (this.freq != freq) {
        var tunnel = this.getTunnel();
        if (tunnel != null) // if we're not connected to a grid, there's no
                            // tunnel
          tunnel.removeTunnel((P) this);
        this.freq = freq;
        tunnel = this.getTunnel();
        if (tunnel != null)
          tunnel.addTunnel((P) this);
        this.getHost().markForSave();
        this.getHost().markForUpdate();
      }
    }

    public boolean isOutput() { return this.output; }

    void setOutput(boolean output) {
      if (this.output != output) {
        T tunnel = this.getTunnel();
        if (tunnel != null)
          tunnel.removeTunnel((P) this);
        this.output = output;
        tunnel = this.getTunnel();
        if (tunnel != null)
          tunnel.addTunnel((P) this);
        this.getHost().markForSave();
      }
    }

    @Override
    public ModelData getModelData() {
      long ret = Short.toUnsignedLong(this.getFrequency());

      if (this.isActive() && this.isPowered()) { ret |= 0x10000L; }

      return ModelData
        .builder()
        .with(P2PTunnelFrequencyModelData.FREQUENCY, ret)
        .build();
    }
  }
}
