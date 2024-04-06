package club.hellin.forceblocks.forceblock.impl;

import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
@Getter
@Setter
public final class ForceBlockConfig {
    @Comment("Where this Force Block is situated")
    private Location location;

    @Comment("Radius of the force produced by this Force Block")
    private int radius;

    @Comment("Owner of this Force Block")
    private UUID owner; // Who placed it

    @Comment("List of people besides the owner that are allowed to enter the area protected by the Force Block")
    private List<UUID> trusted = new ArrayList<>();

    @Comment("Which mode this Force Block is in (FORCE_FIELD, MAGNET)")
    private ForceMode mode = ForceMode.FORCE_FIELD;
}