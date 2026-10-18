package cl.maxi.lanserveroptions.mixin;

import cl.maxi.lanserveroptions.client.AdvancedLanOptionsScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screen.option.LanOptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LanOptionsScreen.class)
public abstract class MultiplayerOptionsScreenMixin extends Screen {
	@Unique
	private @org.jspecify.annotations.Nullable LinearLayout lanServerOptions$footer;

	protected MultiplayerOptionsScreenMixin(Component title) {
		super(title);
	}

	@Redirect(
		method = "init",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"
		)
	)
	private LayoutElement lanServerOptions$addAdvancedButton(HeaderAndFooterLayout layout, LayoutElement child) {
		if (child instanceof LinearLayout footer) {
			this.lanServerOptions$footer = footer;
			Button advanced = Button.builder(
					Component.translatable("lan_server_options.advanced"),
					button -> this.minecraft.gui.setScreen(new AdvancedLanOptionsScreen(this))
				)
				.build();
			advanced.setTooltip(Tooltip.create(Component.translatable("lan_server_options.advanced.tooltip")));
			footer.addChild(advanced);
		}
		return layout.addToFooter(child);
	}

	@Inject(
		method = "init",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;visitWidgets(Ljava/util/function/Consumer;)V",
			shift = At.Shift.BEFORE
		)
	)
	private void lanServerOptions$resizeFooterButtons(CallbackInfo ci) {
		this.lanServerOptions$adjustFooterButtonWidths();
	}

	@Inject(method = "repositionElements", at = @At("HEAD"))
	private void lanServerOptions$resizeFooterButtonsOnResize(CallbackInfo ci) {
		this.lanServerOptions$adjustFooterButtonWidths();
	}

	@Unique
	private void lanServerOptions$adjustFooterButtonWidths() {
		if (this.lanServerOptions$footer == null) {
			return;
		}

		int buttonWidth = Math.min(150, Math.max(70, (this.width - 32) / 3));
		this.lanServerOptions$footer.visitWidgets(widget -> this.lanServerOptions$setButtonWidth(widget, buttonWidth));
	}

	@Unique
	private void lanServerOptions$setButtonWidth(AbstractWidget widget, int width) {
		if (widget instanceof Button) {
			widget.setWidth(width);
		}
	}
}
