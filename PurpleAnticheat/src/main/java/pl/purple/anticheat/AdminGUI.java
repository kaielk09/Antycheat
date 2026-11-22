package pl.purple.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Purple/black OP-only inventory interface for manual moderation and response profiles. */
public final class AdminGUI implements Listener {
 private final PurpleAnticheat plugin; private final BanManager bans;
 private final Map<UUID,Draft> drafts=new ConcurrentHashMap<>();
 private final Map<UUID,String> prompts=new ConcurrentHashMap<>();
 private final Map<UUID,String> selected=new ConcurrentHashMap<>();
 private static final String[] IDS={"movement","combat","flight","speed","interaction","packet","inventory","knockback" ,"aim","other","action"};
 private static final String[] LABELS={"Movement","Combat","Flight","Speed","Interaction","Packet","Inventory","Knockback","Aim" ,"Other","Action"};
 private static final String[] DURATIONS={"20d","6h","3d","1d","12h","1w","30m","2h","perm"};
 private static final int[] REVIEW={1,3,5,10,15,20,30,45,60};
 private final String prefix=PurpleAnticheat.color("&5&lPURPLE &d&lANTICHEAT &8» &r");
 public AdminGUI(PurpleAnticheat p,BanManager b){plugin=p;bans=b;}
 private boolean auth(Player p){return p.isOp()&&p.hasPermission("purpleanticheat.admin");}
 private void denied(Player p){p.sendMessage(prefix+PurpleAnticheat.color("&cOP and purpleanticheat.admin required."));p.closeInventory();}
 private String c(String s){return PurpleAnticheat.color(s);}
 private ItemStack item(Material m,String name,String... lore){ItemStack it=new ItemStack(m);ItemMeta meta=it.getItemMeta();meta.setDisplayName(c(name));List<String> lines=new ArrayList<>();for(String l:lore)lines.add(c(l));meta.setLore(lines);it.setItemMeta(meta);return it;}
 private Inventory menu(String page,int size,String title){Holder h=new Holder(page);Inventory inv=Bukkit.createInventory(h,size,c(title));h.inv=inv;for(int i=0;i<size;i++)if(i<9||i>=size-9||i%9==0||i%9==8)inv.setItem(i,item(Material.PURPLE_STAINED_GLASS_PANE,"&5 "));for(int i=0;i<size;i++)if(inv.getItem(i)==null)inv.setItem(i,item(Material.BLACK_STAINED_GLASS_PANE,"&8 "));return inv;}
 private void put(Player p,Inventory inv){if(!auth(p)){denied(p);return;}p.openInventory(inv);}
 public void openMain(Player p){
  Inventory i=menu("main",54,"&0PurpleAnticheat &8| &5Dashboard");
  i.setItem(10,item(Material.SHIELD,"&d&lProtection settings","&7Configure plugin safeguards"));
  i.setItem(12,item(Material.REDSTONE_TORCH,"&d&lAlerts","&7Status: "+status("alerts"),"&8Click to toggle"));
  i.setItem(14,item(Material.BOOK,"&d&lLogging","&7Status: "+status("logging"),"&8Click to toggle"));
  i.setItem(16,item(Material.WRITABLE_BOOK,"&d&lPunishment history","&7Browse moderation history"));
  i.setItem(29,item(Material.NAME_TAG,"&d&lBan editor","&7Create a manual ban"));
  i.setItem(31,item(Material.COMPARATOR,"&d&lConfiguration","&7Review-mode settings"));
  i.setItem(49,item(Material.EMERALD,"&a&lSAVE & CLOSE"));put(p,i);
 }
 private String status(String key){return plugin.getConfig().getBoolean("settings."+key,true)?"&aON":"&cOFF";}
 private void toggle(String path,Player p,String page){String k="settings."+path;plugin.getConfig().set(k,!plugin.getConfig().getBoolean(k,true));plugin.saveConfig();if(page.equals("protection"))openProtection(p);else if(page.equals("config"))openConfig(p);else openMain(p);}
 private void openProtection(Player p){Inventory i=menu("protection",27,"&0Purple &8| &dProtection");String[] keys={"enabled","alerts","logging","notify-admins"};int[] slots={10,12,14,16};for(int n=0;n<keys.length;n++)i.setItem(slots[n],item(Material.SHIELD,"&d&l"+keys[n],"&7Status: "+status(keys[n]),"&8Click to toggle"));i.setItem(22,item(Material.ARROW,"&7Back"));put(p,i);}
 private void openConfig(Player p){Inventory i=menu("config",27,"&0Purple &8| &dConfiguration");i.setItem(11,item(Material.LECTERN,"&d&lReview mode","&7Status: "+status("review-mode"),"&8Click to toggle"));i.setItem(22,item(Material.ARROW,"&7Back"));put(p,i);}
 public void openConsoleEdit(Player p){
  String sel=selected.computeIfAbsent(p.getUniqueId(),x->IDS[0]);Inventory i=menu("console",54,"&0Purple &8| &dResponse profiles");
  i.setItem(4,item(Material.REDSTONE,"&d&lRESPONSE PROFILES","&7Select a profile, toggle it, then configure duration."));
  int[] slots={10,11,12,13,14,15,16,19,20,21,22};
  for(int n=0;n<IDS.length;n++){String path="autopunish.checks."+IDS[n];boolean en=plugin.getConfig().getBoolean(path+".enabled",false);i.setItem(slots[n],item(IDS[n].equals(sel)?Material.LIME_DYE:(en?Material.ENCHANTED_BOOK:Material.GRAY_DYE),"&d&l"+LABELS[n], "&7Enabled: "+(en?"&aYES":"&cNO"),"&7Duration: &f"+plugin.getConfig().getString(path+".duration","1d"),"&8Click to select/toggle"));}
  for(int n=0;n<DURATIONS.length;n++)i.setItem(27+n,item(Material.CLOCK,"&d"+DURATIONS[n],"&7Set selected profile duration"));
  for(int n=0;n<REVIEW.length;n++)i.setItem(36+n,item(Material.CLOCK,"&d"+REVIEW[n]+"s review","&7Set selected review window"));
  i.setItem(45,item(Material.ARROW,"&7Back"));i.setItem(49,item(Material.EMERALD,"&a&lSAVE & CLOSE"));
  i.setItem(53,item(Material.BARRIER,"&c&lMASTER AUTO-PUNISH","&7Status: "+(plugin.getConfig().getBoolean("autopunish.enabled",true)?"&aON":"&cOFF")));
  put(p,i);
 }
 private void openBan(Player p){
  Draft d=drafts.computeIfAbsent(p.getUniqueId(),x->new Draft(p.getName()));Inventory i=menu("ban",45,"&0Purple &8| &dBan editor");
  i.setItem(10,item(Material.PLAYER_HEAD,"&d&lPlayer","&7Current: &f"+d.target,"&8Click to edit"));
  i.setItem(12,item(Material.WRITABLE_BOOK,"&d&lReason","&7Current: &f"+d.reason,"&8Click to edit"));
  i.setItem(14,item(Material.CLOCK,"&d&lDuration","&7Current: &f"+d.duration,"&7Examples: 30m, 2h, 3d, 1w, perm"));
  i.setItem(16,item(Material.NAME_TAG,"&d&lIssued by","&7Current: &f"+d.staff,"&8Click to edit"));
  i.setItem(22,item(Material.LIME_DYE,"&a&lAPPLY BAN","&7Apply after reviewing fields"));
  i.setItem(24,item(Material.BARRIER,"&cCLEAR FORM"));i.setItem(40,item(Material.ARROW,"&7Back"));put(p,i);
 }
 private void ask(Player p,String field){prompts.put(p.getUniqueId(),field);p.closeInventory();p.sendMessage(prefix+c("&dEnter "+field+" in chat. Type cancel to stop."));}
 @EventHandler public void click(InventoryClickEvent e){
  if(!(e.getView().getTopInventory().getHolder() instanceof Holder h))return;
  e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p)||!auth(p)||e.getClickedInventory()!=e.getView().getTopInventory())return;int s=e.getRawSlot();
  switch(h.page){
   case "main"->{if(s==10)openProtection(p);else if(s==12)toggle("alerts",p,"main");else if(s==14)toggle("logging",p,"main");else if(s==29)openBan(p);else if(s==31)openConfig(p);else if(s==16)openHistory(p);else if(s==49)saveClose(p);}
   case "protection"->{String[] k={"enabled","alerts","logging","notify-admins"};int[] sl={10,12,14,16};for(int n=0;n<sl.length;n++)if(s==sl[n]){toggle(k[n],p,"protection");return;}if(s==22)openMain(p);}
   case "config"->{if(s==11)toggle("review-mode",p,"config");if(s==22)openMain(p);}
   case "console"->{if(s==45)openMain(p);else if(s==49)saveClose(p);else if(s==53){plugin.getConfig().set("autopunish.enabled",!plugin.getConfig().getBoolean("autopunish.enabled",true));plugin.saveConfig();openConsoleEdit(p);}else{int[] sl={10,11,12,13,14,15,16,19,20,21};for(int n=0;n<sl.length;n++)if(s==sl[n]){String id=IDS[n],path="autopunish.checks."+id+".enabled";selected.put(p.getUniqueId(),id);plugin.getConfig().set(path,!plugin.getConfig().getBoolean(path,false));openConsoleEdit(p);return;}if(s>=27&&s<=35){plugin.getConfig().set("autopunish.checks."+selected.getOrDefault(p.getUniqueId(),IDS[0])+".duration",DURATIONS[s-27]);openConsoleEdit(p);}if(s>=36&&s<=44){plugin.getConfig().set("autopunish.checks."+selected.getOrDefault(p.getUniqueId(),IDS[0])+".confirm-seconds",REVIEW[s-36]);openConsoleEdit(p);}}}
   case "ban"->{if(s==10)ask(p,"player name");else if(s==12)ask(p,"ban reason");else if(s==14)ask(p,"duration (30m/2h/3d/1w/perm)");else if(s==16)ask(p,"staff name");else if(s==22)apply(p);else if(s==24){drafts.put(p.getUniqueId(),new Draft(p.getName()));openBan(p);}else if(s==40)openMain(p);}
   case "history"->{if(s==49)openMain(p);}
  }
 }
 private void apply(Player p){Draft d=drafts.get(p.getUniqueId());if(d==null||d.target.isBlank()||d.reason.isBlank()||d.duration.isBlank()||d.staff.isBlank()){p.sendMessage(prefix+c("&cComplete all fields first."));openBan(p);return;}BanManager.BanResult r=bans.ban(d.target,d.reason,d.duration,d.staff);if(r==BanManager.BanResult.SUCCESS){drafts.remove(p.getUniqueId());p.closeInventory();p.sendMessage(prefix+c("&aBan recorded."));}else{p.sendMessage(prefix+c("&cBan failed: "+r));openBan(p);}}
 private void openHistory(Player p){Inventory i=menu("history",54,"&0Purple &8| &dHistory");ConfigurationSection sec=bans.history().getConfigurationSection("history");if(sec==null)i.setItem(22,item(Material.BARRIER,"&7No punishments yet"));else{int slot=10;for(String k:sec.getKeys(false)){if(slot>=44)break;i.setItem(slot++,item(Material.PAPER,"&d"+sec.getString(k+".player","Unknown"),"&7Reason: "+sec.getString(k+".reason",""),"&7Duration: "+sec.getString(k+".duration",""),"&7Staff: "+sec.getString(k+".staff",""),"&7Date: "+sec.getString(k+".time","")));}}i.setItem(49,item(Material.ARROW,"&7Back"));put(p,i);}
 @EventHandler public void drag(InventoryDragEvent e){if(e.getView().getTopInventory().getHolder() instanceof Holder)e.setCancelled(true);}
 @EventHandler public void chat(AsyncPlayerChatEvent e){Player p=e.getPlayer();String field=prompts.remove(p.getUniqueId());if(field==null)return;e.setCancelled(true);String val=e.getMessage().trim();Bukkit.getScheduler().runTask(plugin,()->{if(!p.isOnline())return;if(val.equalsIgnoreCase("cancel")){openBan(p);return;}Draft d=drafts.computeIfAbsent(p.getUniqueId(),x->new Draft(p.getName()));if(field.startsWith("player"))d.target=val;else if(field.startsWith("ban reason"))d.reason=val;else if(field.startsWith("duration")){if(!BanManager.parseDuration(val).valid()){p.sendMessage(prefix+c("&cInvalid duration."));openBan(p);return;}d.duration=val;}else d.staff=val;openBan(p);});}
 @EventHandler public void quit(PlayerQuitEvent e){prompts.remove(e.getPlayer().getUniqueId());drafts.remove(e.getPlayer().getUniqueId());selected.remove(e.getPlayer().getUniqueId());}
 private void saveClose(Player p){plugin.saveConfig();bans.save();p.closeInventory();p.sendMessage(prefix+c("&aSaved."));}
 private static final class Holder implements InventoryHolder{final String page;Inventory inv;Holder(String p){page=p;}public Inventory getInventory(){return inv;}}
 private static final class Draft{String target="",reason="",duration="",staff;Draft(String s){staff=s;}}
}
