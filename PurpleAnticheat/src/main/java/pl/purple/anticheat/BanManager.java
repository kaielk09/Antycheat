package pl.purple.anticheat;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
/** Manual moderation records and Bukkit name bans. */
public final class BanManager {
 private final PurpleAnticheat plugin; private final File file; private final YamlConfiguration data;
 public BanManager(PurpleAnticheat plugin){this.plugin=plugin;file=new File(plugin.getDataFolder(),"punishments.yml");if(!file.exists())try{file.getParentFile().mkdirs();file.createNewFile();}catch(Exception ex){plugin.getLogger().warning("Could not create punishments.yml: "+ex.getMessage());}data=YamlConfiguration.loadConfiguration(file);}
 public BanResult ban(String playerName,String reason,String durationText,String staff){
  if(playerName==null||playerName.isBlank()||reason==null||reason.isBlank()||staff==null||staff.isBlank())return BanResult.INVALID_INPUT;
  DurationParse parsed=parseDuration(durationText);if(!parsed.valid())return BanResult.INVALID_DURATION;
  OfflinePlayer target=Bukkit.getOfflinePlayer(playerName.trim());if(target.getName()==null)return BanResult.PLAYER_NOT_FOUND;
  Date expiry=parsed.permanent()?null:Date.from(Instant.now().plus(parsed.duration()));String name=target.getName(),safeReason=reason.trim();
  Bukkit.getBanList(BanList.Type.NAME).addBan(name,safeReason,expiry,staff.trim());
  String id="history."+UUID.randomUUID();data.set(id+".player",name);data.set(id+".reason",safeReason);data.set(id+".duration",parsed.display());data.set(id+".staff",staff.trim());data.set(id+".time",Instant.now().toString());data.set(id+".expiry",expiry==null?"permanent":expiry.toInstant().toString());save();
  Player online=target.getPlayer();if(online!=null)online.kickPlayer(banScreen(safeReason,parsed.display(),staff.trim()));return BanResult.SUCCESS;
 }
 public static DurationParse parseDuration(String input){
  if(input==null)return new DurationParse(false,false,null,"");String s=input.trim().toLowerCase();
  if(s.equals("perm")||s.equals("permanent")||s.equals("perma"))return new DurationParse(true,true,null,"Permanent");
  if(s.length()<2)return new DurationParse(false,false,null,"");
  try{long n=Long.parseLong(s.substring(0,s.length()-1));char u=s.charAt(s.length()-1);if(n<1||n>100000)return new DurationParse(false,false,null,"");
   Duration d=switch(u){case 'm'->Duration.ofMinutes(n);case 'h'->Duration.ofHours(n);case 'd'->Duration.ofDays(n);case 'w'->Duration.ofDays(Math.multiplyExact(n,7));default->null;};if(d==null)return new DurationParse(false,false,null,"");
   String display=n+switch(u){case 'm'->" minute(s)";case 'h'->" hour(s)";case 'd'->" day(s)";case 'w'->" week(s)";default->"";};return new DurationParse(true,false,d,display);
  }catch(Exception ex){return new DurationParse(false,false,null,"");}
 }
 private String banScreen(String reason,String duration,String staff){String f=plugin.getConfig().getString("messages.ban-format","&7Reason: &f{reason}\\n&7Duration: &f{duration}\\n&7Banned by: &f{staff}");return PurpleAnticheat.color(f.replace("{reason}",reason).replace("{duration}",duration).replace("{staff}",staff).replace("\\n","\n"));}
 public YamlConfiguration history(){return data;}
 public void save(){try{data.save(file);}catch(Exception ex){plugin.getLogger().severe("Failed to save punishments.yml: "+ex.getMessage());}}
 public enum BanResult{SUCCESS,INVALID_INPUT,INVALID_DURATION,PLAYER_NOT_FOUND}
 public record DurationParse(boolean valid,boolean permanent,Duration duration,String display){}
}