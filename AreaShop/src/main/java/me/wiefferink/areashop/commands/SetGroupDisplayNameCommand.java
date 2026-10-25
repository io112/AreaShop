package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionGroupParser;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RegionGroup;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Singleton
public class SetGroupDisplayNameCommand extends AreashopCommandBean {

    private static final CloudKey<RegionGroup> KEY_GROUP = CloudKey.of("group", RegionGroup.class);
    private static final CloudKey<String> KEY_NAME = CloudKey.of("name", String.class);

    private final IFileManager fileManager;
    private final MessageBridge messageBridge;

    @Inject
    public SetGroupDisplayNameCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.setdisplayname")) {
            return "help-setgroupdisplayname";
        }
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        ParserDescriptor<CommandSource<?>, RegionGroup> regionGroupParser = ParserDescriptor.of(
                new RegionGroupParser<>(this.fileManager, "setgroupdisplayname-noGroup"),
                RegionGroup.class);
        return builder
                .literal("setgroupdisplayname")
                .required(KEY_GROUP, regionGroupParser)
                .optional(KEY_NAME, StringParser.greedyStringParser())
                .handler(this::handleCommand);
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("setgroupdisplayname");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        if (!context.hasPermission("areashop.setdisplayname")) {
            throw new AreaShopCommandException("setgroupdisplayname-noPermission");
        }
        RegionGroup group = context.get(KEY_GROUP);
        String displayName = SetDisplayNameCommand.stripQuotes(context.getOrDefault(KEY_NAME, "").strip());
        if (displayName.isEmpty() || "reset".equalsIgnoreCase(displayName)) {
            group.setDisplayName(null);
            this.messageBridge.message(context.sender().sender(), "setgroupdisplayname-successRemoved", group.getName(), group.getDisplayName());
            return;
        }
        group.setDisplayName(displayName);
        this.messageBridge.message(context.sender().sender(), "setgroupdisplayname-success", group.getName(), group.getDisplayName());
    }

}
