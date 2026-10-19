package dev.diorama;

import com.mojang.blaze3d.platform.InputConstants;
import dev.diorama.waypoint.Waypoint;
import dev.diorama.waypoint.WaypointEditScreen;
import dev.diorama.waypoint.WaypointHud;
import dev.diorama.waypoint.WaypointListScreen;
import dev.diorama.waypoint.WaypointShare;
import dev.diorama.waypoint.Waypoints;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** NeoForge entry point: client only. The Fabric version's callbacks map onto NeoForge's events. */
@Mod(value = Diorama.MOD_ID, dist = Dist.CLIENT)
public class DioramaClient {
	// Key categories are bare translation keys on 1.21.1.
	public static final String CATEGORY = "key.category.diorama.map";
	public static final KeyMapping OPEN_MAP = new KeyMapping("key.diorama.open_map", InputConstants.KEY_M, CATEGORY);
	public static final KeyMapping NEW_WAYPOINT = new KeyMapping("key.diorama.new_waypoint", InputConstants.KEY_B, CATEGORY);
	public static final KeyMapping WAYPOINT_LIST = new KeyMapping("key.diorama.waypoints", InputConstants.KEY_U, CATEGORY);
	public static final KeyMapping ZOOM_IN = new KeyMapping("key.diorama.zoom_in", InputConstants.KEY_EQUALS, CATEGORY);
	public static final KeyMapping ZOOM_OUT = new KeyMapping("key.diorama.zoom_out", InputConstants.KEY_MINUS, CATEGORY);
	public static final KeyMapping SETTINGS = new KeyMapping("key.diorama.settings", InputConstants.UNKNOWN.getValue(), CATEGORY);
	public static final KeyMapping TOGGLE_MINIMAP = new KeyMapping("key.diorama.toggle_minimap", InputConstants.UNKNOWN.getValue(), CATEGORY);

	private static void changeZoom(int step) {
		Settings s = Settings.get();
		int next = Math.max(0, Math.min(Settings.ZOOMS.length - 1, s.minimapZoom + step));
		if (next != s.minimapZoom) {
			s.minimapZoom = next;
			s.save();
		}
	}

	public DioramaClient(IEventBus modBus) {
		modBus.addListener(DioramaClient::registerKeys);
		modBus.addListener(DioramaClient::registerLayers);

		NeoForge.EVENT_BUS.addListener(DioramaClient::onClientTick);
		NeoForge.EVENT_BUS.addListener(DioramaClient::onChunkLoad);
		NeoForge.EVENT_BUS.addListener(DioramaClient::onSystemChat);
		NeoForge.EVENT_BUS.addListener(DioramaClient::onCommands);
		NeoForge.EVENT_BUS.addListener(DioramaClient::onLoggingOut);
		// Leaving a world saves the map on the next tick, but closing the window stops ticking first.
		NeoForge.EVENT_BUS.addListener(DioramaClient::onShutdown);
	}

	private static void registerKeys(RegisterKeyMappingsEvent event) {
		event.register(OPEN_MAP);
		event.register(NEW_WAYPOINT);
		event.register(WAYPOINT_LIST);
		event.register(ZOOM_IN);
		event.register(ZOOM_OUT);
		event.register(SETTINGS);
		event.register(TOGGLE_MINIMAP);
		Settings.get();
	}

	private static void registerLayers(RegisterGuiLayersEvent event) {
		// Above every vanilla layer, like Fabric's HudElementRegistry.addLast. Each layer sits deeper
		// in z than the one before it, so these draw in front of all of them.
		WaypointHud waypointHud = new WaypointHud();
		MinimapHud minimapHud = new MinimapHud();
		event.registerAboveAll(Diorama.id("waypoints"), waypointHud::render);
		event.registerAboveAll(Diorama.id("minimap"), minimapHud::render);
	}

	private static void onChunkLoad(ChunkEvent.Load event) {
		// The event also fires for an integrated server's chunks; the map only reads the client's.
		if (event.getLevel() instanceof ClientLevel level && event.getChunk() instanceof LevelChunk chunk) {
			MapSession.chunkLoaded(level, chunk);
		}
	}

	private static void onSystemChat(ClientChatReceivedEvent.System event) {
		ServerRules.onServerMessage(event.getMessage());
	}

	private static void onCommands(RegisterClientCommandsEvent event) {
		WaypointShare.register(event.getDispatcher());
	}

	private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		ServerRules.onDisconnect();
	}

	private static void onShutdown(GameShuttingDownEvent event) {
		MapSession.close();
	}

	private static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		MapSession.tick(mc);
		while (OPEN_MAP.consumeClick()) {
			if (mc.screen == null && mc.level != null && ServerRules.minimapAllowed()) {
				mc.setScreen(new MapScreen());
			}
		}
		while (NEW_WAYPOINT.consumeClick()) {
			Waypoint waypoint = Waypoints.atPlayer();
			if (mc.screen == null && waypoint != null) {
				mc.setScreen(new WaypointEditScreen(null, waypoint, true));
			}
		}
		while (ZOOM_IN.consumeClick()) {
			changeZoom(1);
		}
		while (ZOOM_OUT.consumeClick()) {
			changeZoom(-1);
		}
		while (SETTINGS.consumeClick()) {
			if (mc.screen == null) {
				mc.setScreen(new SettingsScreen(null));
			}
		}
		while (TOGGLE_MINIMAP.consumeClick()) {
			Settings s = Settings.get();
			s.minimap = !s.minimap;
			s.save();
		}
		while (WAYPOINT_LIST.consumeClick()) {
			if (mc.screen == null && mc.level != null) {
				mc.setScreen(new WaypointListScreen(null));
			}
		}
	}
}
