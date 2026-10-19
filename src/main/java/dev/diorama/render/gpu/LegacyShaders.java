package dev.diorama.render.gpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.diorama.Diorama;
import net.minecraft.client.renderer.RenderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;

// How ImmediateCanvas draws before 1.21.6: three render types without depth testing, so the map
// draws in painter's order like the GUI render state of later versions. Textures are bound by
// hand (RenderSystem.setShaderTexture) before each draw, so none of these has a texture shard.
//
// A mod shader that fails to compile through the game's normal path takes the player's resource
// packs (1.21.2-1.21.4) or the whole resource reload (1.21.1, 1.21.5) down with it, so the fog's
// program only ever comes through a side door; fogReady() says whether it may be drawn. A resource
// reload can change the shader's sources, so the fog is checked again after each one.
final class LegacyShaders {
	private static final Logger LOGGER = LoggerFactory.getLogger("diorama");

	private static ShaderInstance fogShader;
	private static final RenderStateShard.ShaderStateShard TEXTURED_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader);
	private static final RenderStateShard.ShaderStateShard FOG_SHADER = new RenderStateShard.ShaderStateShard(() -> fogShader);
	// Each resource reload makes new instances of vanilla's own programs; this is the one seen at the
	// last check, so a different one means the fog has to be checked again.
	private static Object fogReload;
	private static boolean fogFailed;

	static final RenderType TEXTURED = create("diorama_textured", DefaultVertexFormat.POSITION_TEX_COLOR, TEXTURED_SHADER);
	static final RenderType SOLID = create("diorama_solid", DefaultVertexFormat.POSITION_COLOR, RenderStateShard.POSITION_COLOR_SHADER);
	static final RenderType FOG = create("diorama_fog", DefaultVertexFormat.POSITION_TEX_COLOR, FOG_SHADER);

	private static RenderType create(String name, VertexFormat format, RenderStateShard.ShaderStateShard shader) {
		return RenderType.create(name, format, VertexFormat.Mode.QUADS, 1536, RenderType.CompositeState.builder()
			.setShaderState(shader)
			.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
			.setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
			.setCullState(RenderStateShard.NO_CULL)
			.setWriteMaskState(RenderStateShard.COLOR_WRITE)
			.createCompositeState(false));
	}

	static boolean fogReady() {
		ShaderInstance reload = GameRenderer.getPositionTexColorShader();
		if (reload != fogReload) {
			fogReload = reload;
			fogFailed = false;
			if (fogShader != null) {
				fogShader.close();
				fogShader = null;
			}
		}
		// Built by hand rather than through Fabric's core shader callback, whose failure would fail
		// the whole resource reload. The json lives in the minecraft namespace, where the vanilla
		// constructor looks; its stages name diorama:fog, which Fabric API resolves. The game doesn't
		// know about it, so it's closed and built again here after a reload.
		if (fogShader == null && !fogFailed) {
			try {
				fogShader = new ShaderInstance(Minecraft.getInstance().getResourceManager(), "diorama_fog", DefaultVertexFormat.POSITION_TEX_COLOR);
			} catch (Exception e) {
				fogFailed = true;
				LOGGER.error("Diorama's fog shader didn't compile; the map draws without fog", e);
			}
		}
		return fogShader != null;
	}

	// The depth test shard for "always" leaves depth testing as it found it, and before 1.21.2 the
	// GUI's flush turns it on, so earlier GUI shapes could hide parts of the map. Off for the draw,
	// then back as it was.
	static void draw(RenderType type, MeshData mesh) {
		boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		RenderSystem.disableDepthTest();
		type.draw(mesh);
		if (depth) {
			RenderSystem.enableDepthTest();
		}
	}

	static void drawFog(MeshData mesh) {
		draw(FOG, mesh);
	}

	private LegacyShaders() {
	}
}
