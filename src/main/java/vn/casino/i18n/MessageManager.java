package vn.casino.i18n;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import vn.casino.CasinoPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class MessageManager {

    private final CasinoPlugin plugin;
    private final MiniMessage miniMessage;

    @Getter
    private Locale currentLocale;
    private final Map<Locale, FileConfiguration> messages = new HashMap<>();

    public MessageManager(CasinoPlugin plugin, String languageCode) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.currentLocale = Locale.fromCode(languageCode);

        loadLanguageFiles();
    }

    private void loadLanguageFiles() {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        for (Locale locale : Locale.values()) {
            File langFile = new File(langDir, locale.getCode() + ".yml");

            if (!langFile.exists()) {
                try (InputStream in = plugin.getResource("lang/" + locale.getCode() + ".yml")) {
                    if (in != null) {
                        Files.copy(in, langFile.toPath());
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save default language file: " + locale.getCode(), e);
                }
            }

            if (langFile.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
                messages.put(locale, config);
                plugin.getLogger().info("Loaded language file: " + locale.getCode());
            }
        }
    }

    public void reload(String languageCode) {
        this.currentLocale = Locale.fromCode(languageCode);
        messages.clear();
        loadLanguageFiles();
    }

    public String getRawMessage(MessageKey key) {
        return getRawMessage(currentLocale, key);
    }

    public String getRawMessage(Locale locale, MessageKey key) {
        FileConfiguration config = messages.get(locale);
        if (config == null) {
            config = messages.get(Locale.VI);
        }

        String message = config.getString(key.getKey());
        if (message == null) {
            plugin.getLogger().warning("Missing translation for key: " + key.getKey() + " in locale: " + locale.getCode());
            return key.getKey();
        }

        return message;
    }

    public Component getMessage(MessageKey key) {
        return miniMessage.deserialize(getRawMessage(key));
    }

    public Component getMessage(MessageKey key, TagResolver... resolvers) {
        return miniMessage.deserialize(getRawMessage(key), resolvers);
    }

    public Component getMessage(MessageKey key, Map<String, String> placeholders) {
        String message = getRawMessage(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return miniMessage.deserialize(message);
    }

    public Component getMessageWithPrefix(MessageKey key) {
        Component prefix = getMessage(MessageKey.PREFIX);
        Component message = getMessage(key);
        return prefix.append(message);
    }

    public Component getMessageWithPrefix(MessageKey key, TagResolver... resolvers) {
        Component prefix = getMessage(MessageKey.PREFIX);
        Component message = getMessage(key, resolvers);
        return prefix.append(message);
    }

    public Component getMessageWithPrefix(MessageKey key, Map<String, String> placeholders) {
        Component prefix = getMessage(MessageKey.PREFIX);
        Component message = getMessage(key, placeholders);
        return prefix.append(message);
    }

    public void sendMessage(Player player, MessageKey key) {
        player.sendMessage(getMessageWithPrefix(key));
    }

    public void sendMessage(Player player, MessageKey key, TagResolver... resolvers) {
        player.sendMessage(getMessageWithPrefix(key, resolvers));
    }

    public void sendMessage(Player player, MessageKey key, Map<String, String> placeholders) {
        player.sendMessage(getMessageWithPrefix(key, placeholders));
    }

    public void sendRawMessage(Player player, MessageKey key) {
        player.sendMessage(getMessage(key));
    }

    public void sendRawMessage(Player player, MessageKey key, TagResolver... resolvers) {
        player.sendMessage(getMessage(key, resolvers));
    }

    public TagResolver placeholder(String key, String value) {
        return Placeholder.parsed(key, value);
    }

    public TagResolver placeholder(String key, Component value) {
        return Placeholder.component(key, value);
    }

    public Component parse(String miniMessageString) {
        return miniMessage.deserialize(miniMessageString);
    }

    public Component parse(String miniMessageString, TagResolver... resolvers) {
        return miniMessage.deserialize(miniMessageString, resolvers);
    }

    public String serialize(Component component) {
        return miniMessage.serialize(component);
    }
}
