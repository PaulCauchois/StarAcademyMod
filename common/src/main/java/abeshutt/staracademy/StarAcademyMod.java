package abeshutt.staracademy;

import abeshutt.staracademy.init.ModConfigs;
import abeshutt.staracademy.init.ModRegistries;
import abeshutt.staracademy.world.random.JavaRandom;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public final class StarAcademyMod {

    public static final String ID = "academy";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static void init() {
        Cobblemon.INSTANCE.setStarterHandler(new GameStarterHandler());
        ModRegistries.register();

        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
            World world = event.getEntity().getEntityWorld();
            WorldBorder border = world.getWorldBorder();
            double dx = event.getEntity().getPos().getX() - border.getCenterX();
            double dz = event.getEntity().getPos().getZ() - border.getCenterZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            ModConfigs.POKEMON_SPAWN.getLevel(distance).ifPresent(roll -> {
                event.getEntity().getPokemon().setLevel(roll.get(JavaRandom.ofNanoTime()));
            });

            return Unit.INSTANCE;
        });

        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.NORMAL, event -> {
            MinecraftServer server = event.getEntity().getWorld().getServer();
            if(server == null) return Unit.INSTANCE;
            Pokemon pokemon = event.getEntity().getPokemon();

            List<String> prefixes = new ArrayList<>();
            if(pokemon.getShiny()) prefixes.add("Shiny");
            if(pokemon.isLegendary()) prefixes.add("Legendary");

            if(pokemon.getShiny() || pokemon.isLegendary()) {
                MutableText message = Text.empty()
                    .append(Text.literal("A ").formatted(Formatting.BOLD))
                    .append(Text.literal(String.join(" ", prefixes)).formatted(Formatting.BOLD))
                    .append(prefixes.isEmpty() ? Text.empty() : Text.literal(" "))
                    .append(event.getEntity().getDisplayName().copy().formatted(Formatting.BOLD))
                    .append(Text.literal(" has spawned near someone!").formatted(Formatting.BOLD));

                for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.sendMessage(message);
                }
            }

            return Unit.INSTANCE;
        });
    }

    public static Identifier id(String path) {
        return new Identifier(ID, path);
    }

}
