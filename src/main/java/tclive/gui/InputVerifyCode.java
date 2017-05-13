package tclive.gui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import tclive.TCLiveMod;
import tclive.util.Utils;

public class InputVerifyCode extends GuiScreen {
	
	private GuiTextField verifyCodeField;
	private GuiButton loginButton, cancelButton;
	private String textString;
	boolean inited;
	
	public InputVerifyCode(boolean wrongCode) {
		textString = I18n.format(wrongCode ? "live.gui.vericode" : "live.gui.wrongcode");
	}

	@Override
	public void initGui() {
		this.buttonList.clear();
		loginButton = addButton(new GuiButton(0, this.width / 2 - 155, 150, 150, 20, I18n.format("live.gui.login")));
		cancelButton = addButton(new GuiButton(1, this.width / 2 - 155 + 160, 150, 150, 20, I18n.format("live.gui.cancel")));
		verifyCodeField = new GuiTextField(2, this.fontRendererObj, this.width / 2 - 100, 126, 200, 20);
		verifyCodeField.setFocused(true);
		TCLiveMod.mod.verifyCodePicture.updatePicture();
		inited = true;
	}
	
	@Override
	protected void actionPerformed(GuiButton button) {
		switch (button.id) {
		case 0:
			TCLiveMod.mc.displayGuiScreen(null);
			new Thread(() -> {
				TCLiveMod.mod.loginStatus.tryLoginWithCode(verifyCodeField.getText());
				Utils.handleLoginErrorCode(TCLiveMod.mod.loginStatus.getLastError());
			}).start();
			break;
		case 1:
			TCLiveMod.mc.displayGuiScreen(null);
			TCLiveMod.mod.loginStatus.resetStatus();
			break;
		}
	}
	
	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		verifyCodeField.mouseClicked(mouseX, mouseY, mouseButton);
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (verifyCodeField.isFocused()) {
			verifyCodeField.textboxKeyTyped(typedChar, keyCode);
		}
		if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)
			this.actionPerformed(loginButton);
		super.keyTyped(typedChar, keyCode);
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (inited) {
	        this.drawDefaultBackground();
	        verifyCodeField.drawTextBox();
	        this.drawCenteredString(this.fontRendererObj, this.textString, this.width / 2, 70, 16777215);
	        mc.getTextureManager().bindTexture(VerifyCodePicture.VERICODE_TEXTURE);
	        this.drawModalRectWithCustomSizedTexture(this.width / 2 - 50, 82, 0, 0, 100, 40, 100, 40);
	        super.drawScreen(mouseX, mouseY, partialTicks);
		}
	}
	
}
