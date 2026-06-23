package cl.maxi.lanserveroptions.mixin;

import cl.maxi.lanserveroptions.ConfigurableIntegratedServer;
import java.io.IOException;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.server.LanServerPinger;
import net.minecraft.server.MinecraftServer.MultiplayerScope;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin implements ConfigurableIntegratedServer {
	@Shadow
	private @Nullable LanServerPinger lanPinger;

	@Unique
	private int lanServerOptions$maxPlayers = 8;

	@Override
	public void lanServerOptions$setMaxPlayers(int maxPlayers) {
		this.lanServerOptions$maxPlayers = Math.clamp(maxPlayers, 1, 100);
	}

	@Override
	public boolean lanServerOptions$refreshLanAnnouncement() {
		IntegratedServer server = (IntegratedServer) (Object) this;
		if (server.getMultiplayerScope() != MultiplayerScope.LAN || this.lanPinger == null) {
			return true;
		}

		try {
			LanServerPinger replacement = new LanServerPinger(server.getMotd(), Integer.toString(server.getPort()));
			this.lanPinger.interrupt();
			this.lanPinger = replacement;
			replacement.start();
			return true;
		} catch (IOException ignored) {
			return false;
		}
	}

	@Inject(method = "getMaxPlayers", at = @At("HEAD"), cancellable = true)
	private void lanServerOptions$getMaxPlayers(CallbackInfoReturnable<Integer> cir) {
		cir.setReturnValue(this.lanServerOptions$maxPlayers);
	}
}
