package com.Axe.Ruler;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Main extends JavaPlugin {

    private PlayerDatabase db;
    private YamlConfiguration rulesConfig;
    private MiniMessage mm = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        this.db = new PlayerDatabase(getDataFolder());
        loadRulesYaml();
        getServer().getPluginManager().registerEvents(new RulesListener(db, rulesConfig, mm), this);
    }

    @Override
    public void onDisable() {
        db.close();
    }

    private void loadRulesYaml() {
        File file = new File(getDataFolder(), "rules.yml");
        if (!file.exists()) {
            saveResource("rules.yml", false);
        }

        rulesConfig = YamlConfiguration.loadConfiguration(file);
    }

}