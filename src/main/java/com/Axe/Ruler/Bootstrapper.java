package com.Axe.Ruler;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Bootstrapper implements PluginBootstrap {

    private Path dataDir;
    private YamlConfiguration rulesConfig;

    @Override
    public void bootstrap(BootstrapContext context) {
        this.dataDir = context.getDataDirectory();
        loadRulesConfig();


        MiniMessage mm = MiniMessage.miniMessage();

        // Parse title
        Component title = mm.deserialize(rulesConfig.getString("title", "<white>Rules"));

        // Parse rules
        List<String> rules = (List<String>) rulesConfig.getList("rules");
        List<DialogBody> ruleBodies = rules.stream()
                .map(line -> (DialogBody) DialogBody.plainMessage(mm.deserialize(line)))
                .toList();

        // Parse buttons
        var buttonsSection = rulesConfig.getConfigurationSection("buttons");
        Map<String, Object> accept = buttonsSection.getConfigurationSection("accept").getValues(false);
        Map<String, Object> disagree = buttonsSection.getConfigurationSection("disagree").getValues(false);

        ActionButton acceptBtn = ActionButton.builder(mm.deserialize((String) accept.get("text")))
                .tooltip(mm.deserialize((String) accept.get("tooltip")))
                .action(DialogAction.customClick(Key.key("ruler:rules/agree"), null))
                .build();

        ActionButton disagreeBtn = ActionButton.builder(mm.deserialize((String) disagree.get("text")))
                .tooltip(mm.deserialize((String) disagree.get("tooltip")))
                .action(DialogAction.customClick(Key.key("ruler:rules/disagree"), null))
                .build();

        // Register dialog
        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose(),
                e -> e.registry().register(
                        DialogKeys.create(Key.key("ruler:rules")),
                        builder -> builder
                                .base(DialogBase.builder(title)
                                        .canCloseWithEscape(false)
                                        .body(ruleBodies)
                                        .build()
                                )
                                .type(DialogType.confirmation(acceptBtn, disagreeBtn))
                )
        );

        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(
                    Commands.literal("ruler")
                            .then(Commands.literal("reload")
                                    .executes(ctx -> {
                                        loadRulesConfig();
                                        return 1;
                                    }))
                            .build()
            );
        });
    }

    private void loadRulesConfig() {
        Path rulesFile = dataDir.resolve("rules.yml");
        try {
            Files.createDirectories(dataDir);
            if (Files.notExists(rulesFile)) {
                try (InputStream in = getClass().getClassLoader().getResourceAsStream("rules.yml")) {
                    if (in == null) throw new IllegalStateException("Missing rules.yml in jar resources");
                    Files.copy(in, rulesFile);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to setup rules.yml", ex);
        }
        rulesConfig = YamlConfiguration.loadConfiguration(rulesFile.toFile());
    }

}