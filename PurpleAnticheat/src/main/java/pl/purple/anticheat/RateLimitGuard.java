package pl.purple.anticheat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
/** Conservative server-side rate guards for spam/abuse, not cheat detectors. */
public final class RateLimitGuard implements Listener {
 private final PurpleAnticheat plugin;
 private final Map<UUID,Map<String,Deque<Long>>> buckets=new ConcurrentHashMap<>();
 private final Map<UUID,Long> lastWarn=new ConcurrentHashMap<>();
 public RateLimitGuard(PurpleAnticheat plugin){this.plugin=plugin;}
 @EventHandler(ignoreCancelled=true) public void onCommand(PlayerCommandPreprocessEvent e){if(exceeded(e.getPlayer(),"commands","guard.commands",5))e.setCancelled(true);}
 @EventHandler(ignoreCancelled=true) public void onChat(AsyncPlayerChatEvent e){if(exceeded(e.getPlayer(),"chat","guard.chat",5))e.setCancelled(true);}
 @EventHandler(ignoreCancelled=true) public void onInteract(PlayerInteractEvent e){Action a=e.getAction();if(a==Action.LEFT_CLICK_AIR||a==Action.RIGHT_CLICK_AIR)return;if(exceeded(e.getPlayer(),"interactions","guard.interactions",5))e.setCancelled(true);}
 private boolean exceeded(Player p,String bucket,String path,int fallback){
  if(!plugin.getConfig().getBoolean("guard.enabled",true))return false;
  int limit=Math.max(1,plugin.getConfig().getInt(path+".limit",1));
  long window=Math.max(1,plugin.getConfig().getInt(path+".window-seconds",fallback))*1000L,now=System.currentTimeMillis();
  Deque<Long> q=buckets.computeIfAbsent(p.getUniqueId(),id->new ConcurrentHashMap<>()).computeIfAbsent(bucket,k->new ArrayDeque<>());
  synchronized(q){while(!q.isEmpty()&&now-q.peekFirst()>window)q.removeFirst();q.addLast(now);if(q.size()<=limit)return false;}
  warn(p,bucket,limit,window/1000L);return true;
 }
 private void warn(Player p,String category,int limit,long seconds){
  long now=System.currentTimeMillis();Long prev=lastWarn.put(p.getUniqueId(),now);if(prev!=null&&now-prev<2000)return;
  String msg=PurpleAnticheat.color(plugin.getConfig().getString("messages.prefix","&5Purple &dAnticheat &8» ")+"&eRate guard blocked excess "+category+" from &f"+p.getName()+" &7("+limit+" per "+seconds+"s)");
  plugin.getLogger().warning("Rate guard blocked "+category+" from "+p.getName());
  if(!plugin.getConfig().getBoolean("settings.notify-admins",true))return;
  Bukkit.getScheduler().runTask(plugin,()->{for(Player staff:Bukkit.getOnlinePlayers())if(staff.isOp()&&staff.hasPermission("purpleanticheat.admin"))staff.sendMessage(msg);});
 }
}