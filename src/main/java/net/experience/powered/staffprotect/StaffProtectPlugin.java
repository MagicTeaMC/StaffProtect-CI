package net.experience.powered.staffprotect;

import net.experience.powered.staffprotect.commands.StaffProtectCommand;
import net.experience.powered.staffprotect.impl.AddonManagerImpl;
import net.experience.powered.staffprotect.impl.StaffProtectImpl;
import net.experience.powered.staffprotect.listeners.InventoryListener;
import net.experience.powered.staffprotect.listeners.PlayerListener;
import net.experience.powered.staffprotect.notification.NotificationBus;
import net.experience.powered.staffprotect.notification.NotificationManager;
import net.experience.powered.staffprotect.records.Record;
import net.experience.powered.staffprotect.records.RecordFile;
import net.experience.powered.staffprotect.util.Counter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

public final class StaffProtectPlugin extends JavaPlugin {

    private Metrics metrics;
    private VersionController versionController;
    private StaffProtect api;

    @Override
    public void onEnable() {
        this.versionController = new VersionController(getDataFolder());

        String info = " (Git: " +
                versionController.getGitHash() +
                ", branch " +
                versionController.getGitBranchName() +
                ")";
        getLogger().info(info);

        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveResource("config.yml", false);
        }

        api = getStaffProtectAPI();
        getNotificationManager();
        Bukkit.getServicesManager().register(StaffProtect.class, api, this, ServicePriority.Normal);
        ((AddonManagerImpl) api.getAddonManager()).enableAddons();

        final PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new InventoryListener(api), this);
        pluginManager.registerEvents(new PlayerListener(api), this);

        api.getCommandManager().register(new StaffProtectCommand(api));

        File file;
        {
            final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy");
            final String string = getConfig().getString("zoneId", "");
            final ZoneId zoneId = string.equalsIgnoreCase("") ? ZoneId.systemDefault() : ZoneId.of(string);
            final Instant instant = Instant.now(Clock.system(zoneId));
            final String date = dateFormat.format(Date.from(instant));
            final Counter counter = new Counter();
            file = new File(RecordFile.folder, date + ".txt");
            try {
                while (!file.createNewFile()) {
                    file = new File(RecordFile.folder, date + "-" + counter + ".txt");
                    counter.increment();
                }
            } catch (IOException e) {
                getLogger().severe("Could not create record file : " + file);
                e.printStackTrace();
            }
        }
        final RecordFile recordFile = new RecordFile(file);
        recordFile.writeRecord(new Record(System.currentTimeMillis(), "Sever", "StaffProtect was enabled."));

        metrics = new Metrics(this, 19629);
        metrics.addCustomChart(new Metrics.SingleLineChart("amount_of_addons", () -> api.getAddonManager().getAddons().size()));
    }

    @Override
    public void onDisable() {
        final RecordFile recordFile = RecordFile.getInstance();
        if (recordFile != null) {
            recordFile.writeRecord(new Record(System.currentTimeMillis(), "Sever", "StaffProtect was disabled."));
            recordFile.writeRecord(new Record(System.currentTimeMillis(), "StaffProtect", "Saved file."));
        }

        metrics.shutdown();
        if (api == null) return;
        ((AddonManagerImpl) api.getAddonManager()).disableAddons();
    }

    @NotNull
    private StaffProtect getStaffProtectAPI() {
        final NotificationBus bus = new NotificationBus() {

            private final List<UUID> subscribers = new ArrayList<>();

            @Override
            public void subscribe(final @NotNull UUID uuid) {
                subscribers.add(uuid);
            }

            @Override
            public void unsubscribe(final @NotNull UUID uuid) {
                subscribers.remove(uuid);
            }

            @Override
            public @NotNull @UnmodifiableView List<UUID> getSubscribers() {
                return Collections.unmodifiableList(subscribers);
            }
        };
        final StaffProtect staffProtect = new StaffProtectImpl(this, bus);
        new StaffProtectProvider(staffProtect);
        return staffProtect;
    }

    private void getNotificationManager() {
        new NotificationManager(this, api.getNotificationBus()) {
            @Override
            public void sendMessage(final @Nullable String player, final @NotNull Component component) {
                final BukkitAudiences audience = BukkitAudiences.create(api.getPlugin());
                bus.getSubscribers().forEach(uuid -> audience.player(uuid).sendMessage(component));
                sendQuietMessage(player, PlainTextComponentSerializer.plainText().serialize(component));
            }

            @Override
            public void sendQuietMessage(final @Nullable String player, final @NotNull String string) {
                RecordFile.getInstance().writeRecord(new Record(System.currentTimeMillis(), player == null ? "Anonymous" : player, string));
            }
        };
    }
}
