package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.entity.Role;
import discord4j.rest.util.Permission;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AutoRolesSetting extends SubCmd {

    private enum Action {
        ADD, REMOVE
    }

    public AutoRolesSetting(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.SETTING, CommandPermission.ADMIN,
                "auto_roles", "Manage auto assigned role(s)");

        this.addOption(option -> option.name("action")
                .description("Whether to add or remove a role from the auto ones")
                .required(true)
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .choices(DiscordUtil.toOptions(Action.class)));
        this.addOption("role1", "The first role", true, ApplicationCommandOption.Type.ROLE);
        this.addOption("role2", "The second role", false, ApplicationCommandOption.Type.ROLE);
        this.addOption("role3", "The third role", false, ApplicationCommandOption.Type.ROLE);
        this.addOption("role4", "The fourth role", false, ApplicationCommandOption.Type.ROLE);
        this.addOption("role5", "The fifth role", false, ApplicationCommandOption.Type.ROLE);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Flux<Role> roles = Flux.fromStream(IntStream.range(1, 6).boxed())
                .flatMap(index -> context.getOptionAsRole("role%d".formatted(index)));

        return roles.collectList()
                .flatMap(mentionedRoles -> {
                    final Set<Snowflake> roleIds = mentionedRoles.stream()
                            .map(Role::getId)
                            .collect(Collectors.toSet());
                    final Set<Snowflake> autoRoleIds = context.getDbGuild().getSettings().getAutoRoleIds();
                    final String roleStr = FormatUtil.format(mentionedRoles,
                            role -> "`@%s`".formatted(role.getName()), ", ");

                    return switch (action) {
                        case ADD -> AutoRolesSetting.checkPermissions(context, roleIds)
                                .then(Mono.defer(() -> {
                                    if (!autoRoleIds.addAll(roleIds)) {
                                        return context.createFollowupMessage(Emoji.GREY_EXCLAMATION,
                                                context.localize("autoroles.already.added"));
                                    }
                                    return context.getDbGuild().updateSetting(Setting.AUTO_ROLES, autoRoleIds)
                                            .then(context.createFollowupMessage(Emoji.CHECK_MARK,
                                                    context.localize("autoroles.added").formatted(roleStr)));
                                }));
                        case REMOVE -> Mono.defer(() -> {
                            if (!autoRoleIds.removeAll(roleIds)) {
                                return context.createFollowupMessage(Emoji.GREY_EXCLAMATION,
                                        context.localize("autoroles.already.removed"));
                            }
                            return context.getDbGuild().updateSetting(Setting.AUTO_ROLES, autoRoleIds)
                                    .then(context.createFollowupMessage(Emoji.CHECK_MARK,
                                            context.localize("autoroles.removed").formatted(roleStr)));
                        });
                    };
                });
    }

    private static Mono<Void> checkPermissions(Context context, Set<Snowflake> roleIds) {
        return context.getChannel()
                .flatMap(channel -> DiscordUtil.requirePermissions(channel, Permission.MANAGE_ROLES))
                .then(context.getClient().getSelfMember(context.getGuildId()))
                .filterWhen(self -> self.hasHigherRoles(roleIds))
                .switchIfEmpty(Mono.error(new CommandException(context.localize("autoroles.exception.higher"))))
                .then();
    }

}
