package ru.example.rusname;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Требует ввод имени и фамилии в чате при первом входе, применяет их
 * к отображаемому имени игрока (чат и таб) и рендерит сообщения в чате
 * с этим именем вместо ника.
 */
public class NameListener implements Listener {

    // Одно слово: заглавная кириллическая буква + строчные буквы,
    // допускается двойное имя через дефис, например "Анна-Мария".
    private static final Pattern WORD_PATTERN =
            Pattern.compile("^[А-ЯЁ][а-яё]+(-[А-ЯЁ][а-яё]+)?$");

    private final RusNamePlugin plugin;
    private final NameStorage storage;

    // Игроки, которые сейчас должны ввести имя в чат
    private final Set<UUID> awaitingName = ConcurrentHashMap.newKeySet();

    public NameListener(RusNamePlugin plugin, NameStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (storage.hasName(uuid)) {
            applyName(player, storage.getFullName(uuid));
            event.joinMessage(null);
        } else {
            event.joinMessage(null);
            beginNameRequest(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        awaitingName.remove(event.getPlayer().getUniqueId());
    }

    private void beginNameRequest(Player player) {
        awaitingName.add(player.getUniqueId());
        player.sendMessage(Component.text("Добро пожаловать на сервер!", NamedTextColor.GOLD));
        player.sendMessage(Component.text(
                "Напишите в чат ваши Имя и Фамилию на русском языке, через пробел.",
                NamedTextColor.YELLOW));
        player.sendMessage(Component.text(
                "Пример: Иван Петров", NamedTextColor.GRAY));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (awaitingName.contains(uuid)) {
            // Пока идёт ввод имени - сообщение не должно попасть в общий чат
            event.setCancelled(true);

            String message = plainText(event.message()).trim();
            String[] parts = message.split("\\s+");

            if (parts.length != 2 || !WORD_PATTERN.matcher(parts[0]).matches()
                    || !WORD_PATTERN.matcher(parts[1]).matches()) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        player.sendMessage(Component.text(
                                "Неверный формат. Введите Имя и Фамилию на русском, например: Иван Петров",
                                NamedTextColor.RED)));
                return;
            }

            String firstName = capitalize(parts[0]);
            String lastName = capitalize(parts[1]);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                storage.setFullName(uuid, firstName, lastName);
                awaitingName.remove(uuid);
                applyName(player, firstName + " " + lastName);
                player.sendMessage(Component.text(
                        "Готово! Ваше имя: " + firstName + " " + lastName, NamedTextColor.GREEN));
            });
            return;
        }

        // Обычное сообщение - подменяем отображаемое имя в чате на Имя Фамилию
        String fullName = storage.getFullName(uuid);
        if (fullName != null) {
            event.renderer((source, sourceDisplayName, message, viewer) ->
                    Component.text()
                            .append(Component.text(fullName, NamedTextColor.WHITE))
                            .append(Component.text(": ", NamedTextColor.GRAY))
                            .append(message)
                            .build());
        }
    }

    // Блокируем большинство действий, пока имя не введено
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (isAwaiting(event.getPlayer())
                && hasMoved(event)) {
            event.setCancelled(true);
        }
    }

    private boolean hasMoved(PlayerMoveEvent event) {
        if (event.getFrom() == null || event.getTo() == null) return false;
        return event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (isAwaiting(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isAwaiting(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (isAwaiting(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (isAwaiting(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text(
                    "Сначала введите имя и фамилию в чат.", NamedTextColor.RED));
        }
    }

    public boolean isAwaiting(Player player) {
        return awaitingName.contains(player.getUniqueId());
    }

    public void forceReenterName(Player player) {
        beginNameRequest(player);
    }

    /** Применяет отображаемое имя игрока к его нику (чат-рендер) и к табу. */
    public void applyName(Player player, String fullName) {
        Component nameComponent = Component.text(fullName, NamedTextColor.WHITE);
        player.playerListName(nameComponent);
        player.displayName(nameComponent);
    }

    private static String capitalize(String word) {
        if (word.isEmpty()) return word;
        String lower = word.toLowerCase();
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : lower.toCharArray()) {
            if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
            if (c == '-') {
                capitalizeNext = true;
            }
        }
        return result.toString();
    }

    private static String plainText(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(component);
    }
}
