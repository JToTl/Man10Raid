package com.shojabon.man10raid.DataClass;

import com.shojabon.man10raid.DataClass.States.*;
import com.shojabon.man10raid.Enums.RaidState;
import com.shojabon.man10raid.Man10Raid;
import com.shojabon.mcutils.Utils.MySQL.MySQLAPI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class RaidGame {

    Plugin plugin = Bukkit.getPluginManager().getPlugin("Man10Raid");

    public RaidState currentGameState = RaidState.INACTIVE;
    public RaidStateData currentGameStateData;

    public boolean won = false;


    // raid settings

    public int neededWinCommand = 1;
    public int executedWinCommandCount = 0;

    public UUID gameId;

    public String gameName;

    public int scheduledGames = 0;
    public int currentGame = 0;

    public boolean removeLifeOnLogout = false;

    //time settings
    public int registrationTime = 0;
    public int preparationTime = 0;
    public int inGameTime = 0;
    public int endAreaTime = 0;

    //live time counter
    public int inGameTimeLeft = 0;

    //payout settings
    public double totalDamagePayoutMultiplier = 0;
    public double totalProjectileDamagePayoutMultiplier = 0;
    public double totalHealPayoutMultiplier = 0;
    public double totalFriendlyFirePayoutMultiplier = 0;



    //location settings
    public ArrayList<Location> playerSpawnPoints = new ArrayList<>();
    public Location endArea = null;
    public ArrayList<Location> playerRespawnLocations = new ArrayList<>();

    //game settings
    public boolean friendlyFire = false;
    public int revivesAllowed = 0;

    //player count settings
    public int playersAllowed = 50;
    public int minimumPlayersToBegin = 0;
    public int maxPlayersAllowed = 55;
    public HashMap<UUID, RaidPlayer> players = new HashMap<>();

    //commands
    public HashMap<RaidState, ArrayList<String>> commands = new HashMap<>();
    public ArrayList<String> winCommands = new ArrayList<>();
    public ArrayList<String> loseCommands = new ArrayList<>();

    // anti cheater
    public float mustBeAliveForPercentOfGame = 0.65f;
    public long totalGameTime = 0;
    public int mustBeInRanking=30;

    public ArrayList<Pattern> disabledDamageCountMobs = new ArrayList<>();


    // constructors

    public RaidGame(){}

    public RaidGame(String name, FileConfiguration config){
        this.gameName = name;
        scheduledGames = config.getInt("scheduledGames");

        //time settings
        registrationTime = config.getInt("time.registration");
        preparationTime = config.getInt("time.preparation");
        inGameTime = config.getInt("time.inGame");
        endAreaTime = config.getInt("time.endArea");

        //locations
        playerSpawnPoints = (ArrayList<Location>) config.getList("locations.playerSpawn", new ArrayList<Location>());
        playerRespawnLocations = (ArrayList<Location>) config.getList("locations.playerRespawn", new ArrayList<Location>());
        endArea = config.getLocation("locations.endArea");

        //payout

        totalDamagePayoutMultiplier = config.getDouble("payout.totalDamage");
        totalProjectileDamagePayoutMultiplier = config.getDouble("payout.totalProjectileDamage");
        totalHealPayoutMultiplier = config.getDouble("payout.totalHeal");
        totalFriendlyFirePayoutMultiplier = config.getDouble("payout.totalFriendlyFire");


        //settings
        friendlyFire = config.getBoolean("settings.friendlyFire");
        revivesAllowed = config.getInt("settings.revivesAllowed");
        playersAllowed = config.getInt("settings.playersAllowed");
        minimumPlayersToBegin = config.getInt("settings.minimumPlayersToBegin");
        maxPlayersAllowed = config.getInt("settings.maxPlayersAllowed");
        neededWinCommand = config.getInt("settings.neededWinCommand");
        removeLifeOnLogout = config.getBoolean("settings.removeLifeOnLogout");

        //load commands
        ConfigurationSection selection = config.getConfigurationSection("commands");
        if(selection == null) return;
        for(String key: selection.getKeys(false)){
            try{
                System.out.println(key);
                commands.put(RaidState.valueOf(key), new ArrayList<>(selection.getStringList(key)));
            }catch (Exception e){

            }
        }

        // anti cheater
        mustBeAliveForPercentOfGame = (float) config.getDouble("settings.mustBeAliveForPercentOfGame");
        mustBeInRanking=config.getInt("settings.mustBeInRanking",playersAllowed);

        winCommands = new ArrayList<>(config.getStringList("winCommands"));
        loseCommands = new ArrayList<>(config.getStringList("loseCommands"));

        disabledDamageCountMobs = new ArrayList<>();
        for(String mob: config.getStringList("settings.disabledDamageCountMobs")){
            try{
                disabledDamageCountMobs.add(Pattern.compile(mob));
            }catch (PatternSyntaxException e){
                plugin.getLogger().warning("[" + gameName + "] Invalid regex pattern: " + mob);
            }
        }
    }

    // if game playable

    public int playable(){
        if(inGameTime < 0) return -1;
        if(scheduledGames == 0) return -2;
        if(playersAllowed <= 0) return -3;
        if(playerSpawnPoints.size() == 0) return -4;
        return 0;
    }


    // state functions

    public void setGameState(RaidState state){
        if(state == currentGameState) return;

        Bukkit.getScheduler().runTask(plugin, ()-> {
            //stop current state
            if(currentGameStateData != null){
                currentGameStateData.beforeEnd();
            }

            //start next state
            currentGameState = state;
            RaidStateData data = getStateData(currentGameState);
            if(data == null) return;
            currentGameStateData = data;
            currentGameStateData.beforeStart();
            //set current state data
            //execute commands
            if(!commands.containsKey(state)) return;
            Man10Raid.api.executeScript(commands.get(state));
        });
        return;
    }

    public RaidStateData getStateData(RaidState state){
        switch (state){
            case REGISTERING:
                return new RegisteringState();
            case PREPARATION:
                return new PreparationState();
            case IN_GAME:
                return new InGameState();
            case FINISH:
                return new FinishState();
            case CONGRATULATIONS:
                return new CongratulationsState();
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

    public boolean registerPlayer(UUID p, String name, boolean bypass){
        if(currentGameState != RaidState.REGISTERING && !bypass){
            return false;
        }
        if(players.containsKey(p)) {
            return false;
        }
        players.put(p, new RaidPlayer(name, p));
        return true;
    }

    public boolean unregisterPlayer(Player p){
        if(currentGameState != RaidState.REGISTERING) {
            p.sendMessage(Man10Raid.prefix + "§c§l現在選手登録をすることはできません");
            return false;
        }
        if(!players.containsKey(p.getUniqueId())) {
            p.sendMessage(Man10Raid.prefix + "§c§lあなたは登録されていません");
            return false;
        }
        players.remove(p.getUniqueId());
        p.sendMessage(Man10Raid.prefix + "§a§l登録を解除しました");
        return true;
    }

    public boolean unregisterPlayer(UUID uuid){
        if(currentGameState != RaidState.REGISTERING) return false;
        if(!players.containsKey(uuid)) return false;
        players.remove(uuid);
        return true;
    }

    public void preRegisterPlayer(UUID uuid, int gameId){
        if(!preRegisteredPlayers.containsKey(gameId)) preRegisteredPlayers.put(gameId, new ArrayList<>());
        preRegisteredPlayers.get(gameId).add(uuid);
    }

    public HashMap<Integer, List<UUID>> preRegisteredPlayers = new HashMap<>();

    private <T> List<List<T>> batchList(List<T> inputList, final int maxSize) {
        List<List<T>> sublists = new ArrayList<>();

        final int size = inputList.size();

        for (int i = 0; i < size; i += maxSize) {
            // Math.min... size will be smaller than i + maxSize for the last batch (unless perfectly divisible),
            // including the first batch if size is smaller than max size
            sublists.add(new ArrayList<>(inputList.subList(i, Math.min(size, i + maxSize))));
        }

        return sublists;
    }

    public void dividePlayers(){

        ArrayList<UUID> registeredPlayers = new ArrayList<>(players.keySet());
        Collections.shuffle(registeredPlayers);

        if(playersAllowed == 0) return;




        int maxGames = players.size()/playersAllowed;
        if(players.size()%playersAllowed != 0) maxGames++;
        if(scheduledGames > maxGames || scheduledGames <= 0) scheduledGames = maxGames; // if scheduled games is too many games for the amount of players

        //remove players from registered players
        for(int game = 0; game < scheduledGames; game++){
            if(!preRegisteredPlayers.containsKey(game)) continue;
            for(UUID preRegistered: preRegisteredPlayers.get(game)){
                registeredPlayers.remove(preRegistered);
            }
        }

        //add players to index at location
        for(int game = 0; game < scheduledGames; game++){
            if(!preRegisteredPlayers.containsKey(game)) continue;
            for(UUID preRegistered: preRegisteredPlayers.get(game)){
                registeredPlayers.add(game*playersAllowed, preRegistered);
            }
        }

        List<List<UUID>> dividedPlayers = batchList(registeredPlayers, playersAllowed);
        for(int game = 0; game < scheduledGames; game++){
            for(UUID uuid : dividedPlayers.get(game)){
                RaidPlayer player = players.get(uuid);
                player.registeredGame = game;
                player.livesLeft = revivesAllowed;

                //set whitelist message
                Man10Raid.whitelist.setKickMessages(player.uuid, "あなたは" + (game + 1)  + "試合目です");
            }
        }



    }

    public ArrayList<RaidPlayer> getPlayersInGame(int gameNumber){
        ArrayList<RaidPlayer> result = new ArrayList<>();
        for(RaidPlayer player: players.values()){
            if(player.registeredGame == gameNumber) result.add(player);
        }
        return result;
    }

    public int getNumberOfPlayersAliveInGame(int gameNumber){
        ArrayList<RaidPlayer> playersInGame = getPlayersInGame(gameNumber);
        int result = 0;
        for(RaidPlayer player : playersInGame){
            if(player.livesLeft == 0) continue;
            result += 1;
        }
        return result;
    }

    //set settings functions
    //location point

    public void addPlayerSpawnPoint(Location l){
        playerSpawnPoints.add(l);
        Man10Raid.api.saveRaidGameConfig(this);
    }

    public void addRespawnLocation(Location l){
        playerRespawnLocations.add(l);
        Man10Raid.api.saveRaidGameConfig(this);
    }


    public void setEndAreaPoint(Location l){
        endArea = l;
        Man10Raid.api.saveRaidGameConfig(this);
    }

    //time

    public void setRegistrationTime(int time){
        registrationTime = time;
        Man10Raid.api.saveRaidGameConfig(this);
    }

    public void setPreparationTime(int time){
        preparationTime = time;
        Man10Raid.api.saveRaidGameConfig(this);
    }

    public void setInGameTime(int time){
        inGameTime = time;
        Man10Raid.api.saveRaidGameConfig(this);
    }

    public void setEndAreaTime(int time){
        endAreaTime = time;
        Man10Raid.api.saveRaidGameConfig(this);
    }

    //player functions

    public RaidPlayer getPlayer(UUID uuid){
        if(!players.containsKey(uuid)){
            return null;
        }
        return players.get(uuid);
    }

    public void teleportAllPlayersToLobby(){
        Bukkit.getServer().getScheduler().runTask(plugin, ()->{
            for(RaidPlayer player: players.values()){
                if(player.getPlayer() != null && player.getPlayer().isOnline()){
                    player.getPlayer().teleport(Man10Raid.lobbyLocation);
                }
            }
        });
    }

    public void removeOneLife(UUID uuid, boolean playerLeft){
        RaidPlayer deadPlayer = getPlayer(uuid);
        if(deadPlayer == null) return;
        if(deadPlayer.livesLeft <= 0) return;
        deadPlayer.livesLeft --;
        Player p = deadPlayer.getPlayer();
        if(p.isOnline() && !playerLeft){
            if(deadPlayer.livesLeft > 0) {
                //player still can play in arena
                if(playerRespawnLocations.isEmpty()){
                    //no respawn point
                    p.setBedSpawnLocation(playerSpawnPoints.get(0), true);
                }else{
                    p.setBedSpawnLocation(playerRespawnLocations.get(new Random().nextInt(playerRespawnLocations.size())), true);
                }
                p.sendMessage("残りライフ" + deadPlayer.livesLeft);
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20*10, 50));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20*10, 50));
            }else{
                //no respawns left
                p.setBedSpawnLocation(Man10Raid.lobbyLocation, true);
                p.sendMessage("あなたは死んだ");
            }

//            p.spigot().respawn();
            Bukkit.getScheduler().runTaskLater(plugin, ()->{
                p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20*10, 50));
                p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20*10, 50));
            }, 20);
        }

        checkIfGameEnded();
    }

    public void checkIfGameEnded(){
        Bukkit.getScheduler().runTaskLater(plugin, ()->{
            //write if all dead function here
            if(allLivesLeftInCurrentGame() <= 0){
                //all dead (not counting players in different server and in lobby)
                Bukkit.broadcastMessage("全員死亡した");
                setGameState(RaidState.FINISH);
            }
        }, 20);
    }

    public int allLivesLeftInCurrentGame(){
        int total = 0;
        for(RaidPlayer player: getPlayersInGame(currentGame)){
            Player p = player.getPlayer();
            if(p == null) continue;
            if(!p.isOnline()) continue;
            if(!p.getLocation().getWorld().equals(playerSpawnPoints.get(0).getWorld())) continue;
            total += player.livesLeft;
        }
        return total;
    }

    public void teleportPlayerToArena(Player p){
        if(playerRespawnLocations.isEmpty()){
            p.teleport(playerSpawnPoints.get(0));
        }else{
            p.teleport(playerRespawnLocations.get(new Random().nextInt(playerRespawnLocations.size())));
        }
    }

    public void teleportPlayerNearRandomPlayer(Player player){
        Collection<Player> players=playerSpawnPoints.get(0).getNearbyPlayers(500,500).stream().filter(allowedPlayer -> allowedPlayer.getGameMode()==GameMode.SURVIVAL|| allowedPlayer.getGameMode()==GameMode.SURVIVAL).collect(Collectors.toList());
        if(players.isEmpty()){
            player.teleport(playerSpawnPoints.get(0));
            return;
        }
        int size=players.size();
        int rand=new Random().nextInt(size);
        Player basePlayer= (Player) players.toArray()[rand];
        Optional<Location> nearestRespawnLoc=playerRespawnLocations.stream().min(Comparator.comparing(location -> location.distance(basePlayer.getLocation())));

        if(nearestRespawnLoc.isPresent()){
            player.teleport(nearestRespawnLoc.get());
        }
        else{
            player.teleport(playerSpawnPoints.get(0));
        }

    }

    public void payOutToPlayers(int game){
        ArrayList<RaidPlayer> players = getPlayersInGame(game);
        for(RaidPlayer player: players){
            if(player.getPlayer() == null) continue;
            if(!player.getPlayer().isOnline()) continue;
            if(player.livesLeft<1)continue;
            long money = (long) (player.totalDamage * totalDamagePayoutMultiplier +
                                player.totalProjectileDamage * totalProjectileDamagePayoutMultiplier +
                                player.totalHeal * totalHealPayoutMultiplier +
                                player.totalFriendlyDamage * totalFriendlyFirePayoutMultiplier);
            player.prizeMoney = money;
            if(Man10Raid.vault.deposit(player.uuid, money)){
                player.paymentSuccess = true;
            }

        }
    }

    public void logPlayersInGame(int game){
        ArrayList<RaidPlayer> players = getPlayersInGame(game);
        ArrayList<HashMap<String, Object>> payloads = new ArrayList<>();

        for(RaidPlayer player: players){
            HashMap<String, Object> localPayload = new HashMap<>();
            localPayload.put("game_id", gameId);
            localPayload.put("game_registered_match", player.registeredGame);
            localPayload.put("name", player.name);
            localPayload.put("uuid", player.uuid);
            localPayload.put("total_damage", player.totalDamage);
            localPayload.put("total_friendly_damage", player.totalFriendlyDamage);
            localPayload.put("total_projectile_damage", player.totalProjectileDamage);
            localPayload.put("total_heal", player.totalHeal);
            localPayload.put("payment_amount", player.prizeMoney);
            localPayload.put("won", won);
            localPayload.put("payment_success", player.paymentSuccess);
            payloads.add(localPayload);
        }

        Man10Raid.mysql.futureExecute(MySQLAPI.buildInsertQuery(payloads, "raid_player_log"));
    }


    //ranking functions

    public static <K, V extends Comparable<V> > Map<K, V>
    valueSort(final Map<K, V> map)
    {
        Comparator<K> valueComparator = (k1, k2) -> {
            int comp = map.get(k1).compareTo(
                    map.get(k2));
            if (comp == 0)
                return 1;
            else
                return comp;
        };

        // SortedMap created using the comparator
        Map<K, V> sorted = new TreeMap<K, V>(valueComparator);

        sorted.putAll(map);

        return sorted;
    }


    public ArrayList<RaidPlayer> getTotalDamageRanking(int game){
        ArrayList<RaidPlayer> players = getPlayersInGame(game);
        HashMap<UUID, RaidPlayer> playerMap = new HashMap<>();

        ArrayList<RaidPlayer> result = new ArrayList<>();

        TreeMap<UUID, Long> valueMap = new TreeMap<>();
        for(RaidPlayer player: players) {
            playerMap.put(player.uuid, player);
            valueMap.put(player.uuid, player.totalDamage);
        }

        Map<UUID, Long> finalMap = valueSort(valueMap);

        for(UUID uuid: finalMap.keySet()){
            result.add(playerMap.get(uuid));
        }
        Collections.reverse(result);


        return result;
    }

    public ArrayList<RaidPlayer> getTotalProjectileDamageRanking(int game){
        ArrayList<RaidPlayer> players = getPlayersInGame(game);
        HashMap<UUID, RaidPlayer> playerMap = new HashMap<>();

        ArrayList<RaidPlayer> result = new ArrayList<>();

        TreeMap<UUID, Long> valueMap = new TreeMap<>();
        for(RaidPlayer player: players) {
            playerMap.put(player.uuid, player);
            valueMap.put(player.uuid, player.totalProjectileDamage);
        }

        Map<UUID, Long> finalMap = valueSort(valueMap);

        for(UUID uuid: finalMap.keySet()){
            result.add(playerMap.get(uuid));
        }
        Collections.reverse(result);


        return result;
    }

    public ArrayList<RaidPlayer> getTotalHealRanking(int game){
        ArrayList<RaidPlayer> players = getPlayersInGame(game);
        HashMap<UUID, RaidPlayer> playerMap = new HashMap<>();

        ArrayList<RaidPlayer> result = new ArrayList<>();

        TreeMap<UUID, Long> valueMap = new TreeMap<>();
        for(RaidPlayer player: players) {
            playerMap.put(player.uuid, player);
            valueMap.put(player.uuid, player.totalHeal);
        }

        Map<UUID, Long> finalMap = valueSort(valueMap);

        for(UUID uuid: finalMap.keySet()){
            result.add(playerMap.get(uuid));
        }
        Collections.reverse(result);


        return result;
    }

    //logging functions

    public void logCurrentMatch(){
        HashMap<String, Object> payload = new HashMap<>();
        payload.put("game_id", gameId);
        payload.put("game_match", currentGame);
        payload.put("won", won);
        payload.put("game_time", (inGameTime - inGameTimeLeft));

        Man10Raid.mysql.futureExecute(MySQLAPI.buildInsertQuery(payload, "raid_game_log"));
    }





}
