package me.shadowtp.Fission;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public class Firearms extends FireAbility implements AddonAbility {

    private final String path = "ExtraAbilities.ShadowTP.Firearms.";
    private long duration;
    @Attribute(Attribute.COOLDOWN)
    private long cooldown;
    @Attribute(Attribute.DAMAGE)
    private double volleyDamage;
    @Attribute(Attribute.RANGE)
    private double volleyRange;
    private long volleyCooldown;
    private double volleySpeed;
    private double volleyHitbox;
    private double lightningRange;
    private double lightningDamage;
    private long lightningCooldown;
    private double chainRadius;
    private double chainDamage;
    private long dashCooldown;
    private double dashSpeed;
    private double dashLift;
    private long liftCooldown;
    private double liftPower;
    private long fallSaveCooldown;
    private int ambientParticles;

    private long lastVolleyTime;
    private long lastLightningTime;
    private long lastDashTime;
    private long lastLiftTime;
    private long lastFallSaveTime;
    private long lastAmbientTime;
    private boolean ended;
    private Location location;

    public Firearms(Player player) {
        super(player);
        if (!this.bPlayer.canBend(this)) {
            return;
        }

        duration = getConfig().getLong(path + "Duration");
        cooldown = getConfig().getLong(path + "Cooldown");
        volleyDamage = getConfig().getDouble(path + "VolleyDamage");
        volleyRange = getConfig().getDouble(path + "VolleyRange");
        volleyCooldown = getConfig().getLong(path + "VolleyCooldown");
        volleySpeed = getConfig().getDouble(path + "VolleySpeed");
        volleyHitbox = getConfig().getDouble(path + "VolleyHitbox");
        lightningRange = getConfig().getDouble(path + "LightningRange");
        lightningDamage = getConfig().getDouble(path + "LightningDamage");
        lightningCooldown = getConfig().getLong(path + "LightningCooldown");
        chainRadius = getConfig().getDouble(path + "LightningChainRadius");
        chainDamage = getConfig().getDouble(path + "LightningChainDamage");
        dashCooldown = getConfig().getLong(path + "DashCooldown");
        dashSpeed = getConfig().getDouble(path + "DashSpeed");
        dashLift = getConfig().getDouble(path + "DashLift");
        liftCooldown = getConfig().getLong(path + "LiftCooldown");
        liftPower = getConfig().getDouble(path + "LiftPower");
        fallSaveCooldown = getConfig().getLong(path + "FallSaveCooldown");
        ambientParticles = getConfig().getInt(path + "AmbientParticles");
        location = player.getLocation().clone();

        start();
        playActivationEffects();
    }

    @Override
    public void progress() {
        if (this.player == null || this.player.isDead() || !this.player.isOnline()) {
            endAbility();
            return;
        }

        if (System.currentTimeMillis() - getStartTime() >= duration) {
            endAbility();
            return;
        }

        if (GeneralMethods.isRegionProtectedFromBuild(this, this.player.getLocation())) {
            endAbility();
            return;
        }

        location = player.getLocation().clone();
        playAmbientEffects();
    }

    public void triggerFireVolley() {
        if (!isReady(lastVolleyTime, volleyCooldown)) {
            return;
        }
        lastVolleyTime = System.currentTimeMillis();

        Location start = player.getEyeLocation().clone();
        Vector direction = start.getDirection().normalize();
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.2);
        Vector left = right.clone().multiply(-1);

        Set<LivingEntity> hitTargets = new HashSet<>();
        fireVolleyProjectile(start, direction.clone().add(left).normalize(), hitTargets);
        fireVolleyProjectile(start, direction.clone(), hitTargets);
        fireVolleyProjectile(start, direction.clone().add(right).normalize(), hitTargets);

        playFirebendingSound(start);
    }

    public void triggerLightningArc() {
        if (!isReady(lastLightningTime, lightningCooldown)) {
            return;
        }
        lastLightningTime = System.currentTimeMillis();

        Location start = player.getEyeLocation().clone();
        Vector direction = start.getDirection().normalize();
        Location current = start.clone();
        double travelled = 0;
        LivingEntity primaryTarget = null;

        while (travelled <= lightningRange) {
            current.add(direction.clone().multiply(0.6));
            if (GeneralMethods.isSolid(current.getBlock()) || isWater(current.getBlock())) {
                break;
            }
            current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, current, 4, 0.1, 0.1, 0.1, 0.01);
            current.getWorld().spawnParticle(Particle.CRIT, current, 2, 0.1, 0.1, 0.1, 0.01);

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(current, 1.4)) {
                if (entity instanceof LivingEntity living && !entity.getUniqueId().equals(player.getUniqueId())) {
                    primaryTarget = living;
                    break;
                }
            }

            if (primaryTarget != null) {
                break;
            }

            travelled += 0.6;
        }

        if (primaryTarget == null) {
            return;
        }

        Location impact = primaryTarget.getLocation().clone();
        impact.getWorld().strikeLightningEffect(impact);
        DamageHandler.damageEntity(primaryTarget, lightningDamage, this);
        playLightningChain(impact);

        for (Entity entity : GeneralMethods.getEntitiesAroundPoint(impact, chainRadius)) {
            if (!(entity instanceof LivingEntity living) || entity.getUniqueId().equals(player.getUniqueId())
                    || entity.getUniqueId().equals(primaryTarget.getUniqueId())) {
                continue;
            }
            DamageHandler.damageEntity(living, chainDamage, this);
            playLightningArc(impact, living.getLocation());
        }
    }

    public void triggerBlazeDash() {
        if (!isReady(lastDashTime, dashCooldown)) {
            return;
        }
        lastDashTime = System.currentTimeMillis();

        Vector direction = player.getLocation().getDirection().normalize();
        Vector velocity = direction.multiply(dashSpeed).setY(dashLift);
        player.setVelocity(player.getVelocity().add(velocity));

        playFirebendingParticles(player.getLocation(), 12, 0.4, 0.2, 0.4);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.1f, 1.4f);
        spawnArmDisplays(player.getLocation());
    }

    public void triggerCinderLift() {
        if (!isReady(lastLiftTime, liftCooldown)) {
            return;
        }
        lastLiftTime = System.currentTimeMillis();

        player.setVelocity(player.getVelocity().add(new Vector(0, liftPower, 0)));
        ParticleEffect.FLAME.display(player.getLocation(), 18, 0.3, 0.1, 0.3, 0.02);
        ParticleEffect.SMOKE_NORMAL.display(player.getLocation(), 12, 0.3, 0.1, 0.3, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.2f, 1.2f);
        spawnArmDisplays(player.getLocation());
    }

    public void triggerFallSave(Location impactLocation) {
        if (!isReady(lastFallSaveTime, fallSaveCooldown)) {
            return;
        }
        lastFallSaveTime = System.currentTimeMillis();

        Location base = impactLocation.clone().subtract(0, 0.9, 0);
        ParticleEffect.FLAME.display(base, 20, 0.6, 0.1, 0.6, 0.02);
        ParticleEffect.SMOKE_LARGE.display(base, 10, 0.6, 0.1, 0.6, 0.02);
        base.getWorld().playSound(base, Sound.ITEM_FIRECHARGE_USE, 1.4f, 0.8f);
        spawnArmDisplays(base);
    }

    private void fireVolleyProjectile(Location start, Vector direction, Set<LivingEntity> hitTargets) {
        Location current = start.clone();
        double travelled = 0;

        while (travelled <= volleyRange) {
            current.add(direction.clone().multiply(volleySpeed));
            if (GeneralMethods.isSolid(current.getBlock()) || isWater(current.getBlock())) {
                break;
            }

            ParticleEffect.FLAME.display(current, 1, 0.05, 0.05, 0.05, 0.01);
            ParticleEffect.SMOKE_NORMAL.display(current, 1, 0.05, 0.05, 0.05, 0.01);

            for (Entity entity : GeneralMethods.getEntitiesAroundPoint(current, volleyHitbox)) {
                if (!(entity instanceof LivingEntity living) || entity.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }
                if (hitTargets.contains(living)) {
                    continue;
                }
                DamageHandler.damageEntity(living, volleyDamage, this);
                living.setFireTicks(60);
                hitTargets.add(living);
                spawnArmDisplays(living.getLocation());
            }

            travelled += volleySpeed;
        }
    }

    private void playActivationEffects() {
        Location origin = player.getLocation().clone();
        ParticleEffect.FLAME.display(origin, 30, 0.6, 0.4, 0.6, 0.03);
        ParticleEffect.ELECTRIC_SPARK.display(origin, 20, 0.4, 0.2, 0.4, 0.01);
        origin.getWorld().playSound(origin, Sound.ENTITY_BLAZE_AMBIENT, 1.2f, 1.6f);
        spawnArmDisplays(origin);
    }

    private void playAmbientEffects() {
        if (!isReady(lastAmbientTime, 150L)) {
            return;
        }
        lastAmbientTime = System.currentTimeMillis();

        Location base = player.getLocation().clone().add(0, 1.2, 0);
        Vector direction = player.getLocation().getDirection().normalize();
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize().multiply(0.5);
        Location leftHand = base.clone().add(right.clone().multiply(-1));
        Location rightHand = base.clone().add(right);

        int sparkParticles = Math.max(1, (int) Math.ceil(ambientParticles / 2.0));
        leftHand.getWorld().spawnParticle(Particle.FLAME, leftHand, ambientParticles, 0.1, 0.1, 0.1, 0.01);
        rightHand.getWorld().spawnParticle(Particle.FLAME, rightHand, ambientParticles, 0.1, 0.1, 0.1, 0.01);
        leftHand.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, leftHand, sparkParticles, 0.1, 0.1, 0.1, 0.01);
        rightHand.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, rightHand, sparkParticles, 0.1, 0.1, 0.1, 0.01);
    }

    private void playLightningChain(Location impact) {
        impact.getWorld().playSound(impact, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2f, 1.1f);
        ParticleEffect.ELECTRIC_SPARK.display(impact, 35, 0.6, 0.6, 0.6, 0.05);
        ParticleEffect.CRIT_MAGIC.display(impact, 14, 0.3, 0.4, 0.3, 0.04);
    }

    private void playLightningArc(Location start, Location end) {
        double distance = start.distance(end);
        if (distance <= 0.1) {
            return;
        }
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        Location current = start.clone();
        double travelled = 0;

        while (travelled <= distance) {
            current.add(direction.clone().multiply(0.5));
            current.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, current, 3, 0.05, 0.05, 0.05, 0.01);
            current.getWorld().spawnParticle(Particle.END_ROD, current, 1, 0.02, 0.02, 0.02, 0.01);
            travelled += 0.5;
        }
    }

    private void spawnArmDisplays(Location base) {
        World world = base.getWorld();
        if (world == null) {
            return;
        }

        Vector[] offsets = new Vector[] {
                new Vector(0.4, 0.05, 0.4),
                new Vector(-0.4, 0.05, 0.4),
                new Vector(0.4, 0.05, -0.4),
                new Vector(-0.4, 0.05, -0.4)
        };

        for (Vector offset : offsets) {
            Location spawn = base.clone().add(offset);
            ItemDisplay display = world.spawn(spawn, ItemDisplay.class, entity -> {
                entity.setItemStack(new ItemStack(Material.BLAZE_ROD));
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setBrightness(new Display.Brightness(15, 15));
                entity.setTransformation(new Transformation(
                        new Vector3f(0f, 0f, 0f),
                        new AxisAngle4f(0f, 0f, 0f, 1f),
                        new Vector3f(0.4f, 0.4f, 0.4f),
                        new AxisAngle4f(0f, 0f, 0f, 1f)
                ));
            });

            Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, display::remove, 20L);
        }
    }

    private boolean isReady(long lastUse, long cooldown) {
        return System.currentTimeMillis() - lastUse >= cooldown;
    }

    private void endAbility() {
        if (ended) {
            return;
        }
        ended = true;
        bPlayer.addCooldown(this);
        remove();
    }

    @Override
    public void load() {
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new FirearmsListener(), ProjectKorra.plugin);
        ConfigManager.getConfig().addDefault(path + "Duration", 45000);
        ConfigManager.getConfig().addDefault(path + "Cooldown", 20000);
        ConfigManager.getConfig().addDefault(path + "VolleyDamage", 2.5);
        ConfigManager.getConfig().addDefault(path + "VolleyRange", 18);
        ConfigManager.getConfig().addDefault(path + "VolleyCooldown", 1000);
        ConfigManager.getConfig().addDefault(path + "VolleySpeed", 0.9);
        ConfigManager.getConfig().addDefault(path + "VolleyHitbox", 1.2);
        ConfigManager.getConfig().addDefault(path + "LightningRange", 22);
        ConfigManager.getConfig().addDefault(path + "LightningDamage", 4);
        ConfigManager.getConfig().addDefault(path + "LightningCooldown", 3000);
        ConfigManager.getConfig().addDefault(path + "LightningChainRadius", 4);
        ConfigManager.getConfig().addDefault(path + "LightningChainDamage", 2.5);
        ConfigManager.getConfig().addDefault(path + "DashCooldown", 4000);
        ConfigManager.getConfig().addDefault(path + "DashSpeed", 1.2);
        ConfigManager.getConfig().addDefault(path + "DashLift", 0.2);
        ConfigManager.getConfig().addDefault(path + "LiftCooldown", 3500);
        ConfigManager.getConfig().addDefault(path + "LiftPower", 0.9);
        ConfigManager.getConfig().addDefault(path + "FallSaveCooldown", 1000);
        ConfigManager.getConfig().addDefault(path + "AmbientParticles", 4);

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
        return "Firearms";
    }

    @Override
    public String getInstructions() {
        return "Sneak to activate Firearms. Left Click: Fire Volley. Right Click: Lightning Arc. Sneak + Left: Cinder Lift. Sneak + Right: Blaze Dash.";
    }

    @Override
    public String getDescription() {
        return "Ignite blazing firearms to unleash rapid fire volleys, crackling lightning arcs, and blazing mobility for a limited time.";
    }

    @Override
    public String getAuthor() {
        return "ShadowTP & RyanDusty";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }
}
