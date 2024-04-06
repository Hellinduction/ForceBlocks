package club.hellin.forceblocks.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public final class WorldEditUtils {
    public static List<Location> makeSphere(org.bukkit.World world, Vector center, double radius) {
        List<Location> sphereBlocks = new ArrayList<>();

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        for (int x = centerX - (int) radius; x <= centerX + radius; x++) {
            for (int y = centerY - (int) radius; y <= centerY + radius; y++) {
                for (int z = centerZ - (int) radius; z <= centerZ + radius; z++) {
                    if (isInSphere(centerX, centerY, centerZ, x, y, z, radius)) {
                        sphereBlocks.add(new Location(world, x, y, z));
                    }
                }
            }
        }

        return sphereBlocks;
    }

    private static boolean isInSphere(int centerX, int centerY, int centerZ, int x, int y, int z, double radius) {
        return Math.pow(x - centerX, 2) + Math.pow(y - centerY, 2) + Math.pow(z - centerZ, 2) <= Math.pow(radius, 2);
    }
}