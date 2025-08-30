package me.shadowtp.Fission;

import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Fission extends FireAbility implements AddonAbility {

    public Fission(Player player) {
        super(player);
    }

    @Override
    public void progress() {

    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public void load() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return 0;
    }

    @Override
    public String getName() {
        return "Fission";
    }

    @Override
    public String getInstructions(){
        return "Combo!";
    }


    @Override
    public String getAuthor() {
        return "ShadowTP & RyanDusty";
    }

    @Override
    public String getVersion() {
        return "Suck my Nuts";
    }
}
