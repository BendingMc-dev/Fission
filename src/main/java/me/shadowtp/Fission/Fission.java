package me.shadowtp.Fission;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
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
        handleMarkedEntities();

    }

    public void ApplyFissionMark(Location left, Location right) {
        long startTime = getStartTime();
        targets = new HashSet<>();
        targets.addAll(GeneralMethods.getEntitiesAroundPoint(left, hitbox));
        targets.addAll(GeneralMethods.getEntitiesAroundPoint(right, hitbox));

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

        for (Map.Entry<UUID, Long> entry : markedTarget.entrySet()) {
            UUID markedEntity = entry.getKey();
            Long markedTime = entry.getValue();

            Entity victim = Bukkit.getEntity(markedEntity);

            if (System.currentTimeMillis() - markedTime >= markduration) {
                this.remove();
                break;
            } else if (victim == null || victim.isDead()) {
                return;
            } else if (!(victim instanceof LivingEntity)) {
                return;
            }
            Location affectedLocation = victim.getLocation().clone();

            affectedLocation.getWorld().spawnParticle(Particle.EXPLOSION, affectedLocation, 5, 1, 1, 1, 0.1);
            affectedLocation.getWorld().spawnParticle(Particle.FLAME, affectedLocation, 20, 2, 2, 2, 0.2);


            affectedLocation.getWorld().playSound(victim, Sound.ENTITY_GENERIC_EXPLODE, 2f, 1);
            DamageHandler.damageEntity(victim, damage, this);



        }
        markedTarget.clear();
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
                ProjectKorra.log.info("Mark expired for entity: " + entityUUID);
                continue;
            }

            Entity entity = Bukkit.getEntity(entityUUID);
            if (entity == null || entity.isDead()) {
                iter.remove();
                ProjectKorra.log.info("Removed mark from null/dead entity");
                continue;
            }

            if (entity instanceof LivingEntity) {
                LivingEntity victim = (LivingEntity) entity;
                Location affectedLocation = victim.getLocation();

                if (GeneralMethods.isRegionProtectedFromBuild(this, affectedLocation)) {
                    iter.remove();
                    ProjectKorra.log.info("Removed mark from protected region");
                    continue;
                }

                affectedLocation.getWorld().spawnParticle(Particle.WAX_ON, affectedLocation, 5, 0.2, 0.5, 0.2, 0.1);
                ProjectKorra.log.info("Spawning particle wax");

                if ((currentTime - markTime) % 1000 < 50) {
                    affectedLocation.getWorld().playSound(affectedLocation, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 2f, 1.2f);
                    ProjectKorra.log.info("Playing sound firework");
                }

            } else {
                iter.remove();
            }
        }
    }




    public HashMap<UUID, Long> getMarked() {
        return markedTarget;
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