package club.hellin.forceblocks;

import club.hellin.forceblocks.commands.ApplyForceBlockCommand;
import club.hellin.forceblocks.commands.ForceBlockTrustCommand;
import club.hellin.forceblocks.commands.ProjectileAimbotCommand;
import club.hellin.forceblocks.commands.ReachCommand;
import club.hellin.forceblocks.forceblock.ForceBlockManager;
import club.hellin.forceblocks.listeners.ForceBlockListeners;
import club.hellin.forceblocks.utils.papi.PapiInit;
import com.jonahseguin.drink.CommandService;
import com.jonahseguin.drink.Drink;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    public static final String NBT_RADIUS_TAG = "force_block_radius";

    public static Main instance;

    public Main() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.registerListeners();
        this.registerCommands();

        ForceBlockManager.getInstance().init();
        PapiInit.initPapi();
    }

    @Override
    public void onDisable() {
    }

    private void registerListeners() {
        final PluginManager manager = Bukkit.getPluginManager();
        manager.registerEvents(new ForceBlockListeners(), instance);
    }

    private void registerCommands() {
        final CommandService service = Drink.get(instance);

        service.register(new ApplyForceBlockCommand(), "applyforceblock");
        service.register(new ForceBlockTrustCommand(), "forceblocktrust");
        service.register(new ProjectileAimbotCommand(), "projectileaimbot");
        service.register(new ReachCommand(), "reach");

        service.registerCommands();
    }
}