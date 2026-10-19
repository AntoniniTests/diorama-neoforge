package dev.diorama.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.diorama.waypoint.WorldProjection;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the projection the world is drawn with, after view bobbing, the damage tilt and
 * nausea are applied, so waypoint labels can stay pinned to their spot in the world.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	// renderLevel set the world's projection through resetProjectionMatrix, and it was the only
	// Matrix4f local at that point.
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;resetProjectionMatrix(Lorg/joml/Matrix4f;)V", ordinal = 0))
	private void diorama$captureWorldProjection(CallbackInfo ci, @Local(ordinal = 0) Matrix4f projectionMatrix) {
		Camera camera = ((GameRenderer) (Object) this).getMainCamera();
		WorldProjection.capture(projectionMatrix, new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf())), camera.getPosition());
	}
}
