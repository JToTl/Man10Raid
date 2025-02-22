package com.shojabon.man10raid;

import com.shojabon.man10raid.DataClass.RaidGame;
import com.shojabon.man10raid.DataClass.RaidPlayer;
import com.shojabon.man10raid.Enums.RaidState;
import com.shojabon.mcutils.Utils.BaseUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Man10RaidAPI {

    public RaidGame currentGame;

    public static HashMap<String, RaidGame> games = new HashMap<>();

    Man10Raid plugin;

    public Man10RaidAPI(Man10Raid plugin){
        this.plugin = plugin;
    }

    public boolean saveRaidGameConfig(RaidGame game){
        //path check
        File path = new File(plugin.getDataFolder() + File.separator + "games");
        if(!path.exists()) {
            if(!path.mkdirs()) return false;
        }

        File file = new File(plugin.getDataFolder() + File.separator + "games", game.gameName + ".yml");
        YamlConfiguration config= YamlConfiguration.loadConfiguration(file);

        // save data
        config.set("scheduledGames", game.scheduledGames);

        //time settings
        config.set("time.registration", game.registrationTime);
        config.set("time.preparation", game.preparationTime);
        config.set("time.inGame", game.inGameTime);
        config.set("time.endArea", game.endAreaTime);

        //locations
        config.set("locations.playerSpawn", game.playerSpawnPoints);
        config.set("locations.playerRespawn", game.playerRespawnLocations);
        config.set("locations.endArea", game.endArea);

        //settings
        config.set("settings.friendlyFire", game.friendlyFire);
        config.set("settings.revivesAllowed", game.revivesAllowed);
        config.set("settings.playersAllowed", game.playersAllowed);
        config.set("settings.maxPlayersAllowed", game.maxPlayersAllowed);
        config.set("settings.minimumPlayersToBegin", game.minimumPlayersToBegin);
        config.set("settings.neededWinCommand", game.neededWinCommand);

        //payout
        config.set("payout.totalDamage", game.totalDamagePayoutMultiplier);
        config.set("payout.totalProjectileDamage", game.totalProjectileDamagePayoutMultiplier);
        config.set("payout.totalHeal", game.totalHealPayoutMultiplier);
        config.set("payout.totalFriendlyFire", game.totalFriendlyFirePayoutMultiplier);

        //file check
        File customConfigFile = new File(plugin.getDataFolder() + File.separator + "games", game.gameName + ".yml");
        try {
            config.save(customConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public RaidGame getRaidGame(String name){
        File customConfigFile = new File(plugin.getDataFolder() + File.separator + "games", name + ".yml");
        if(!customConfigFile.exists()) return null;

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(customConfigFile);
        } catch (IOException | InvalidConfigurationException e) {
            return null;
        }

        RaidGame raid = new RaidGame(name, config);

        games.put(name, raid);
        return games.get(name);
    }

    //current game control

    public void endGame(){
        Bukkit.getScheduler().runTask(plugin, ()-> {
            if(currentGame == null) return;

            //reset spawn points
            for(RaidPlayer player : currentGame.players.values()){
                Player p = player.getPlayer();
                if(p == null) continue;
                p.setBedSpawnLocation(Man10Raid.lobbyLocation);
            }

            currentGame.teleportAllPlayersToLobby();
            currentGame.setGameState(RaidState.INACTIVE);
            currentGame = null;
            Man10Raid.whitelist.disable();
        });
    }

    //command script

    public void executeScript(ArrayList<String> script){
        Man10Raid.threadPool.submit(() -> {
           for(String code: script){
               String[] args = code.split(" ");

               //sleep function
               if(args[0].equalsIgnoreCase("sleep") && args.length == 2 && BaseUtils.isInt(args[1])){
                   try {
                       Thread.sleep(1000L * Integer.parseInt(args[1]));
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   }
                   continue;
               }

               //dialog function
               if(args[0].equalsIgnoreCase("dialog") && args.length >= 3 && BaseUtils.isInt(args[1])){
                   StringBuilder dialogString = new StringBuilder();
                   for(int i = 2; i < args.length; i++){
                       dialogString.append(args[i]);
                       dialogString.append(" ");
                   }
                   Man10RaidAPI.broadcastDialog(dialogString.toString(), Integer.parseInt(args[1]));
                   try {
                       Thread.sleep(1000L * Integer.parseInt(args[1]));
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   }
                   continue;
               }

               //command function
               Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), code));
           }
        });
    }

    //clear cache

    public void clearCache(){
        games.clear();
    }

    //move player

    public void sendPlayerToServer(String name, String server){
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("ConnectOther");
            out.writeUTF(name);
            out.writeUTF(server);
        } catch (IOException eee) {
            eee.printStackTrace();
        }
        plugin.getServer().sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
    }

    //text functions

    public static void sendHighlightedMessage(Player p, String message){
        p.sendMessage("§e§l=================================");
        p.sendMessage("");
        p.sendMessage(message);
        p.sendMessage("");
        p.sendMessage("§e§l=================================");
    }

    public static void broadcastHighlightedMessage(String message){
        Bukkit.broadcastMessage("§e§l=================================");
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(message);
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§e§l=================================");
    }

    public static void broadcastDialog(String message, int seconds){
        Man10Raid.threadPool.submit(() -> {
            for(int i = 0; i < seconds*5; i++){
                for(int ii = 0; ii < 25; ii++){
                    Bukkit.broadcastMessage("");
                }

                Bukkit.broadcastMessage("§b=================================");
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(message);
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage("§b=================================");

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });
    }


}
