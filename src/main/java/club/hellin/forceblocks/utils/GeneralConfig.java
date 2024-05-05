package club.hellin.forceblocks.utils;

import club.hellin.forceblocks.Main;
import de.exlll.configlib.*;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.*;

@Configuration
@Getter
@Setter
public final class GeneralConfig {
    @Ignore
    public static final YamlConfigurationProperties PROPERTIES = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder().build();

    @Ignore
    private static GeneralConfig SINGLETON_INSTANCE;

    public static GeneralConfig getInstance() {
        if (!(SINGLETON_INSTANCE instanceof GeneralConfig))
            SINGLETON_INSTANCE = load(getConfigFile());

        return SINGLETON_INSTANCE;
    }

    @Comment("Force Block list of bypassed players")
    private List<UUID> bypassList = new ArrayList<>();

    @Comment("List of players with projectile aimbot enabled")
    private List<UUID> projectileAimbotToggledOn = new ArrayList<>();

    @Comment("The map of players to their reach value")
    private Map<UUID, Double> reachMap = new HashMap<>();

    @Comment("Weather or not the Projectile Aimbot should path find")
    private boolean pathFind = false;

    private int bestCounterOfAllTime = 0;

    private static File getConfigFile() {
        return new File(Main.instance.getDataFolder(), "config.yml");
    }

    public static void save(final GeneralConfig config) {
        final Class<GeneralConfig> configClass = GeneralConfig.class;
        final File configFile = getConfigFile();

        if (!configFile.exists()) {
            YamlConfigurations.update(configFile.toPath(), configClass, PROPERTIES);
            return;
        }

        YamlConfigurations.save(configFile.toPath(), configClass, config, PROPERTIES);
    }

    public static GeneralConfig load(final File configFile) {
        final Class<? extends GeneralConfig> configClass = GeneralConfig.class;

        if (!configFile.exists())
            save(null);

        final GeneralConfig config = YamlConfigurations.load(configFile.toPath(), configClass, PROPERTIES);
        return config;
    }
}