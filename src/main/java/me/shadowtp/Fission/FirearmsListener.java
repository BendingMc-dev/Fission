package me.shadowtp.Fission;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class FirearmsListener implements Listener {

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null || !"Firearms".equals(bPlayer.getBoundAbilityName())) {
            return;
        }

        if (CoreAbility.getAbility(player, Firearms.class) == null) {
            new Firearms(player);
        }
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        if (!event.getAction().isLeftClick() && !event.getAction().isRightClick()) {
            return;
        }
        Player player = event.getPlayer();
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);
        if (bPlayer == null || !"Firearms".equals(bPlayer.getBoundAbilityName())) {
            return;
        }

        Firearms firearms = CoreAbility.getAbility(player, Firearms.class);
        if (firearms == null) {
            return;
        }

        if (player.isSneaking()) {
            if (event.getAction().isLeftClick()) {
                firearms.triggerCinderLift();
            } else {
                firearms.triggerBlazeDash();
            }
        } else {
            if (event.getAction().isLeftClick()) {
                firearms.triggerFireVolley();
            } else {
                firearms.triggerLightningArc();
            }
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Firearms firearms = CoreAbility.getAbility(player, Firearms.class);
        if (firearms == null) {
            return;
        }

        event.setCancelled(true);
        firearms.triggerFallSave(player.getLocation());
    }
}
