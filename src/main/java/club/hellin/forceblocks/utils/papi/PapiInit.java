package club.hellin.forceblocks.utils.papi;

import club.hellin.forceblocks.Main;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;

@UtilityClass
public final class PapiInit {
    public void initPapi() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null)
            return;

        new ForceBlocksExpansion(Main.instance).register();
    }
}