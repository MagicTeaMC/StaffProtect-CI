package net.experience.powered.staffprotect.hooks;

import net.experience.powered.staffprotect.interfaces.Permission;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LuckPermsHook implements Permission {

    private final LuckPerms luckPerms;

    public LuckPermsHook() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
        } else {
            throw new IllegalStateException("LuckPerms is null, yet it was initialised");
        }
    }

    @Override
    public boolean hasPermission(final @NotNull UUID uuid, final @NotNull String permission) {
        final UserManager userManager = luckPerms.getUserManager();
        User user = userManager.getUser(uuid);
        if (user == null) {
            final CompletableFuture<User> future = userManager.loadUser(uuid);
            user = future.join();
        }
        final CachedPermissionData cachedData = user.getCachedData().getPermissionData();
        return cachedData.checkPermission(permission).asBoolean();
    }
}