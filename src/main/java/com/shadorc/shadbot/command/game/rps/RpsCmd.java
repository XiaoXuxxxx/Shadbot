package com.shadorc.shadbot.command.game.rps;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Snowflake;
import io.prometheus.client.Summary;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RpsCmd extends BaseCmd {

    private static final Summary RPS_SUMMARY = Summary.build()
            .name("game_rps")
            .help("RPS game")
            .labelNames("result")
            .register();
    private static final int GAINS = 300;

    private final Map<Tuple2<Snowflake, Snowflake>, RpsPlayer> players;

    public RpsCmd() {
        super(CommandCategory.GAME, List.of("rps"));
        this.setGameRateLimiter();
        this.players = new ConcurrentHashMap<>();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final String arg = context.requireArg();

        final Handsign userHandsign = Utils.parseEnum(Handsign.class, arg,
                new CommandException(String.format("`%s` is not a valid handsign. %s.",
                        arg, FormatUtils.options(Handsign.class))));

        final Handsign botHandsign = Utils.randValue(Handsign.values());

        final StringBuilder strBuilder = new StringBuilder(String.format("**%s**: %s %s **VS** %s %s :**Shadbot**%n",
                context.getUsername(), userHandsign.getHandsign(), userHandsign.getEmoji(),
                botHandsign.getEmoji(), botHandsign.getHandsign()));

        return Mono.just(this.getOrCreatePlayer(context))
                .flatMap(player -> {
                    if (userHandsign.isSuperior(botHandsign)) {
                        final int winStreak = player.getWinStreak().incrementAndGet();
                        final long gains = Math.min((long) GAINS * winStreak, Config.MAX_COINS);
                        RPS_SUMMARY.labels("win").observe(gains);
                        return player.win(gains)
                                .thenReturn(strBuilder.append(
                                        String.format(Emoji.BANK + " (**%s**) Well done, you win **%s** (Win Streak x%d)!",
                                                context.getUsername(), FormatUtils.coins(gains), player.getWinStreak().get())));
                    } else if (userHandsign == botHandsign) {
                        player.getWinStreak().set(0);
                        strBuilder.append("It's a draw.");
                    } else {
                        player.getWinStreak().set(0);
                        strBuilder.append("I win !");
                    }
                    return Mono.just(strBuilder);
                })
                .map(StringBuilder::toString)
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    private RpsPlayer getOrCreatePlayer(Context context) {
        return this.players.computeIfAbsent(Tuples.of(context.getGuildId(), context.getAuthorId()),
                TupleUtils.function(RpsPlayer::new));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Play a Rock–paper–scissors game.")
                .addArg("handsign", FormatUtils.format(Handsign.values(), Handsign::getHandsign, ", "), false)
                .addField("Gains", String.format("The winner gets **%s** multiplied by his win-streak.",
                        FormatUtils.coins(GAINS)), false)
                .build();
    }

}
