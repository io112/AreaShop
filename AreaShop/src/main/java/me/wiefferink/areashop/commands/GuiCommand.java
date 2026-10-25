package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.PlayerCommandSource;
import me.wiefferink.areashop.gui.RentGui;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Singleton
public class GuiCommand extends AreashopCommandBean {

    private final RentGui rentGui;

    @Inject
    public GuiCommand(@Nonnull RentGui rentGui) {
        this.rentGui = rentGui;
    }

    @Override
    public String stringDescription() {
        return "Opens a menu with the regions you rent";
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.gui")) {
            return "help-gui";
        }
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("gui", "myregions")
                .senderType(PlayerCommandSource.class)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("gui", "myregions");
    }

    private void handleCommand(@Nonnull CommandContext<PlayerCommandSource> context) {
        if (!context.hasPermission("areashop.gui")) {
            throw new AreaShopCommandException("gui-noPermission");
        }
        this.rentGui.open(context.sender().sender(), 0);
    }
}
