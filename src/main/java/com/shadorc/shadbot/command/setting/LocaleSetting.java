package com.shadorc.shadbot.command.setting;

import com.shadorc.shadbot.core.command.*;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import discord4j.core.object.command.ApplicationCommandOption;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LocaleSetting extends SubCmd {

    public LocaleSetting(final GroupCmd groupCmd) {
        super(groupCmd, CommandCategory.SETTING, "language", "Manage default server language");

        final List<String> locales = Arrays.stream(I18nManager.LOCALES)
                .map(Locale::toLanguageTag)
                .collect(Collectors.toList());

        this.addOption(option -> option.name("value")
                .description("The default language of the server")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .required(true)
                .choices(DiscordUtil.toOptions(locales)));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String locale = context.getOptionAsString("value").orElseThrow();
        return context.getDbGuild()
                .updateSetting(Setting.LOCALE, locale)
                .then(context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("setting.locale.message")
                        .formatted(locale)));
    }
}
