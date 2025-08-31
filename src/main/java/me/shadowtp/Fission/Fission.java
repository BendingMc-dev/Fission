package me.shadowtp.Fission;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.util.Vector;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class Fission extends FireAbility implements AddonAbility {

    private final String path = "ExtraAbilities.ShadowTP.Fission.";
    private Location location;
    @Attribute(Attribute.RANGE)
    private final double range;
    @Attribute(Attribute.COOLDOWN)
    private final long cooldown;
    @Attribute(Attribute.DAMAGE)
    private final double damage;
    private final double speed;
    private int travelled;
    private Vector direction;
    private final double markduration;
    public Set<Entity> targets = new HashSet<>();
    public Iterator<Entity> iterator;
    public int maxspread;
    public int hitbox;
    private boolean isMarked = false;





    public Fission(Player player) {
        super(player);
        range = getConfig().getDouble(path + "Range");
        cooldown = getConfig().getLong(path + "Cooldown");
        damage = getConfig().getDouble(path + "Damage");
        markduration = getConfig().getDouble(path + "MarkDuration");
        maxspread = getConfig().getInt(path + "MaxSpread");
        speed = getConfig().getDouble(path + "Speed");
        hitbox = getConfig().getInt(path + "Hitbox");
        location = player.getEyeLocation().clone();



        if (!this.bPlayer.canBend(this)){
            return;
        }

        bPlayer.addCooldown(this);

        start();
    }

    @Override //equivalent of Update/FixedUpdate() called each frame/tick
    public void progress() {
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

            int xOffset = 0;
            int yOffset = 0;
            int zOffset = 0;
            int amount = 1;

            direction = player.getEyeLocation().getDirection();
            location = location.add(direction.clone().multiply(speed));

            if (GeneralMethods.isSolid(location.getBlock()) || isWater(location.getBlock())) {
                break;
            }
            if (travelled > range) {
                remove();
                break;
            }
            double t = travelled / range;
            double curve = Math.sin(Math.PI * t) * maxspread;

            Vector perpendicular = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();

            Location left = location.clone().add(perpendicular.clone().multiply(curve));
            Location right = location.clone().add(perpendicular.clone().multiply(-curve));


            playFirebendingSound(left);
            playFirebendingSound(right);

            playFirebendingParticles(left, amount, xOffset, yOffset, zOffset);
            playFirebendingParticles(right, amount, xOffset, yOffset, zOffset);

            ApplyFissionMark(left, right);


            travelled++;
        }

    }

    public void ApplyFissionMark(Location left, Location right) {
        int xOffset = 0;
        int yOffset = 0;
        int zOffset = 0;
        int amount = 3;

        long startTime = getStartTime();
        targets = new HashSet<>();
        targets.addAll(GeneralMethods.getEntitiesAroundPoint(left, hitbox));
        targets.addAll(GeneralMethods.getEntitiesAroundPoint(right, hitbox));

        iterator = targets.iterator();
        long currenttime = System.currentTimeMillis();

        for (Entity entity : targets) {
            if (entity instanceof LivingEntity && entity.getUniqueId() != player.getUniqueId()) {

                while (iterator.hasNext()) {
                    Entity affected = iterator.next();
                    Location affectedLocation = affected.getLocation();
                    //Player player = (Player) affected;  -> Casted to player, game dont like!
                    LivingEntity victim = (LivingEntity) affected;
                    //ProjectKorra.log.info("He was in paris?");


                    if (victim.isDead()) {
                        this.remove();
                        ProjectKorra.log.severe("George?"); //never see?
                        break;
                    } else if (affectedLocation != null && GeneralMethods.isRegionProtectedFromBuild(this, this.location)) {
                        this.remove();
                        ProjectKorra.log.severe("HBadd bugaer?");
                        break;
                    }

                    if (currenttime - startTime >= markduration) {
                        affectedLocation.getWorld().spawnParticle(Particle.WAX_ON, amount, xOffset, yOffset, zOffset);
                        affectedLocation.getWorld().playSound(affected, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1, 1);
                        //DamageHandler.damageEntity(victim,damage, CoreAbility.getAbility(Fission.class));
                        ProjectKorra.log.severe("Who was in paris?");
                        isMarked = true;
                    }
                    iterator.remove();
                    isMarked = false;
                }
                return;
            }
        }
    }

    public void BigSparkyBoomBoom() {
        int xOffset = 1;
        int yOffset = 1;
        int zOffset = 1;
        int amount = 5;


        if (!iterator.hasNext()) { return; }
        Entity thingy = iterator.next();
        location = thingy.getLocation().clone();

        if (targets == null || targets.isEmpty()) {
            return;
        }

        location.getWorld().spawnParticle(Particle.EXPLOSION,amount, xOffset, yOffset, zOffset);
        location.getWorld().playSound(thingy, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);

        DamageHandler.damageEntity(thingy,damage, CoreAbility.getAbility(Fission.class));
    }






    public Set<Entity> getTargets() {
        return targets;
    }

    // boring slop
    @Override
    public void load() {
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new FissionListener(), ProjectKorra.plugin);
        ProjectKorra.log.info("Fission successfully loaded!");
        ProjectKorra.log.info("Can you hear the music...?");
        ConfigManager.getConfig().addDefault(path + "Cooldown", 5000);
        ConfigManager.getConfig().addDefault(path + "Range", 40);
        ConfigManager.getConfig().addDefault(path + "Damage", 5);
        ConfigManager.getConfig().addDefault(path + "MarkDuration", 5000);
        ConfigManager.getConfig().addDefault(path + "MaxSpread", 2);
        ConfigManager.getConfig().addDefault(path + "Speed", 0.2);
        ConfigManager.getConfig().addDefault(path + "Hitbox", 1);

        ConfigManager.defaultConfig.save();
    }

    @Override
    public void stop() {
        this.remove();

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
        return "Left Click to Mark, Sneak to Detonate!";
    }

    @Override
    public String getDescription(){
        return "Fingers in me bum ATM";
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