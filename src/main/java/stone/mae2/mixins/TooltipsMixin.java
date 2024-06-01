package stone.mae2.mixins;

import appeng.core.localization.Tooltips;
import appeng.core.localization.Tooltips.Amount;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Tooltips.class, remap = false)
public abstract class TooltipsMixin {

    @Shadow
    public static long[] BYTE_NUMS;

    @Shadow
    public static String[] units;

    /**
     * Overwritten because AE2's code is just wrong and I don't want to jump through
     * hoops fixing it
     * 
     * TODO Push this fix to mainline AE2 since its literally a bug
     * 
     * @author stone
     * @reason AE2 is wrong
     * @param amount
     * @return
     */
    @Overwrite
    public static Amount getByteAmount(long amount) {
        if (amount < 1024)
        {
            return new Amount(String.valueOf(amount), "");
        } else
        {
            int i = 0;
            double mantissa = amount;
            while (i < units.length && mantissa >= 1024)
            {
                i++;
                mantissa /= 1024;
            }
            return new Amount(Tooltips.getAmount(mantissa, 1), units[i - 1]);
        }
    }
}
