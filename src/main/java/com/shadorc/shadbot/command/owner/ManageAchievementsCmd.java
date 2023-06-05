package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.database.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

public class ManageAchievementsCmd extends SubCmd {

    private enum Action {
        ADD, REMOVE
    }

    public ManageAchievementsCmd(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.OWNER, CommandPermission.OWNER, "manage_achievements",
                "Manage user's achievements");
        this.addOption(option -> option.name("action")
                .description("Whether to add or remove an achievment")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .choices(DiscordUtil.toOptions(Action.class)));
        this.addOption(option -> option.name("achievement")
                .description("The achievement")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .choices(DiscordUtil.toOptions(Achievement.class)));
        this.addOption("user", "The user", true, ApplicationCommandOption.Type.USER);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Achievement achievement = context.getOptionAsEnum(Achievement.class, "achievement").orElseThrow();

        return context.getOptionAsUser("user")
                .switchIfEmpty(Mono.error(new CommandException("User not found.")))
                .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                        err -> Mono.empty())
                .flatMap(user -> DatabaseManager.getUsers().getDBUser(user.getId())
                        .flatMap(dbUser -> switch (action) {
                            case ADD -> dbUser.unlockAchievement(achievement);
                            case REMOVE -> dbUser.lockAchievement(achievement);
                        })
                        .then(context.createFollowupMessage(Emoji.CHECK_MARK, "%s achievement **%s** to **%s** done."
                                .formatted(FormatUtil.capitalizeEnum(action), achievement.getTitle(context),
                                        user.getTag()))));
    }

}
