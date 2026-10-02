package club.hellin.forceblocks.customentities;

import lombok.experimental.UtilityClass;

/**
 * Parrot entity type moved from EntityType to EntityTypes in 26.1
 */
@UtilityClass
public final class ParrotCompat {
    private static final String[] HOLDERS = {
            "net.minecraft.world.entity.EntityTypes", // 26.1+
            "net.minecraft.world.entity.EntityType" // 1.21.11 and below
    };

    public Object getParrotEntityType() {
        for (final String holder : HOLDERS) {
            try {
                return Class.forName(holder).getField("PARROT").get(null);
            } catch (final ReflectiveOperationException | LinkageError ignored) {}
        }

        return null;
    }
}
