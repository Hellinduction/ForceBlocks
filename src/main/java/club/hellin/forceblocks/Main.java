package club.hellin.forceblocks;

import club.hellin.forceblocks.commands.*;
import club.hellin.forceblocks.forceblock.ForceBlockManager;
import club.hellin.forceblocks.inventory.InventoryManager;
import club.hellin.forceblocks.listeners.ForceBlockListeners;
import club.hellin.forceblocks.listeners.InventoryListeners;
import club.hellin.forceblocks.utils.papi.PapiInit;
import com.jonahseguin.drink.CommandService;
import com.jonahseguin.drink.Drink;
import de.metaphoriker.pathetic.api.factory.PathfinderFactory;
import de.metaphoriker.pathetic.api.factory.PathfinderInitializer;
import de.metaphoriker.pathetic.api.pathing.Pathfinder;
import de.metaphoriker.pathetic.api.pathing.configuration.HeuristicWeights;
import de.metaphoriker.pathetic.api.pathing.configuration.PathfinderConfiguration;
import de.metaphoriker.pathetic.bukkit.PatheticBukkit;
import de.metaphoriker.pathetic.bukkit.initializer.BukkitPathfinderInitializer;
import de.metaphoriker.pathetic.bukkit.provider.LoadingNavigationPointProvider;
import de.metaphoriker.pathetic.engine.factory.AStarPathfinderFactory;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Main extends JavaPlugin {
    public static final String NBT_RADIUS_TAG = "force_block_radius";

    public static Main instance;

    private Pathfinder pathfinder;
    private Pathfinder playerPathfinder;

    public Main() {
        instance = this;
    }

    private void initPathfinders() {
        PatheticBukkit.initialize(instance);

        final PathfinderFactory factory = new AStarPathfinderFactory();
        final PathfinderInitializer initializer = new BukkitPathfinderInitializer();

        final PathfinderConfiguration pathfinderConfig = PathfinderConfiguration.builder()
                .provider(new LoadingNavigationPointProvider())
                .fallback(true)
                .heuristicWeights(HeuristicWeights.create(0.6, 0.15, 0.2, 0.3))
                .build();

        final PathfinderConfiguration playerPathfinderConfig = PathfinderConfiguration.builder()
                .provider(new LoadingNavigationPointProvider())
                .fallback(true)
                .heuristicWeights(HeuristicWeights.DIRECT_PATH_WEIGHTS)
                .build();

        this.pathfinder = factory.createPathfinder(pathfinderConfig, initializer);
        this.playerPathfinder = factory.createPathfinder(playerPathfinderConfig, initializer);
    }

    @Override
    public void onEnable() {
        try {
            this.initPathfinders();
        } catch (final Exception exception) {
            exception.printStackTrace();
        }

        this.registerListeners();
        this.registerCommands();

        ForceBlockManager.getInstance().init();
        InventoryManager.getInstance().init();

        PapiInit.initPapi();
    }

    @Override
    public void onDisable() {
        ForceBlockManager.getInstance().shutdown();
    }

    private void registerListeners() {
        final PluginManager manager = Bukkit.getPluginManager();

        manager.registerEvents(new ForceBlockListeners(), instance);
        manager.registerEvents(new InventoryListeners(), instance);
    }

    private void registerCommands() {
        final CommandService service = Drink.get(instance);

        service.register(new ApplyForceBlockCommand(), "applyforceblock");
        service.register(new FollowCommand(), "follow");
        service.register(new ForceBlockTrustCommand(), "forceblocktrust");
        service.register(new ProjectileAimbotCommand(), "projectileaimbot");
        service.register(new ReachCommand(), "reach");
        service.register(new BypassForceBlockCommand(), "bypassforceblock");

        if (isPaper())
            service.register(new CarrierParrotCommand(), "carrierparrot");

//        service.register(new OpenInventoryCommand(), "openinventory");

        service.registerCommands();
    }

    public static boolean isPaper() {
        boolean isPaper = false;

        try {
            Class.forName("io.papermc.paper.plugin.manager.PaperPluginManagerImpl");
            isPaper = true;
        } catch (final Throwable throwable) {}

        return isPaper;
    }
}