package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
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
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class SetDisplayNameCommand extends AreashopCommandBean {

    private static final CloudKey<String> KEY_NAME = CloudKey.of("name", String.class);
    private static final Pattern REGION_FLAG = Pattern.compile("(?:^|\\s)--region(?:\\s+(\\S+)|\\s*$)");
    private final IFileManager fileManager;
    private final MessageBridge messageBridge;

    @Inject
    public SetDisplayNameCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
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
        // The name is free text (spaces, Cyrillic, quotes), so '--region <region>' is taken out of it by hand:
        // a cloud flag only works after the name and fails silently when it is written first
        return builder.literal("setdisplayname")
                .optional(KEY_NAME, StringParser.greedyStringParser())
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
        String input = context.getOrDefault(KEY_NAME, "");
        String regionName = null;
        Matcher matcher = REGION_FLAG.matcher(input);
        if (matcher.find()) {
            regionName = matcher.group(1);
            if (regionName == null) {
                throw new AreaShopCommandException("setdisplayname-help");
            }
            input = input.substring(0, matcher.start()) + " " + input.substring(matcher.end());
        }
        GeneralRegion region;
        if (regionName != null) {
            region = this.fileManager.getRegion(regionName);
            if (region == null) {
                throw new AreaShopCommandException("cmd-notRegistered", regionName);
            }
        } else {
            region = RegionParseUtil.getOrParseRegion(context, sender, CloudKey.of("region", GeneralRegion.class));
        }
        if (!sender.hasPermission("areashop.setdisplayname")
                && !(sender instanceof Player player && region.isLandlord(player.getUniqueId()))) {
            this.messageBridge.message(sender, "setdisplayname-noLandlord", region);
            return;
        }
        String displayName = stripQuotes(input.strip());
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

    /**
     * Remove one pair of quotes around a name, players often quote names with spaces.
     * @param name The name to strip
     * @return The name without surrounding quotes
     */
    @Nonnull
    static String stripQuotes(@Nonnull String name) {
        if (name.length() >= 2
                && (name.startsWith("\"") && name.endsWith("\"") || name.startsWith("'") && name.endsWith("'"))) {
            return name.substring(1, name.length() - 1).strip();
        }
        return name;
    }

}
