package club.hellin.forceblocks.forceblock.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.exlll.configlib.Comment;
import de.exlll.configlib.Configuration;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
@Getter
@Setter
public final class ForceBlockConfig {
    @Comment("Where this Force Block is situated")
    @JsonIgnore
    private Location location;

    @Comment("Radius of the force produced by this Force Block")
    @JsonIgnore
    private int radius;

    @Comment("Owner of this Force Block")
    private UUID owner; // Who placed it

    @Comment("Material of this Force Block")
    @JsonIgnore
    private Material material;

    @Comment("List of people besides the owner that are allowed to enter the area protected by the Force Block")
    private List<UUID> trusted = new ArrayList<>();

    @Comment("Which mode this Force Block is in (FORCE_FIELD, MAGNET)")
    private ForceMode mode = ForceMode.FORCE_FIELD;

    @Comment("Should this Force Block affect players?")
    private boolean affectPlayers = true;

    @Comment("Should this Force Block affect non hostile mobs?")
    private boolean affectNonHostileMobs = false;

    @Comment("Should this Force Block affect explosives?")
    private boolean affectExplosives = true;

    @Comment("Should this Force Block affect hostile mobs?")
    private boolean affectHostileMobs = true;

    @Comment("Should this Force Block affect trusted players?")
    private boolean affectTrustedPlayers = false;

    @Comment("Should this Force Block affect projectiles?")
    private boolean affectProjectiles = true;

    @Comment("Should this Force Block display a hologram above it?")
    private boolean displayHologram = true;

    @Comment("Should this Force Block display particles above it?")
    private boolean displayParticles = true;

    @Comment("Should this Force Block affect the owner? This will overrule affectTrustedPlayers")
    private boolean affectOwner = false;
}