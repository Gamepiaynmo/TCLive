package tclive.gui;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.IntBuffer;

import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITickableTextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import tclive.TCLiveMod;

public class VerifyCodePicture extends AbstractTexture implements ITickableTextureObject {

	public static final ResourceLocation VERICODE_TEXTURE = new ResourceLocation("tclive:textures/verify_code.png");
	
	private boolean needToUpdate = false;
	private static final String picturePath = "./veri_code.png";

	@Override
	public void loadTexture(IResourceManager resourceManager) throws IOException {
		GlStateManager.enableTexture2D();
		this.deleteGlTexture();
		GlStateManager.bindTexture(getGlTextureId());
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 100, 40, 0, GL11.GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
	}
	
	public void updatePicture() {
		needToUpdate = true;
	}

	@Override
	public void tick() {
		if (needToUpdate) {
			this.deleteGlTexture();
			try (FileInputStream input = new FileInputStream(picturePath)) {
				BufferedImage bufferedimage = TextureUtil.readBufferedImage(input);
	            TextureUtil.uploadTextureImageAllocate(this.getGlTextureId(), bufferedimage, false, false);
			} catch (IOException e) {
				TCLiveMod.LOG.warn("Failed to load verify code picture.");
			}
			needToUpdate = false;
		}
	}
}
