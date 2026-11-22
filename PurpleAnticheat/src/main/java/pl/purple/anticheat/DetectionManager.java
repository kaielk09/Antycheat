package pl.purple.anticheat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
/** Response pipeline for validated server-side signals; contains no cheat heuristics. */
public final class DetectionManager implements Listener {
 private final PurpleAnticheat plugin;private final Map<UUID,Long> frozenUntil=new ConcurrentHashMap<>();private final Map<UUID,String> pending=new ConcurrentHashMap<>();
 public DetectionManager(PurpleAnticheat plugin){this.plugin=plugin;}
 public boolean reportSignal(Player p,String check,String evidence){
  if(p==null||!p.isOnline()||check==null||check.isBlank())return false;String id=check.toLowerCase(java.util.Locale.ROOT),base="autopunish.checks."+id;
  if(!plugin.getConfig().getBoolean("settings.enabled",true)||!plugin.getConfig().getBoolean(base+".enabled",false)||pending.containsKey(p.getUniqueId()))return false;
  int seconds=Math.max(1,Math.min(30,plugin.getConfig().getInt(base+".confirm-seconds",3)));UUID uuid=p.getUniqueId();
  frozenUntil.put(uuid,System.currentTimeMillis()+seconds*1000L);pending.put(uuid,id);p.setVelocity(new org.bukkit.util.Vector(0,0,0));
  p.sendMessage(PurpleAnticheat.color(plugin.getConfig().getString("messages.prefix","&5Purple &dAnticheat &8» ")+"&cMovement temporarily restricted while a detection is reviewed."));
  notifyStaff(p,id,evidence,seconds);
  Bukkit.getScheduler().runTaskLater(plugin,()->{String active=pending.remove(uuid);frozenUntil.remove(uuid);if(active==null||!p.isOnline())return;if(plugin.getConfig().getBoolean("autopunish.enabled",true))plugin.punishDetection(active,p.getName(),evidence==null?"validated detection":evidence);},seconds*20L);return true;
 }
 private void notifyStaff(Player target,String check,String evidence,int seconds){
  if(!plugin.getConfig().getBoolean("settings.notify-admins",true))return;String m=PurpleAnticheat.color(plugin.getConfig().getString("messages.prefix","&5Purple &dAnticheat &8» ")+"&cSignal &f"+check+" &cfor &f"+target.getName()+"&c. Review window: &f"+seconds+"s"+(evidence==null||evidence.isBlank()?"":" &8("+evidence+")"));
  for(Player staff:Bukkit.getOnlinePlayers())if(staff.isOp()&&staff.hasPermission("purpleanticheat.admin"))staff.sendMessage(m);
 }
 @EventHandler(ignoreCancelled=true) public void onMove(PlayerMoveEvent e){Long until=frozenUntil.get(e.getPlayer().getUniqueId());if(until==null)return;if(System.currentTimeMillis()>=until){frozenUntil.remove(e.getPlayer().getUniqueId());return;}if(e.getTo()!=null&&(e.getFrom().getX()!=e.getTo().getX()||e.getFrom().getY()!=e.getTo().getY()||e.getFrom().getZ()!=e.getTo().getZ()))e.setTo(e.getFrom());}
 @EventHandler public void onQuit(PlayerQuitEvent e){UUID id=e.getPlayer().getUniqueId();frozenUntil.remove(id);pending.remove(id);}
}