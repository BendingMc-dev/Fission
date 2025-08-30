package me.shadowtp.Fission;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.util.Vector;


import java.util.Iterator;
import java.util.List;



public class Fission extends FireAbility implements AddonAbility {

    private final String path = "ExtraAbilties.ShadowTP.Fission.";
    private Location location;
    @Attribute(Attribute.RANGE)
    private final double range;
    @Attribute(Attribute.COOLDOWN)
    private final long cooldown;
    @Attribute(Attribute.DAMAGE)
    private final double damage;
    private double lifetime;
    private int travelled;
    private Vector direction;
    private double markduration;


    public Fission(Player player) {
        super(player);
        range = getConfig().getDouble(path + "Range");
        cooldown = getConfig().getLong(path + "Cooldown");
        damage = getConfig().getDouble(path + "Damage");
        markduration = getConfig().getDouble(path + "MarkDuration");

        if (!this.bPlayer.canBend(this)){
            return;
        }

        bPlayer.addCooldown(this);

        start();
    }

    @Override //equivalent of Update/FixedUpdate() called each frame/tick
    public void progress() {
        direction = player.getEyeLocation().getDirection().clone().normalize();


        /// Sanity Checks
        if (this.player.isDead() || !this.player.isOnline()) {
            this.remove();
            return;
        } else if (this.location != null && GeneralMethods.isRegionProtectedFromBuild(this, this.location)) {
            this.remove();
            return;
        }


        // While Progressing
        while (travelled <= range) {

            int xOffset = 1;
            int yOffset = 1;
            int zOffset = 1;
            int amount = 1;

            location = location.add(direction.clone().multiply(travelled));

            if (GeneralMethods.isSolid(location.getBlock()) || isWater(location.getBlock())) {
                break;
            }
            if (travelled >= range) {
                remove();
                break;
            }

            playFirebendingSound(location);
            playFirebendingParticles(location, amount, xOffset, yOffset, zOffset);
            ApplyFissionMark();

            travelled++;
        }

    }

    public void ApplyFissionMark() {
        int xOffset = 1;
        int yOffset = 1;
        int zOffset = 1;
        int amount = 1;

        long startTime = System.currentTimeMillis();

        List<Entity> targets = GeneralMethods.getEntitiesAroundPoint(location, 1);
        Iterator<Entity> iterator = targets.iterator();


        for (Entity entity : targets) {
            if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {

                while (iterator.hasNext()) {
                    Entity affected = iterator.next();
                    Location affectedLocation = affected.getLocation();
                    Player player = (Player) affected;

                    if (affected.isDead() || !player.isOnline()) {
                        this.remove();
                        break;
                    } else if (affectedLocation != null && GeneralMethods.isRegionProtectedFromBuild(this, this.location)) {
                        this.remove();
                        break;
                    }

                    while (System.currentTimeMillis() - startTime > markduration) {
                        affectedLocation.getWorld().spawnParticle(Particle.WAX_ON, amount, xOffset, yOffset, zOffset);
                        affectedLocation.getWorld().playSound(affected, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1, 1);
                    }
                    iterator.remove();
                }
                return;
            }
        }
    }

    public void BigSparkyBoomBoom(){
        // sanity.
            // ijogredjije
    }






    // boring slop


    @Override
    public void load() {
        ConfigManager.getConfig().addDefault(path + "Cooldown", 5000);
        ConfigManager.getConfig().addDefault(path + "Range", 40);
        ConfigManager.getConfig().addDefault(path + "Damage", 5);
        ConfigManager.getConfig().addDefault(path + "MarkDuration", 5000);
    }

    @Override
    public void stop() {

    }
    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return cooldown;
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
    public String getDescription(){
        return "Fingers in me bum";
    }


    @Override
    public String getAuthor() {
        return "ShadowTP & RyanDusty";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
