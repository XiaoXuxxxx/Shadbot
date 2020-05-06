package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class StopCmd extends BaseCmd {

    public StopCmd() {
        super(CommandCategory.MUSIC, List.of("stop"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        return Mono.just(context.requireGuildMusic())
                .map(ignored -> context.getClient().getVoiceConnectionRegistry())
                .flatMap(registry -> registry.getVoiceConnection(context.getGuildId().asLong()))
                .flatMap(VoiceConnection::disconnect)
                .then(context.getChannel())
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.STOP_BUTTON + " Music stopped by **%s**.",
                        context.getUsername()), channel))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Stop music.")
                .build();
    }
}