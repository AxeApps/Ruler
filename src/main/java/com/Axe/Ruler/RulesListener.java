package com.Axe.Ruler;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.papermc.paper.connection.PlayerCommonConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RulesListener implements Listener {

    private final PlayerDatabase  db;
    private final MiniMessage mm;
    private YamlConfiguration rulesConfig;

    private final Map<PlayerCommonConnection, CompletableFuture<Boolean>> awaitingResponse = new HashMap<>();

    public RulesListener(PlayerDatabase db, YamlConfiguration rulesConfig, MiniMessage mm) {
        this.db = db;
        this.mm = mm;
        this.rulesConfig = rulesConfig;
    }

    @EventHandler
    void onPlayerConfigure(AsyncPlayerConnectionConfigureEvent event) {
        PlayerProfile profile = event.getConnection().getProfile();

        if (db.playerExists(profile.getName(), profile.getId().toString())) {
            return;
        }

        Dialog dialog = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG).get(Key.key("ruler:rules"));
        if (dialog == null) {
            return;
        }

        CompletableFuture<Boolean> response = new CompletableFuture<>();

        awaitingResponse.put(event.getConnection(), response);

        event.getConnection().getAudience().showDialog(dialog);


        if (!response.join()) {
            event.getConnection().disconnect(mm.deserialize(rulesConfig.getString("disagree-message")));
        } else {
            db.addPlayer(profile.getName(), profile.getId().toString());
        }

        awaitingResponse.remove(event.getConnection());
    }

    @EventHandler
    void onHandleDialog(PlayerCustomClickEvent event) {
        Key key = event.getIdentifier();

        if (key.equals(Key.key("ruler:rules/disagree"))) {
            setConnectionJoinResult(event.getCommonConnection(), false);
        } else if (key.equals(Key.key("ruler:rules/agree"))) {
            setConnectionJoinResult(event.getCommonConnection(), true);
        }
    }

    private void setConnectionJoinResult(PlayerCommonConnection connection, boolean value) {
        CompletableFuture<Boolean> future = awaitingResponse.get(connection);
        if (future != null) {
            future.complete(value);
        }
    }

}