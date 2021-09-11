package com.shojabon.man10raid.DataClass;

import com.shojabon.man10raid.DataClass.States.EndRegisterState;
import com.shojabon.man10raid.DataClass.States.RegisteringState;
import com.shojabon.man10raid.Enums.RaidState;
import com.shojabon.man10raid.Man10Raid;
import com.shojabon.man10raid.Utils.BaseUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class RaidGame {

    public RaidState currentGameState = RaidState.INACTIVE;
    public RaidStateData currentGameStateData;


    // raid settings

    public UUID gameId;

    public String gameName;

    public int scheduledGames = 0;
    public int currentGame = 0;

    public int gameTime = 0;
    public int currentGameTime = 0;

    public ArrayList<Location> playerSpawnPoints = new ArrayList<>();
    public Location endArea = null;

    public boolean friendlyFire = false;

    public int revivesAllowed = 0;


    public int playersAllowed = 50;
    public int minimumPlayersToBegin = 0;
    public int maxPlayersAllowed = 55;
    public HashMap<UUID, RaidPlayer> players = new HashMap<>();

    // constructors

    public RaidGame(){}

    public RaidGame(String name, FileConfiguration config){
        scheduledGames = config.getInt("scheduledGames");
        gameTime = config.getInt("gameTime");
        playerSpawnPoints = (ArrayList<Location>) config.getList("locations.playerSpawn", new ArrayList<Location>());
        endArea = config.getLocation("locations.endArea");
        friendlyFire = config.getBoolean("settings.friendlyFire");
        revivesAllowed = config.getInt("settings.revivesAllowed");
        playersAllowed = config.getInt("settings.playersAllowed");
        minimumPlayersToBegin = config.getInt("settings.minimumPlayersToBegin");
        maxPlayersAllowed = config.getInt("settings.maxPlayersAllowed");
    }


    // state functions

    public boolean changeGameState(RaidState state){
        if(state == currentGameState) return true;

        currentGameState = state;
        //stop current state
        if(currentGameStateData != null){
            currentGameStateData.beforeEnd();
        }

        //start next state
        RaidStateData data = getStateData(state);
        if(data == null) return true;
        data.beforeStart();
        //set current state data
        currentGameStateData = data;
        return true;
    }

    public RaidStateData getStateData(RaidState state){
        switch (state){
            case REGISTERING:
                return new RegisteringState();
            case END_REGISTRATION:
                return new EndRegisterState();
        }
        return null;
    }

    //registration function

    public boolean registerPlayer(Player p, boolean bypass){
        if(currentGameState != RaidState.REGISTERING && !bypass){
            p.sendMessage(Man10Raid.prefix + "§c§l現在選手登録をすることはできません");
            return false;
        }
        if(players.containsKey(p.getUniqueId())) {
            p.sendMessage(Man10Raid.prefix + "§c§lあなたはすでに登録されています");
            return false;
        }
        players.put(p.getUniqueId(), new RaidPlayer(p.getName(), p.getUniqueId()));
        p.sendMessage(Man10Raid.prefix + "§a§l登録しました");
        return true;
    }

    public void dividePlayers(){
        ArrayList<UUID> registeredPlayers = new ArrayList<>(players.keySet());
        Collections.shuffle(registeredPlayers);

        int maxGames = players.size()/playersAllowed+1;

        if(maxGames > scheduledGames && scheduledGames != -1) maxGames = scheduledGames; //if maxGames bigger than scheduled games and not all player game

        for(int game = 0; game < maxGames; game++){

            // if total player bigger than game size
            int playerPerGame = players.size();
            if(playerPerGame > playersAllowed) playerPerGame = playersAllowed;

            for(int i = 0; i < playerPerGame; i++){
                RaidPlayer player = players.get(registeredPlayers.get(i));
                player.registeredGame = game;
            }
        }
    }





}
