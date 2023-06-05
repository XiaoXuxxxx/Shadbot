package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.command.CommandProcessor;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.ReactorUtil;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Mono;

public class InteractionCreateListener implements EventListener<DeferrableInteractionEvent> {

    @Override
    public Class<DeferrableInteractionEvent> getEventType() {
        return DeferrableInteractionEvent.class;
    }

    @Override
    public Mono<?> execute(DeferrableInteractionEvent event) {
        Telemetry.INTERACTING_USERS.add(event.getInteraction().getUser().getId().asLong());

        if (event.getInteraction().getGuildId().isEmpty()) {
            return event.reply(I18nManager.localize(Config.DEFAULT_LOCALE, "interaction.dm"));
        }

        return event.getInteraction().getChannel()
                .ofType(TextChannel.class)
                .flatMap(channel -> channel.getEffectivePermissions(event.getClient().getSelfId()))
                .filterWhen(ReactorUtil.filterOrExecute(
                        permissions -> permissions.contains(Permission.SEND_MESSAGES)
                                && permissions.contains(Permission.VIEW_CHANNEL),
                        event.reply(Emoji.RED_CROSS
                                        + I18nManager.localize(Config.DEFAULT_LOCALE, "interaction.missing.permissions"))
                                .withEphemeral(true)))
                .flatMap(__ -> Mono.justOrEmpty(event.getInteraction().getGuildId()))
                .flatMap(guildId -> DatabaseManager.getGuilds().getDBGuild(guildId))
                .map(dbGuild -> new Context(event, dbGuild))
                .flatMap(CommandProcessor::processCommand);
    }

}
