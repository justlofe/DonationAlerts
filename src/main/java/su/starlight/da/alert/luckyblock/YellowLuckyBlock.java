package su.starlight.da.alert.luckyblock;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import su.starlight.da.util.ItemContainer;

import java.util.Random;

public final class YellowLuckyBlock extends LuckyBlockBehaviour {

    public YellowLuckyBlock() {
        super(0, 0, 1);
    }

    @Override
    public void onItem(Player player, Random random, ItemContainer container) {

    }

    @Override
    public void onStructure(Player player, Random random, Block block) {
        placeStructure("yellow", player, block);
    }

    @Override
    public void onMob(Player player, Random random, Block block) {
    }

}
