package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

public class SendMessageCmd extends SubCmd {

    public SendMessageCmd(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.OWNER, CommandPermission.OWNER, "send_message",
                "Send a private message to a user");
        this.addOption(option -> option.name("user")
                .description("The user to send a message to")
                .required(true)
                .type(ApplicationCommandOption.Type.USER.getValue()));
        this.addOption(option -> option.name("message")
                .description("The message to send")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getOptionAsUser("user")
                .switchIfEmpty(Mono.error(new CommandException("User not found.")))
                .filter(user -> !user.getId().equals(context.getClient().getSelfId()))
                .switchIfEmpty(Mono.error(new CommandException("I can't send a private message to myself.")))
                .filter(user -> !user.isBot())
                .switchIfEmpty(Mono.error(new CommandException("I can't send private message to other bots.")))
                .flatMap(user -> {
                    final String message = context.getOptionAsString("message").orElseThrow();
                    return user.getPrivateChannel()
                            .cast(MessageChannel.class)
                            .flatMap(privateChannel -> DiscordUtil.sendMessage(message, privateChannel))
                            .onErrorMap(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                                    err -> new CommandException("I'm not allowed to send a private message to this user."))
                            .then(context.createFollowupMessage(Emoji.CHECK_MARK, "Message \"%s\" sent to **%s** (%s)."
                                    .formatted(message, user.getUsername(), user.getId().asString())));
                });
    }

}
