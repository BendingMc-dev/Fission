package me.shadowtp.Fission;

import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class FissionListener implements Listener {

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event){
        if (event.isSneaking()){
            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(event.getPlayer());
            if (bPlayer != null)   {
                if (bPlayer.getBoundAbilityName().equals("Fission")){
                    new Fission((Player)bPlayer);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event){
        if (event.getAction().isLeftClick()){
            BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(event.getPlayer());
            Fission fission = new Fission((Player)bPlayer);

            if (bPlayer != null && fission.getTargets() == null) {
                if (bPlayer.getBoundAbilityName().equals("Fission")) {
                    new Fission((Player)bPlayer);
                }
            } else if (fission.getTargets() != null) {
                fission.BigSparkyBoomBoom();
            }
        }
    }
}