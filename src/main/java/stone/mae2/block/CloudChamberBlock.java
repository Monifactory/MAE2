package stone.mae2.block;

import appeng.block.AEBaseBlock;
import appeng.decorative.solid.QuartzGlassBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import stone.mae2.bootstrap.MAE2Tags;

public class CloudChamberBlock extends QuartzGlassBlock {

    public static final int AREA = 16;
    public static final int PARTICLES_PER_BLOCK = 8;

    protected long lastTick = -1;

    public CloudChamberBlock(Properties properties) { super(properties); }

    public CloudChamberBlock() {
        this(AEBaseBlock.glassProps().noOcclusion().isValidSpawn((p1, p2, p3, p4) -> false));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        /*
         * // setup random that'll be the same for each block in the area long seed =
         * level.getGameTime(); seed ^= pos.getX() / AREA; seed ^= pos.getY() / AREA;
         * seed ^= pos.getZ() / AREA; Random rand = new Random(seed);
         * 
         * Vec3 start = new Vec3(rand.nextDouble(AREA), rand.nextDouble(AREA),
         * rand.nextDouble(AREA)); Vec3 end = new Vec3(rand.nextDouble(AREA),
         * rand.nextDouble(AREA), rand.nextDouble(AREA));
         */
        if (level.getGameTime() > lastTick)
        {
            lastTick = level.getGameTime();
            if (random.nextFloat() < .5f)
            {
                @SuppressWarnings("resource")
                Vec3 playerPos = Minecraft.getInstance().player.position();
                Vec3 offset = playerPos.offsetRandom(random, AREA);
                double polar = random.nextDouble() * 2 * Math.PI;
                double azimuthal = random.nextDouble() * 2 * Math.PI;
                Vec3 normal = new Vec3(Math.sin(polar) * Math.cos(azimuthal),
                    Math.cos(polar), Math.sin(polar) * Math.sin(azimuthal));
                drawTrail(level, normal.scale(-AREA).add(offset), normal.scale(AREA).add(offset));
            }
            if (random.nextFloat() < .1f)
            {
                Vec3 playerPos = Minecraft.getInstance().player.position();
                Vec3 offset = new Vec3(playerPos.x() + random.nextDouble() * AREA, playerPos.y() + random.nextDouble() * AREA, playerPos.z() + random.nextDouble() * AREA);
                Vec3 start = new Vec3(+random.nextDouble() * AREA, +random.nextDouble() * AREA,
                    +random.nextDouble() * 3).add(offset);
                Vec3 end = new Vec3(-random.nextDouble() * AREA, -random.nextDouble() * AREA,
                    -random.nextDouble() * 3).add(offset);
                drawTrail(level, start, end,
                    net.minecraft.core.particles.ParticleTypes.CLOUD);
            }
        }
    }

    // public static void drawTrail(Level level, Vec3 start, RandomSource random, )

    /**
     * Draws a cloud chamber trail from start to end
     * 
     * @param level
     * @param start
     * @param end
     * @param particle
     */
    public static void drawTrail(Level level, Vec3 start, Vec3 end, ParticleOptions particle) {
        double step = (1d / (PARTICLES_PER_BLOCK * start.distanceTo(end)));
        Vec3i lastBlock = new Vec3i((int) Math.floor(start.x), (int) Math.floor(start.y),
            (int) Math.floor(start.z));
        boolean inChamber = level
            .getBlockState(new BlockPos(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ()))
            .is(MAE2Tags.CLOUD_CHAMBERS);
        ;
        for (double i = 0; i < 1; i += step)
        {
            Vec3 lerped = start.lerp(end, i);
            if ((int) Math.floor(lerped.x) != lastBlock.getX()
                || (int) Math.floor(lerped.y) != lastBlock.getY()
                || (int) Math.floor(lerped.z) != lastBlock.getZ())
            {
                inChamber = level
                    .getBlockState(new BlockPos((int) Math.floor(lerped.x),
                        (int) Math.floor(lerped.y), (int) Math.floor(lerped.z)))
                    .is(MAE2Tags.CLOUD_CHAMBERS);
                lastBlock = new Vec3i((int) Math.floor(lerped.x), (int) Math.floor(lerped.y),
                    (int) Math.floor(lerped.z));
            }
            if (inChamber)
            {
                level
                    .addParticle(particle, lerped.x(), lerped.y(), lerped.z(),
                        0, 0, 0);
            } else
            {
                i += step;
            }

        }
    }

    public static void drawTrail(Level level, Vec3 start, Vec3 end) {
        drawTrail(level, start, end, appeng.client.render.effects.ParticleTypes.VIBRANT);
    }
}
