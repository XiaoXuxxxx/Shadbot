package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.object.Emoji;
import discord4j.core.object.command.ApplicationCommandOption;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class EnableCommandCmd extends SubCmd {

    public EnableCommandCmd(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.OWNER, CommandPermission.OWNER, "enable_command",
                "Enable/disable a command");
        this.addOption(option -> option.name("command")
                .description("The command to enable/disable")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
        this.addOption(option -> option.name("enabled")
                .description("True to enable, false to disable")
                .required(true)
                .type(ApplicationCommandOption.Type.BOOLEAN.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String commandName = context.getOptionAsString("command").orElseThrow();
        final Cmd cmd = CommandManager.getCommand(commandName);
        if (cmd == null) {
            return Mono.error(new CommandException("Command `%s` not found.".formatted(commandName)));
        }

        final boolean enabled = context.getOptionAsBool("enabled").orElseThrow();
        cmd.setEnabled(enabled);

        final String enabledStr = enabled ? "enabled" : "disabled";
        DEFAULT_LOGGER.info("Command {} {}", cmd.getName(), enabledStr);

        return context.createFollowupMessage(Emoji.CHECK_MARK, "Command `%s` %s".formatted(commandName, enabledStr));
    }

}
