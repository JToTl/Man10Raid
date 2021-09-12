package com.shojabon.man10raid;

import com.shojabon.man10raid.Commands.Man10RaidCommand;
import com.shojabon.man10raid.DataClass.RaidGame;
import com.shojabon.man10raid.Utils.MySQL.ThreadedMySQLAPI;
import com.shojabon.man10raid.Utils.STimer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.html.parser.Entity;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Man10Raid extends JavaPlugin implements @NotNull Listener {

    public static ExecutorService threadPool = Executors.newCachedThreadPool();
    public static Man10RaidAPI api;
    public static String prefix;
    public static ThreadedMySQLAPI mysql;

    public static FileConfiguration config;

    public static Location lobbyLocation;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Plugin startup logic
        api = new Man10RaidAPI(this);
        //

        config = getConfig();
        lobbyLocation = config.getLocation("lobbyLocation");
        prefix = config.getString("prefix");

        mysql = new ThreadedMySQLAPI(this);
        Man10RaidCommand command = new Man10RaidCommand(this);
        getCommand("mraid").setExecutor(command);
        getCommand("mraid").setTabCompleter(command);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        STimer.pluginEnabled = false;
    }

}
