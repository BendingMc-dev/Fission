package me.shadowtp.Fission;

import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class FissionListener implements Listener {

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event){
        Player player = event.getPlayer();
        Fission fission = CoreAbility.getAbility(player, Fission.class);
        BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);


        if (event.isSneaking()) {
            if (bPlayer != null && fission != null) {
                if (bPlayer.getBoundAbilityName().equals("Fission")) {
                    if (!fission.getMarked().isEmpty()) {
                        ProjectKorra.log.info("Triggering BigSparkyBoomBoom!");
                        fission.BigSparkyBoomBoom();
                    } else {
                        ProjectKorra.log.info("No marked entities to detonate");
                    }
                } else {
                    ProjectKorra.log.info("Wrong ability bound: " + bPlayer.getBoundAbilityName());
                }
            } else {
                ProjectKorra.log.info("Missing bPlayer or fission instance");
            }
        }
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event){
        if (event.getAction().isLeftClick()) {
            Player player = event.getPlayer();
            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(player);

            if (bPlayer != null) {
                if (bPlayer.getBoundAbilityName().equals("Fission")) {
                    new Fission(player);
                }
            }
        }
    }
}