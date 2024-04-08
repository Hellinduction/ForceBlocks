package club.hellin.forceblocks;

import club.hellin.forceblocks.commands.*;
import club.hellin.forceblocks.forceblock.ForceBlockManager;
import club.hellin.forceblocks.listeners.ForceBlockListeners;
import club.hellin.forceblocks.utils.papi.PapiInit;
import com.jonahseguin.drink.CommandService;
import com.jonahseguin.drink.Drink;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.patheloper.api.pathing.Pathfinder;
import org.patheloper.api.pathing.configuration.HeuristicWeights;
import org.patheloper.api.pathing.configuration.PathingRuleSet;
import org.patheloper.mapping.PatheticMapper;

@Getter
public final class Main extends JavaPlugin {
    public static final String NBT_RADIUS_TAG = "force_block_radius";

    public static Main instance;

    private BypassForceBlockCommand bypassForceBlockCommand;
    private Pathfinder pathfinder;
    private Pathfinder playerPathfinder;

    public Main() {
        instance = this;
    }

    @Override
    public void onEnable() {
        PatheticMapper.initialize(instance);

        this.pathfinder = PatheticMapper.newPathfinder(PathingRuleSet.createAsyncRuleSet()
                .withAllowingFailFast(true)
                .withAllowingFallback(true)
                .withLoadingChunks(true)
                .withAllowingDiagonal(false)
                .withHeuristicWeights(HeuristicWeights.create(0.6, 0.15, 0.2, 0.3)));

        this.playerPathfinder = PatheticMapper.newPathfinder(PathingRuleSet.createAsyncRuleSet()
                .withAllowingFailFast(false)
                .withAllowingFallback(true)
                .withLoadingChunks(true)
                .withAllowingDiagonal(false)
                .withHeuristicWeights(HeuristicWeights.DIRECT_PATH_WEIGHTS));

        this.registerListeners();
        this.registerCommands();

        ForceBlockManager.getInstance().init();
        PapiInit.initPapi();
    }

    @Override
    public void onDisable() {
        PatheticMapper.shutdown();
    }

    private void registerListeners() {
        final PluginManager manager = Bukkit.getPluginManager();
        manager.registerEvents(new ForceBlockListeners(), instance);
    }

    private void registerCommands() {
        final CommandService service = Drink.get(instance);

        service.register(new ApplyForceBlockCommand(), "applyforceblock");
        service.register(new FollowCommand(), "follow");
        service.register(new ForceBlockTrustCommand(), "forceblocktrust");
        service.register(new ProjectileAimbotCommand(), "projectileaimbot");
        service.register(new ReachCommand(), "reach");
        service.register(this.bypassForceBlockCommand = new BypassForceBlockCommand(), "bypassforceblock");

        service.registerCommands();
    }
}