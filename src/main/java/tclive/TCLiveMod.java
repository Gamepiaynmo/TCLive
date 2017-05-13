package tclive;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import tclive.gui.InputVerifyCode;
import tclive.gui.PostWindow;
import tclive.gui.VerifyCodePicture;
import tclive.tieba.LoginStatus;
import tclive.util.Utils;

@Mod(modid = TCLiveMod.MODID, name = "Dim", version = TCLiveMod.VERSION, guiFactory = "tclive.gui.LiveConfig")
public class TCLiveMod {

	public static final String MODID = "tclive";
	public static final String VERSION = "@version@";
	
	public static final Logger LOG = LogManager.getLogger();
	
	@Instance(MODID)
	public static TCLiveMod mod;
	public static Minecraft mc;
	
	public Configuration config;
	
	public String tb_username;
	public String tb_password;
	public String tb_livetid;
	public String tb_tail;
	
	public static final KeyBinding tb_post = new KeyBinding("live.key.post", KeyConflictContext.IN_GAME, Keyboard.KEY_F6, "live.key.category");

	public CookieStore cookieStore = new BasicCookieStore();
	public CloseableHttpClient httpClient;
	public JsonParser jsonParser = new JsonParser();
	
	public LoginStatus loginStatus;
	public VerifyCodePicture verifyCodePicture;
	
	public TCLiveMod() {
		mod = this;
		mc = Minecraft.getMinecraft();
		loginStatus = new LoginStatus();
	}
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		loadConfig(config);
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event) {
		ClientRegistry.registerKeyBinding(tb_post);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		mc.getTextureManager().loadTickableTexture(VerifyCodePicture.VERICODE_TEXTURE, verifyCodePicture = new VerifyCodePicture());
		PostWindow.loadThumbnailPictures();
	}
	
	private void loadConfig(Configuration config) {
		config.load();
		updateConfig(config);
	}
	
	public void updateConfig(Configuration config) {
		tb_username = config.getString("username", "live", "", "", "live.config.username");
		tb_password = config.getString("password", "live", "", "", "live.config.password");
		tb_livetid = config.getString("livetid", "live", "", "", "live.config.livetid");
		tb_tail = config.getString("tail", "live", "\\n\\n\\n\\n                                        ---- From TCLive", "", "live.config.tail");
		loginStatus.resetStatus();
		config.save();
	}
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		if (event.phase == Phase.START && mc.currentScreen == null) {
			if (tb_post.isPressed()) {
				if (!loginStatus.hasLogined())
					tryLogin();
				else
					mc.displayGuiScreen(new PostWindow());
			}
		}
	}
	
	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		updateConfig(config);
		if (!loginStatus.hasLogined())
			tryLogin();
	}
	
	private void tryLogin() {
		new Thread(() -> {
			while (mc.currentScreen != null)
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			loginStatus.tryLogin();
			Utils.handleLoginErrorCode(loginStatus.getLastError());
		}).start();
	}
	
}
