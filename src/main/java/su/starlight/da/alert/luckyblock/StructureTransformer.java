package su.starlight.da.alert.luckyblock;

import net.minecraft.core.BlockPos;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.StructureBlock;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.util.BlockTransformer;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

@SuppressWarnings("UnstableApiUsage")
public final class StructureTransformer implements BlockTransformer {

    private final Consumer<BlockPos> playerBlockPos;

    public StructureTransformer(Consumer<BlockPos> playerBlockPos) {
        this.playerBlockPos = playerBlockPos;
    }

    @Override
    public @NotNull BlockState transform(@NotNull LimitedRegion region, int x, int y, int z, @NotNull BlockState current, @NotNull TransformationState transformationState) {
        if(current.getType() == Material.STRUCTURE_BLOCK && ((StructureBlock) current.getBlockData()).getMode() == StructureBlock.Mode.DATA) {
            playerBlockPos.accept(new BlockPos(x, y, z));
            return Material.AIR.createBlockData().createBlockState();
        }
        return current;
    }

}
