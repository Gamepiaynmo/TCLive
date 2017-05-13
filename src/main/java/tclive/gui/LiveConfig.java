package tclive.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.IModGuiFactory;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;
import tclive.TCLiveMod;

public class LiveConfig implements IModGuiFactory {
	
	public static class ConfigScreen extends GuiConfig {

		public ConfigScreen(GuiScreen parentScreen) {
			super(parentScreen, getConfigElements(), TCLiveMod.MODID, false, false, I18n.format("live.config.title"));
		}
		
        private static List<IConfigElement> getConfigElements() {
            return new ConfigElement(TCLiveMod.mod.config.getCategory("live")).getChildElements();
        }
        
        @Override
        public void onGuiClosed() {
        	TCLiveMod.mod.updateConfig(TCLiveMod.mod.config);
        }
	}

	@Override
	public void initialize(Minecraft minecraftInstance) {
		
	}

	@Override
	public Class<? extends GuiScreen> mainConfigGuiClass() {
		return ConfigScreen.class;
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return null;
	}

	@Override
	public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
		return null;
	}

}
