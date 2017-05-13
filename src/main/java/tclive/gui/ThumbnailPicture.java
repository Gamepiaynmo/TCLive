package tclive.gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ITickableTextureObject;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import tclive.TCLiveMod;

public class ThumbnailPicture extends AbstractTexture implements ITickableTextureObject {
	
	int index;
	public final ResourceLocation THUMBNAIL_TEXTURE;
	boolean needUpdate;
	BufferedImage image;
	public static int width = 480, height = 270;
	
	public ThumbnailPicture(int id) {
		index = id;
		THUMBNAIL_TEXTURE = new ResourceLocation("tclive:textures/thumbnail" + id + ".png");
	}

	@Override
	public void loadTexture(IResourceManager resourceManager) throws IOException {
		GlStateManager.enableTexture2D();
		this.deleteGlTexture();
		GlStateManager.bindTexture(getGlTextureId());
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) null);
	}
	
	public void updateTexture(BufferedImage image) {
		this.image = image;
		needUpdate = true;
	}

	@Override
	public void tick() {
		if (needUpdate) {
			//int scale = new ScaledResolution(TCLiveMod.mc).getScaleFactor();
			TextureUtil.uploadTextureImageSub(this.getGlTextureId(), image, (width - image.getWidth()) / 2, (height - image.getHeight()) / 2, true, false);
			needUpdate = false;
		}
	}

}
