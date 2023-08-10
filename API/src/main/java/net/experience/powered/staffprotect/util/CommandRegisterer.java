package net.experience.powered.staffprotect.util;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public class CommandRegisterer {

    private CommandMap commandMap;

    private CommandRegisterer(final @NotNull String bukkitName) {
        switch (bukkitName) {
            case "Paper" -> this.commandMap = Bukkit.getCommandMap();
            case "Spigot" -> { // Added for backwards compatibility for Spigot
                Field fCommandMap;
                try {
                    fCommandMap = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
                    fCommandMap.setAccessible(true);
                    final Object commandMapObject = fCommandMap.get(Bukkit.getPluginManager());
                    if (commandMapObject instanceof CommandMap) {
                        this.commandMap = (CommandMap) commandMapObject;
                    }
                } catch (final NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            default -> {
                Bukkit.getLogger().warning("You're running on unsupported version.");
                try {
                    this.commandMap = Bukkit.getCommandMap();
                } catch (final Exception e) {
                    new CommandRegisterer("Spigot");
                }
            }
        }
    }

    @Contract(" -> new")
    public static @NotNull CommandRegisterer getInstance() {
        return new CommandRegisterer(Bukkit.getName());
    }

    public void register(final @NotNull Command command) {
        commandMap.register("staffProtect", command);
    }
}