package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Singleton
public class SetDisplayNameCommand extends AreashopCommandBean {

    private static final CloudKey<String> KEY_NAME = CloudKey.of("name", String.class);
    private final CommandFlag<GeneralRegion> regionFlag;
    private final MessageBridge messageBridge;

    @Inject
    public SetDisplayNameCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        this.messageBridge = messageBridge;
        this.regionFlag = RegionParseUtil.createDefault(fileManager);
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if (target.hasPermission("areashop.setdisplayname") || target.hasPermission("areashop.setdisplayname.landlord")) {
            return "help-setdisplayname";
        }
        return null;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("setdisplayname")
                .optional(KEY_NAME, StringParser.greedyFlagYieldingStringParser())
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("setdisplayname");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.setdisplayname") && (!sender.hasPermission("areashop.setdisplayname.landlord") && sender instanceof Player)) {
            this.messageBridge.message(sender, "setdisplayname-noPermission");
            return;
        }
        GeneralRegion region = RegionParseUtil.getOrParseRegion(context, sender, this.regionFlag);
        if (!sender.hasPermission("areashop.setdisplayname")
                && !(sender instanceof Player player && region.isLandlord(player.getUniqueId()))) {
            this.messageBridge.message(sender, "setdisplayname-noLandlord", region);
            return;
        }
        String displayName = context.getOrDefault(KEY_NAME, "").strip();
        if (displayName.isEmpty() || "reset".equalsIgnoreCase(displayName)) {
            region.setDisplayName(null);
            region.update();
            this.messageBridge.message(sender, "setdisplayname-successRemoved", region);
            return;
        }
        region.setDisplayName(displayName);
        region.update();
        this.messageBridge.message(sender, "setdisplayname-success", region);
    }

}
