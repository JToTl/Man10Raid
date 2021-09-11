package com.shojabon.man10raid.DataClass;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RaidPlayer {

    public double totalDamage = 0;
    public String name;
    public UUID uuid;
    public int registeredGame = -1;

    public RaidPlayer(String name, UUID uuid){
        this.name = name;
        this.uuid = uuid;
    }

    public Player getPlayer(){
        return Bukkit.getServer().getPlayer(uuid);
    }


}
