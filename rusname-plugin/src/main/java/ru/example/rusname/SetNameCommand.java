package ru.example.rusname;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

/**
 * /setname <Имя> <Фамилия> - позволяет игроку сменить своё имя и фамилию вручную.
 */
public class SetNameCommand implements CommandExecutor {

    private static final Pattern WORD_PATTERN =
            Pattern.compile("^[А-ЯЁ][а-яё]+(-[А-ЯЁ][а-яё]+)?$");

    private final RusNamePlugin plugin;
    private final NameStorage storage;
    private final NameListener listener;

    public SetNameCommand(RusNamePlugin plugin, NameStorage storage, NameListener listener) {
        this.plugin = plugin;
        this.storage = storage;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эту команду может использовать только игрок.");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(Component.text("Использование: /setname <Имя> <Фамилия>", NamedTextColor.RED));
            return true;
        }

        String firstRaw = args[0];
        String lastRaw = args[1];
        String firstName = capitalize(firstRaw);
        String lastName = capitalize(lastRaw);

        if (!WORD_PATTERN.matcher(firstName).matches() || !WORD_PATTERN.matcher(lastName).matches()) {
            player.sendMessage(Component.text(
                    "Имя и фамилия должны состоять из русских букв, например: Иван Петров",
                    NamedTextColor.RED));
            return true;
        }

        storage.setFullName(player.getUniqueId(), firstName, lastName);
        listener.applyName(player, firstName + " " + lastName);
        player.sendMessage(Component.text(
                "Имя изменено на: " + firstName + " " + lastName, NamedTextColor.GREEN));
        return true;
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
}
