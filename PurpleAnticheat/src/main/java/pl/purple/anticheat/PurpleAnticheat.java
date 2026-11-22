package pl.purple.anticheat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
public final class PurpleAnticheat extends JavaPlugin implements CommandExecutor,TabCompleter {
 private BanManager banManager;private AdminGUI adminGUI;private DetectionManager detectionManager;private RateLimitGuard rateLimitGuard;
 @Override public void onEnable(){
  saveDefaultConfig();banManager=new BanManager(this);adminGUI=new AdminGUI(this,banManager);detectionManager=new DetectionManager(this);rateLimitGuard=new RateLimitGuard(this);
  getServer().getPluginManager().registerEvents(adminGUI,this);getServer().getPluginManager().registerEvents(detectionManager,this);getServer().getPluginManager().registerEvents(rateLimitGuard,this);
  var cmd=getCommand("purpleanticheat");if(cmd==null){getLogger().severe("Command missing from plugin.yml; disabling.");getServer().getPluginManager().disablePlugin(this);return;}cmd.setExecutor(this);cmd.setTabCompleter(this);
  getLogger().info("PurpleAnticheat enabled: admin GUI, response pipeline and rate guards loaded.");
 }
 @Override public void onDisable(){if(banManager!=null)banManager.save();}
 @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
  if(!(sender instanceof Player p)){sender.sendMessage("This command is available in-game only.");return true;}
  if(!p.isOp()||!p.hasPermission("purpleanticheat.admin")){p.sendMessage(color("&cYou need OP and purpleanticheat.admin."));return true;}
  if(args.length==1&&args[0].equalsIgnoreCase("banedit")){adminGUI.openMain(p);return true;}
  if(args.length==2&&args[0].equalsIgnoreCase("console")&&args[1].equalsIgnoreCase("edit")){adminGUI.openConsoleEdit(p);return true;}
  p.sendMessage(color(getConfig().getString("messages.prefix","&5Purple &dAnticheat &8» ")+"&7Usage: &f/"+label+" banedit &7or &f/"+label+" console edit"));return true;
 }
 @Override public List<String> onTabComplete(CommandSender s,Command c,String a,String[] args){
  if(!(s instanceof Player p)||!p.isOp()||!p.hasPermission("purpleanticheat.admin"))return List.of();
  if(args.length==1)return List.of("banedit","console").stream().filter(x->x.startsWith(args[0].toLowerCase())).toList();
  if(args.length==2&&args[0].equalsIgnoreCase("console"))return List.of("edit").stream().filter(x->x.startsWith(args[1].toLowerCase())).toList();return List.of();
 }
 public boolean punishDetection(String check,String player,String detail){
  if(!getConfig().getBoolean("autopunish.enabled",true)||check==null)return false;String id=check.toLowerCase(java.util.Locale.ROOT),path="autopunish.checks."+id;
  if(!getConfig().getBoolean(path+".enabled",false))return false;String duration=getConfig().getString(path+".duration","1d"),reason="PurpleAnticheat: "+(detail==null||detail.isBlank()?"validated detection":detail.trim());
  BanManager.BanResult result=banManager.ban(player,reason,duration,"PurpleAnticheat");if(result==BanManager.BanResult.SUCCESS)getLogger().warning("Automatic punishment applied: check="+id+", player="+player+", duration="+duration);return result==BanManager.BanResult.SUCCESS;
 }
 public boolean reportSignal(Player p,String check,String evidence){return detectionManager!=null&&detectionManager.reportSignal(p,check,evidence);}
 public static String color(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}
 public BanManager getBanManager(){return banManager;}public AdminGUI getAdminGUI(){return adminGUI;}public DetectionManager getDetectionManager(){return detectionManager;}
}