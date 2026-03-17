package systems.courant.sd.app;

import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;

import systems.courant.sd.app.canvas.CommandPalette;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single registry of all application commands. Each command is defined once
 * and can be materialized as both a {@link MenuItem} and a
 * {@link CommandPalette.Command}, eliminating duplication between the menu bar
 * builders and the command palette supplier.
 */
final class CommandRegistry {

    record CommandEntry(String name, String category, Runnable action, KeyCombination accelerator) {

        CommandEntry(String name, String category, Runnable action) {
            this(name, category, action, null);
        }
    }

    private final Map<String, CommandEntry> entries = new LinkedHashMap<>();

    void add(String name, String category, Runnable action) {
        entries.put(name, new CommandEntry(name, category, action));
    }

    void add(String name, String category, Runnable action, KeyCombination accelerator) {
        entries.put(name, new CommandEntry(name, category, action, accelerator));
    }

    /**
     * Creates a {@link MenuItem} for the named command. Sets the action and
     * accelerator from the registry entry.
     *
     * @throws IllegalArgumentException if no entry with that name exists
     */
    MenuItem toMenuItem(String name) {
        CommandEntry entry = entries.get(name);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown command: " + name);
        }
        MenuItem item = new MenuItem(name);
        item.setOnAction(e -> entry.action().run());
        if (entry.accelerator() != null) {
            item.setAccelerator(entry.accelerator());
        }
        return item;
    }

    /**
     * Returns all registered commands as palette commands.
     */
    List<CommandPalette.Command> toPaletteCommands() {
        List<CommandPalette.Command> commands = new ArrayList<>(entries.size());
        for (CommandEntry entry : entries.values()) {
            commands.add(new CommandPalette.Command(entry.name(), entry.category(), entry.action()));
        }
        return commands;
    }
}
