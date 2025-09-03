package me.shadowtp.Fission;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import com.projectkorra.projectkorra.attribute.Attribute;
import org.bukkit.util.Vector;

import java.util.*;


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
    private final long markduration;
    public Set<Entity> targets = new HashSet<>();
    public int maxspread;
    public int hitbox;
    public HashMap<UUID, Long> markedTarget = new HashMap<>();





    public Fission(Player player) {
        super(player);
        range = getConfig().getDouble(path + "Range");
        cooldown = getConfig().getLong(path + "Cooldown");
        damage = getConfig().getDouble(path + "Damage");
        markduration = getConfig().getLong(path + "MarkDuration");
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

    @Override
    public void progress() {
        if (this.player.isDead() || !this.player.isOnline()) {
            this.remove();
            return;
        } else if (this.location != null && GeneralMethods.isRegionProtectedFromBuild(this, this.location)) {
            this.remove();
            return;
        }
        while (travelled <= range) {

            int xOffset = 0;
            int yOffset = 0;
            int zOffset = 0;
            int amount = 1;

            direction = player.getEyeLocation().getDirection();
            direction.multiply(1);
            location = location.add(direction);

            if (GeneralMethods.isSolid(location.getBlock()) || isWater(location.getBlock())) {
                break;
            }
            if (travelled > range) {
                remove();
                break;
            }


            location.getWorld().playSound(location, Sound.ENTITY_CREEPER_PRIMED, 1, 2f);

            ParticleEffect.CAMPFIRE_COSY_SMOKE.display(location, amount, xOffset, yOffset, zOffset);
            ParticleEffect.SMOKE_NORMAL.display(location, amount, xOffset, yOffset, zOffset);
            ApplyFissionMark(location);


            travelled++;
        }



        handleMarkedEntities();

    }


    public void ApplyFissionMark(Location loc) {
        long startTime = getStartTime();
        targets = new HashSet<>();
        targets.addAll(GeneralMethods.getEntitiesAroundPoint(loc, hitbox));

        for (Entity entity : targets) {
            if (!(entity instanceof LivingEntity)) return;
            if (entity.getUniqueId().equals(player.getUniqueId())) return;

            LivingEntity victim = (LivingEntity) entity;
            Location affectedLocation = victim.getLocation();

            if (victim.isDead()) {
                this.remove();
                ProjectKorra.log.severe("George?");
                break;
            } else if (affectedLocation != null && GeneralMethods.isRegionProtectedFromBuild(this, this.location)) {
                this.remove();
                ProjectKorra.log.severe("HBadd bugaer?");
                break;
            }

                if (markedTarget.containsKey(victim.getUniqueId())) {
                    return;
                }

                markedTarget.put(victim.getUniqueId(), startTime);
                ProjectKorra.log.severe("Marked entity: " + victim.getUniqueId() + "Type:" + victim.getType());

        }
    }


    public void BigSparkyBoomBoom() {
        ProjectKorra.log.info("Big Sparky Boom Boom");

        if (markedTarget.isEmpty()) { return; }

        Iterator<Map.Entry<UUID, Long>> iter = markedTarget.entrySet().iterator();

        while (iter.hasNext()) {

            Map.Entry<UUID, Long> entry = iter.next();

            UUID markedEntity = entry.getKey();
            Long markedTime = entry.getValue();


            Entity victim = Bukkit.getEntity(markedEntity);

            if (System.currentTimeMillis() - markedTime >= markduration) {
                iter.remove();
                continue;
            } else if (victim == null || victim.isDead()) {
                iter.remove();
                continue;
            } else if (!(victim instanceof LivingEntity)) {
                iter.remove();
                continue;
            }
            Location affectedLocation = ((LivingEntity) victim).getEyeLocation().clone();
            Location playerLocation = player.getEyeLocation().clone();

            double distanceToVictim = playerLocation.distance(affectedLocation);
            int dynamicRange = (int) Math.ceil(distanceToVictim);
            Vector directionToPlayer = playerLocation.toVector().subtract(affectedLocation.toVector()).normalize();
            Location projectileLocation = affectedLocation.clone();
            int projectileTravelled = 0;


            affectedLocation.getWorld().spawnParticle(Particle.EXPLOSION, affectedLocation, 5, 1, 1, 1, 0.1);
            affectedLocation.getWorld().spawnParticle(Particle.FLAME, affectedLocation, 20, 2, 2, 2, 0.2);


            affectedLocation.getWorld().playSound(victim, Sound.ENTITY_GENERIC_EXPLODE, 2f, 1);
            while (projectileTravelled <= dynamicRange) {
                ProjectKorra.log.info("DistanceToVictim:" + distanceToVictim + "\nDynamic Range:" + dynamicRange + "\n");
                int xOffset = 0;
                int yOffset = 0;
                int zOffset = 0;
                int amount = 1;

                projectileLocation = projectileLocation.add(directionToPlayer.clone().multiply(1));

                if (GeneralMethods.isSolid(projectileLocation.getBlock()) || isWater(projectileLocation.getBlock())) {
                    ProjectKorra.log.info("solid nonce");
                    break;
                }
                if (projectileTravelled >= dynamicRange) {
                    ProjectKorra.log.info("fuck ass range brah");
                    break;
                }

                // Calculate curves effect
                double t = (double) projectileTravelled / dynamicRange;
                double curve = Math.sin(Math.PI * t) * maxspread;

                Vector perpendicular = directionToPlayer.clone().crossProduct(new Vector(0, 1, 0)).normalize();

                Location left = projectileLocation.clone().add(perpendicular.clone().multiply(curve));
                Location right = projectileLocation.clone().add(perpendicular.clone().multiply(-curve));



                playFirebendingSound(left);
                playFirebendingSound(right);

                playFirebendingParticles(left, amount, xOffset, yOffset, zOffset);
                playFirebendingParticles(right, amount, xOffset, yOffset, zOffset);

                ParticleEffect.CAMPFIRE_COSY_SMOKE.display(left, amount, xOffset, yOffset, zOffset);
                ParticleEffect.CAMPFIRE_COSY_SMOKE.display(right, amount, xOffset, yOffset, zOffset);

                ParticleEffect.SMOKE_NORMAL.display(left, amount, xOffset, yOffset, zOffset);
                ParticleEffect.SMOKE_NORMAL.display(right, amount, xOffset, yOffset, zOffset);

                projectileTravelled++;
            }
            DamageHandler.damageEntity(victim, damage, this);
            iter.remove();

        }
        this.remove();
    }
    private void handleMarkedEntities() {
        if (markedTarget.isEmpty()) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> iter = markedTarget.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<UUID, Long> entry = iter.next();
            UUID entityUUID = entry.getKey();
            Long markTime = entry.getValue();

            if (currentTime - markTime >= markduration) {
                iter.remove();
                continue;
            }

            Entity entity = Bukkit.getEntity(entityUUID);
            if (entity == null || entity.isDead()) {
                iter.remove();
                continue;
            }

            if (entity instanceof LivingEntity) {
                LivingEntity victim = (LivingEntity) entity;
                Location affectedLocation = victim.getLocation();

                if (GeneralMethods.isRegionProtectedFromBuild(this, affectedLocation)) {
                    iter.remove();
                    break;
                }

                affectedLocation.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, affectedLocation, 3, 0.3, 0.5, 0.3, 0.1);
                affectedLocation.getWorld().spawnParticle(Particle.CRIT, affectedLocation, 2, 0.2, 0.3, 0.2, 0.1);

                affectedLocation.getWorld().spawnParticle(Particle.ENCHANT, affectedLocation, 4, 0.5, 0.8, 0.5, 0.8);

                affectedLocation.getWorld().spawnParticle(Particle.FLAME, affectedLocation, 1, 0.2, 0.2, 0.2, 0.02);

                if ((currentTime - markTime) % 1000 < 50) {
                    affectedLocation.getWorld().playSound(affectedLocation, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 2f, 2f);
                }

            } else {
                iter.remove();
            }
        }
    }




    public HashMap<UUID, Long> getMarked() {
        return markedTarget;
    }


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