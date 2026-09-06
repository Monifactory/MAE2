package stone.mae2.item.faulty;

import org.joml.Matrix4f;

import com.lowdragmc.lowdraglib.utils.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.api.util.AEColor;
import appeng.block.AEBaseEntityBlock;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import stone.mae2.MAE2;
import stone.mae2.bootstrap.MAE2Config;
import stone.mae2.bootstrap.MAE2Items;
import stone.mae2.util.TransHelper;

public class UberMode extends FaultyCardMode {
  private static final String START_POS = "start";
  private BlockPos start;
	@Override
	public ResourceLocation getType() {
    return MAE2.toKey("uber_mode");
	}
    
  @Override
  public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
    if (this.start == null) {
      this.start = context.getClickedPos();
      this.save(stack.getOrCreateTag());
      context.getPlayer().displayClientMessage(Component.translatable(TransHelper.GUI.toKey("faulty", "uber", "start")), true);
      return InteractionResult.CONSUME;
    } else {
      BlockPos end = context.getClickedPos();
      Level level = context.getLevel();
      BlockEntity be = level.getBlockEntity(context.getClickedPos());
      Direction side = context.getClickedFace();
      Player player = context.getPlayer();
      if (be instanceof IPartHost originalHost) {
        SelectedPart selectedPart = originalHost.selectPartWorld(context.getClickLocation());
        side = selectedPart.side;
      }
      if (!isVolumeValid(end)) {
        player.displayClientMessage(TransHelper.GUI.translatable("faulty.uber.failed"), true);
      } else {
        boolean failed = false;
        InteractionHand hand = context.getHand();
        Vec3 clickLocation = context.getClickLocation();
        for (BlockPos pos : BlockPos.betweenClosed(start, end)) {
          BlockState state = level.getBlockState(pos);
          if (state.getBlock() instanceof AEBaseEntityBlock baseBlock) {
            baseBlock.use(state, level, pos, player, hand, new BlockHitResult(clickLocation, side, pos, false));
          } else {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IPartHost aoePartHost) {
              IPart part = aoePartHost.getPart(side);
              if (part != null) {
                // no idea what the Vec pos argument does here, doesn't seem used in any implementation
                failed |= !part.onActivate(player, hand, clickLocation);
              }
            }
          }
        }

        if (failed) {
          context.getPlayer().displayClientMessage(TransHelper.GUI.translatable("faulty.multi.failed"), true);
        } else {
          context.getPlayer().displayClientMessage(TransHelper.GUI.translatable("faulty.multi.succeeded"), true);
        }
      }
      this.start = null;
      this.save(stack.getOrCreateTag());
      return InteractionResult.CONSUME;
    }
  }

  @Override
  public InteractionResult onItemSecondaryUseFirst(ItemStack stack, UseOnContext context) {
    this.start = null;
    this.save(stack.getOrCreateTag());
    return InteractionResult.PASS;
  }

  public boolean isVolumeValid(BlockPos end) {
    int width = Math.abs(start.getX() - end.getX());
    int height = Math.abs(start.getY() - end.getY());
    int depth = Math.abs(start.getZ() - end.getZ());
    int volume = width * height * depth;
    return volume <= MAE2.CONFIG.faulty().maxUberVolume();
  }

  @Override
  protected FaultyCardMode load(CompoundTag tag) {
    if (tag.contains(START_POS)) {
      this.start = BlockPos.of(tag.getLong(START_POS));
    }
    return this;
  }
    
  @Override
  public CompoundTag save(CompoundTag tag) {
    CompoundTag data = super.save(tag);
    if (this.start != null) {
      data.putLong(START_POS, this.start.asLong());
    }
    return data;
  }

	@Override
	protected Component getName() {
    return Component.translatable(TransHelper.GUI.toKey("faulty", "uber"));
	}

	@Override
	public int getTintColor() {
    return AEColor.LIGHT_BLUE.mediumVariant;
	}

  // TODO: move this to main faulty card class so others could utilize it
  @SubscribeEvent
  public static void onRenderLevel(RenderLevelStageEvent event) {
    if (event.getStage() != Stage.AFTER_SOLID_BLOCKS)
      return;
    Minecraft client = Minecraft.getInstance();
    LocalPlayer player = client.player;
    ItemStack main = player.getMainHandItem();
    if (main.isEmpty()) {
      main = player.getOffhandItem();
    }
    if (!main.isEmpty() && main.getItem() == MAE2Items.FAULTY_MEMORY_CARD.get()) {
      FaultyCardMode mode = FaultyCardMode.of(main);
      if (mode instanceof UberMode uber) {
        BlockHitResult hit = rayTrace(player.level(), player);
        if (uber.start != null && hit.getType() == HitResult.Type.BLOCK) {
          BlockPos end = hit.getBlockPos();
          // rendering stuff I kinda understand
          PoseStack poses = event.getPoseStack();
          poses.pushPose();
          Vec3 camera = event.getCamera().getPosition();
          poses.translate(-camera.x, -camera.y, -camera.z);
          // this automatically figures out how to fit the entire corner blocks
          // into the bounding box
          BoundingBox box = BoundingBox.fromCorners(end, uber.start);
          int color = uber.isVolumeValid(end) ? AEColor.LIGHT_BLUE.mediumVariant : AEColor.RED.mediumVariant;
          float red   = ((color & 0xFF0000) >> 16) / 255f;
          float green = ((color & 0x00FF00) >>  8) / 255f;
          float blue  = ((color & 0x0000FF) >>  0) / 255f;

          // what is a tesselator? idk, but this lets me render stuff
          // feel like this should be bad, but idk. Should only be a problem when
          // rendering the box anyways
          BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
          MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(bufferBuilder);
          VertexConsumer consumer = buffer.getBuffer(RenderType.lines());
          // inflate by tiny bit to prevent z-fighting
          AABB aabb = AABB.of(box).inflate(0.01);
          LevelRenderer.renderLineBox(poses, consumer, aabb, red, green, blue, 1);
          buffer.endBatch();
          poses.popPose();
        }
      }
    }
  }

  // TODO move this to a shared utils class
  public static BlockHitResult rayTrace(Level level, Player player) {
    return rayTrace(level, player, player.getBlockReach());
  }
  // stolen with permission from Spatial Tools Compatiable
  public static BlockHitResult rayTrace(Level level, Player player, double maxDistance) {
    Vec3 eye = player.getEyePosition(1.0F);
    Vec3 look = player.getViewVector(1.0F);
    Vec3 end = eye.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);

    ClipContext context = new ClipContext(
                                          eye,
                                          end,
                                          ClipContext.Block.OUTLINE,
                                          ClipContext.Fluid.NONE,
                                          player);

    HitResult result = level.clip(context);
    if (result instanceof BlockHitResult blockHit && result.getType() == HitResult.Type.BLOCK) {
      return blockHit;
    }

    return BlockHitResult.miss(end, Direction.getNearest(look.x, look.y, look.z), BlockPos.containing(end));
  }
}
