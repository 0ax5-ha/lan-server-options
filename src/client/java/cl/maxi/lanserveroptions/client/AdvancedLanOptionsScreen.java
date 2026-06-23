package cl.maxi.lanserveroptions.client;

import cl.maxi.lanserveroptions.ConfigurableIntegratedServer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.gamerules.GameRules;
import org.jspecify.annotations.Nullable;

public class AdvancedLanOptionsScreen extends Screen {
	private static final Component TITLE = Component.translatable("lan_server_options.title");
	private static final int MAX_MOTD_LENGTH = 80;
	private static final int MIN_PLAYERS = 1;
	private static final int MAX_PLAYERS = 100;
	private static final int MAX_IDLE_MINUTES = 1440;

	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private final Screen parent;
	private boolean onlineMode;
	private boolean whitelist;
	private boolean pvp;
	private int maxPlayers;
	private int idleMinutes;
	private @Nullable EditBox motdEdit;
	private @Nullable EditBox maxPlayersEdit;
	private @Nullable EditBox idleEdit;
	private @Nullable Button applyButton;
	private @Nullable StringWidget offlineWarning;
	private boolean maxPlayersValid = true;
	private boolean idleValid = true;

	public AdvancedLanOptionsScreen(Screen parent) {
		super(TITLE);
		this.parent = parent;
	}

	@Override
	protected void init() {
		IntegratedServer server = this.minecraft.getSingleplayerServer();
		if (server == null) {
			this.onClose();
			return;
		}

		this.onlineMode = server.usesAuthentication();
		this.whitelist = server.isUsingWhitelist();
		this.pvp = server.getGameRules().get(GameRules.PVP);
		this.maxPlayers = server.getMaxPlayers();
		this.idleMinutes = server.playerIdleTimeout();

		this.layout.addTitleHeader(this.title, this.font);
		LinearLayout content = this.layout.addToContents(LinearLayout.vertical().spacing(4));
		content.defaultCellSetting().alignHorizontallyCenter();

		CycleButton<Boolean> onlineButton = content.addChild(
			CycleButton.onOffBuilder(this.onlineMode).create(Component.translatable("lan_server_options.online_mode"), (button, value) -> {
				this.onlineMode = value;
				this.updateOfflineWarning();
			})
		);
		onlineButton.setTooltip(Tooltip.create(Component.translatable("lan_server_options.online_mode.tooltip")));
		CycleButton<Boolean> whitelistButton = content.addChild(
			CycleButton.onOffBuilder(this.whitelist).create(Component.translatable("lan_server_options.whitelist"), (button, value) -> this.whitelist = value)
		);
		whitelistButton.setTooltip(Tooltip.create(Component.translatable("lan_server_options.whitelist.tooltip")));

		CycleButton<Boolean> pvpButton = content.addChild(
			CycleButton.onOffBuilder(this.pvp).create(Component.translatable("lan_server_options.pvp"), (button, value) -> this.pvp = value)
		);
		pvpButton.setTooltip(Tooltip.create(Component.translatable("lan_server_options.pvp.tooltip")));

		this.motdEdit = new EditBox(this.font, 180, 20, Component.translatable("lan_server_options.motd"));
		this.motdEdit.setMaxLength(MAX_MOTD_LENGTH);
		this.motdEdit.setValue(server.getMotd());
		this.motdEdit.setHint(Component.translatable("lan_server_options.motd"));
		content.addChild(this.labeledRow(Component.translatable("lan_server_options.motd"), this.motdEdit));

		this.maxPlayersEdit = new EditBox(this.font, 72, 20, Component.translatable("lan_server_options.max_players"));
		this.maxPlayersEdit.setValue(Integer.toString(this.maxPlayers));
		this.maxPlayersEdit.setResponder(this::validateMaxPlayers);
		content.addChild(this.labeledRow(Component.translatable("lan_server_options.max_players"), this.maxPlayersEdit));

		this.idleEdit = new EditBox(this.font, 72, 20, Component.translatable("lan_server_options.idle_timeout"));
		this.idleEdit.setValue(Integer.toString(this.idleMinutes));
		this.idleEdit.setResponder(this::validateIdleTimeout);
		content.addChild(this.labeledRow(Component.translatable("lan_server_options.idle_timeout"), this.idleEdit));

		this.offlineWarning = content.addChild(new StringWidget(Component.translatable("lan_server_options.offline_warning.short"), this.font));
		this.offlineWarning.setMessage(this.offlineWarning.getMessage().copy().withStyle(ChatFormatting.YELLOW));
		this.offlineWarning.setTooltip(Tooltip.create(Component.translatable("lan_server_options.offline_warning")));
		this.updateOfflineWarning();

		LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
		this.applyButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> this.apply(server)).build());
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose()).build());

		this.layout.visitWidgets(this::addRenderableWidget);
		this.repositionElements();
	}

	private LinearLayout labeledRow(Component label, EditBox field) {
		LinearLayout row = LinearLayout.horizontal().spacing(6);
		row.defaultCellSetting().alignVerticallyMiddle();
		row.addChild(new StringWidget(label, this.font));
		row.addChild(field);
		return row;
	}

	private void validateMaxPlayers(String value) {
		Integer parsed = parseInRange(value, MIN_PLAYERS, MAX_PLAYERS);
		this.maxPlayersValid = parsed != null;
		if (parsed != null) {
			this.maxPlayers = parsed;
		}
		setValidationState(this.maxPlayersEdit, this.maxPlayersValid, Component.translatable("lan_server_options.max_players.error", MIN_PLAYERS, MAX_PLAYERS));
		this.updateApplyButton();
	}

	private void validateIdleTimeout(String value) {
		Integer parsed = parseInRange(value, 0, MAX_IDLE_MINUTES);
		this.idleValid = parsed != null;
		if (parsed != null) {
			this.idleMinutes = parsed;
		}
		setValidationState(this.idleEdit, this.idleValid, Component.translatable("lan_server_options.idle_timeout.error", MAX_IDLE_MINUTES));
		this.updateApplyButton();
	}

	private static @Nullable Integer parseInRange(String value, int min, int max) {
		try {
			int parsed = Integer.parseInt(value);
			return parsed >= min && parsed <= max ? parsed : null;
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private static void setValidationState(@Nullable EditBox field, boolean valid, Component error) {
		if (field != null) {
			field.setTextColor(valid ? EditBox.DEFAULT_TEXT_COLOR : 0xFFFF5555);
			field.setTooltip(valid ? null : Tooltip.create(error));
		}
	}

	private void updateApplyButton() {
		if (this.applyButton != null) {
			this.applyButton.active = this.maxPlayersValid && this.idleValid;
		}
	}

	private void apply(IntegratedServer server) {
		String previousMotd = server.getMotd();
		String newMotd = this.motdEdit == null ? previousMotd : this.motdEdit.getValue().strip();
		server.setUsesAuthentication(this.onlineMode);
		server.setUsingWhitelist(this.whitelist);
		server.setEnforceWhitelist(this.whitelist);
		server.getGameRules().set(GameRules.PVP, this.pvp, server);
		server.setPlayerIdleTimeout(this.idleMinutes);
		server.setMotd(newMotd);
		((ConfigurableIntegratedServer) server).lanServerOptions$setMaxPlayers(this.maxPlayers);
		server.invalidateStatus();
		if (!previousMotd.equals(newMotd) && !((ConfigurableIntegratedServer) server).lanServerOptions$refreshLanAnnouncement()) {
			Component error = Component.translatable("lan_server_options.motd.refresh_failed");
			this.minecraft.gui.hud.getChat().addClientSystemMessage(error);
			this.minecraft.getNarrator().saySystemQueued(error);
		}
		this.onClose();
	}

	private void updateOfflineWarning() {
		if (this.offlineWarning != null) {
			this.offlineWarning.visible = !this.onlineMode;
		}
	}

	@Override
	protected void repositionElements() {
		if (this.motdEdit != null) {
			this.motdEdit.setWidth(Math.max(100, Math.min(180, this.width - 140)));
		}
		if (this.offlineWarning != null) {
			this.offlineWarning.setMaxWidth(Math.max(100, this.width - 20));
		}
		this.layout.arrangeElements();
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(this.parent);
	}
}
