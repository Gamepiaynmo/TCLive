package tclive.gui;

import net.coobird.thumbnailator.Thumbnails;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.google.common.collect.Lists;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ResourceLocation;
import tclive.TCLiveMod;
import tclive.tieba.MakeReply;
import tclive.util.Utils;

public class PostWindow extends GuiScreen {

    private static final ResourceLocation post_tex = new ResourceLocation("tclive:textures/post.png");
    private static final ResourceLocation[] emotion_texes = new ResourceLocation[50];
	public static ThumbnailPicture[] thumbnailPictures = new ThumbnailPicture[4];
	public static PostWindow postWindow;
	ListThumbnailSelection thumbnailList;
	ListEditBox textList;
	MakeReply makeReply = new MakeReply();
	boolean selectEmotion;
	
	@Override
	public void initGui() {
		ListEditBox text = new ListEditBox(this.width / 2 - 28, this.height / 2 - 90, 144, 15, 10, 8);
		if (postWindow != null) {
			text.entries = postWindow.textList.entries;
			text.entries.forEach(entry -> entry.calcSize());
		}
		textList = text;
		postWindow = this;
		this.buttonList.clear();
		thumbnailList = new ListThumbnailSelection(this.width / 2 - 122, this.height / 2 - 94, 80, 4, 45, 8);
		this.addButton(new GuiButton(0, width / 2 - 31, height / 2 + 67, 20, 20, "E"));
		this.addButton(new GuiButton(1, width / 2 - 9, height / 2 + 67, 65, 20, I18n.format("live.gui.post")));
		this.addButton(new GuiButton(2, width / 2 + 58, height / 2 + 67, 65, 20, I18n.format("live.gui.cancel")));
		thumbnailList.onInit();
	}
	
	@Override
	protected void actionPerformed(GuiButton button) {
		switch (button.id) {
		case 0:
			selectEmotion = true;
			break;
		case 1:
			TCLiveMod.mc.displayGuiScreen(null);
			makePost();
			break;
		case 2:
			TCLiveMod.mc.displayGuiScreen(null);
			break;
		}
	}
	
	public void writeString(String text) {
		textList.writeText(text);
	}
	
	private void makePost() {
		new Thread(() -> {
			String content = "";
			try {
				for (ListEditBox.EditBoxEntry entry : textList.entries) {
					String str = entry.text;
					content += replaceImages(str);
					content += URLEncoder.encode("[br]", "utf-8");
				}
				if (TCLiveMod.mod.tb_tail != null)
					content += replaceSymbols(TCLiveMod.mod.tb_tail);
				makeReply.tryPost(content);
			} catch (Exception e) {
				Utils.handlePostErrorCode(-1);
				return;
			}
			Utils.handlePostErrorCode(makeReply.getLastError());
			if (makeReply.getLastError() == 0) {
				postWindow = null;
			}
		}).start();
	}
	
	private String replaceImages(String text) {
		String image = Utils.patternMatch(text, "@\\[pic(?:\\d+)\\]@", 0);
		if (image != null) {
			int start = text.indexOf(image);
			String pre = text.substring(0, start);
			String post = text.substring(start + image.length());
			int imageId = Integer.parseInt(Utils.patternMatch(image, "pic(\\d+)"));
			File imageFile = thumbnailList.getPicFile(imageId);
			if (imageFile != null) {
				image = makeReply.uploadImage(imageFile);
			}
			return replaceEmotions(pre) + image + replaceImages(post);
		} else {
			return replaceEmotions(text);
		}
	}
	
	private String replaceEmotions(String text) {
		String emotion = Utils.patternMatch(text, "@\\[emo(?:\\d+)\\]@", 0);
		if (emotion != null) {
			int start = text.indexOf(emotion);
			String pre = text.substring(0, start);
			String post = text.substring(start + emotion.length());
			int emotionId = Integer.parseInt(Utils.patternMatch(emotion, "emo(\\d+)"));
			emotion = makeReply.getEmotionString(emotionId);
			return replaceSymbols(pre) + emotion + replaceEmotions(post);
		} else {
			return replaceSymbols(text);
		}
	}
	
	private String replaceSymbols(String text) {
		try {
			text = text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("  ", "&ensp;&ensp;").replaceAll("\\\\n", "[br]");
			return URLEncoder.encode(text, "utf-8");
		} catch (Exception e) {
			return text;
		}
	}
	
	@Override
    public void handleMouseInput() throws IOException {
		if (selectEmotion) {
	        int i = Mouse.getEventX() * this.width / this.mc.displayWidth;
	        int j = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
	        if (Mouse.getEventButtonState() && i >= width / 2 - 150 && i < width / 2 + 150 && j >= height / 2 - 75 && j < height / 2 + 75) {
	        	int x = (i - (width / 2 - 150)) / 30;
	        	int y = (j - (height / 2 - 75)) / 30;
	        	PostWindow.postWindow.writeString(String.format("@[emo%02d]@", y * 10 + x + 1));
	        	selectEmotion = false;
	        }
		} else {
			thumbnailList.handleMouseInput();
			textList.handleMouseInput();
			super.handleMouseInput();
		}
	}
	
	@Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		mc.getTextureManager().bindTexture(post_tex);
		this.drawTexturedModalRect(this.width / 2 - 128, this.height / 2 - 100, 0, 0, 256, 200);
		super.drawScreen(mouseX, mouseY, partialTicks);
		GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
		thumbnailList.drawScreen(mouseX, mouseY, partialTicks);
		textList.drawScreen(mouseX, mouseY, partialTicks);
		if (selectEmotion) {
			GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
	    	int x = width / 2 - 150;
	    	int y = height / 2 - 75;
	    	for (int i = 0; i < 5; i++) {
	    		for (int j = 0; j < 10; j++) {
	    			int index = i * 10 + j + 1;
	    			mc.getTextureManager().bindTexture(emotion_texes[index - 1]);
	    			this.drawModalRectWithCustomSizedTexture(x + 30 * j, y + 30 * i, 0, 0, 30, 30, 30, 30);
	    		}
	    	}
		}
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (selectEmotion) {
	    	if (keyCode == Keyboard.KEY_ESCAPE) {
	    		selectEmotion = false;
	    	}
		} else {
			textList.keyTyped(typedChar, keyCode);
			super.keyTyped(typedChar, keyCode);
		}
	}
	
	static class ListThumbnailSelection extends VerticleScrollList {
		
		List<File> picList = Lists.newArrayList();
		ListThumbnailSelectionEntry[] entries = new ListThumbnailSelectionEntry[4];

		public ListThumbnailSelection(int x, int y, int width, int slotCnt, int slotHeight, int scrollWidth) {
			super(x, y, width, slotCnt, slotHeight, scrollWidth);
		}

		@Override
		public IGuiListEntry getListEntry(int index) {
			return entries[index - scrollPos];
		}

		@Override
		protected int getSize() {
			return picList.size();
		}

		@Override
		protected void elementClicked(int slotIndex, int button, int mouseX, int mouseY) {
			PostWindow.postWindow.textList.writeText("@[pic" + slotIndex + "]@");
		}
		
		public void onInit() {
			File[] files = new File(TCLiveMod.mc.mcDataDir, "screenshots").listFiles();
			if (files != null)
				for (int i = files.length - 1; i >= 0; i--)
					picList.add(files[i]);
			for (int i = 0; i < 4; i++) {
				entries[i] = new ListThumbnailSelectionEntry(getPicFile(i), i);
			}
		}
		
		File getPicFile(int index) {
			if (index < 0 || index >= picList.size()) return null;
			return picList.get(index);
		}
		
		@Override
		public void onScroll(int dscroll) {
			if (dscroll > 0) {
				int index = entries[0].texIndex;
				for (int i = 0; i < 3; i++)
					entries[i] = entries[i + 1];
				entries[3] = new ListThumbnailSelectionEntry(getPicFile(scrollPos + 4), index);
			} else {
				int index = entries[3].texIndex;
				for (int i = 3; i > 0; i--)
					entries[i] = entries[i - 1];
				entries[0] = new ListThumbnailSelectionEntry(getPicFile(scrollPos - 1), index);
			}
		}
		
	}
	
	static class ListThumbnailSelectionEntry implements IGuiListEntry {
		
		File picFile;
		public final int texIndex;
		boolean picLoaded;
		
		public ListThumbnailSelectionEntry(File file, int texId) {
			picFile = file;
			texIndex = texId;
			if (file != null) loadImage();
		}
		
		public void loadImage() {
			new Thread(() -> {
				try {
					thumbnailPictures[texIndex].updateTexture(Thumbnails.of(picFile).size(ThumbnailPicture.width, ThumbnailPicture.height).asBufferedImage());
					picLoaded = true;
				} catch (IOException e) {
				}
			}).start();
		}

		@Override
		public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {
			
		}

		@Override
		public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
			TCLiveMod.mc.getTextureManager().bindTexture(thumbnailPictures[texIndex].THUMBNAIL_TEXTURE);
			if (picLoaded) {
				Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 80, 45, 80, 45);
			}
		}

		@Override
		public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
			return true;
		}

		@Override
		public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
			
		}
		
	}
	
	static class ListEditBox extends VerticleScrollList {
		
		static ListEditBox listEditBox;
		
		static class Pos {
			int x, y;
			public Pos(int x, int y) {
				this.x = x;
				this.y = y;
			}
		}
		
		static class EditBoxEntry implements IGuiListEntry {
			
			String text;
			int size;
			
			public EditBoxEntry(String text) { this.text = text; calcSize(); }

			@Override
			public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {
				
			}

			@Override
			public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
				for (EditBoxEntry entry : listEditBox.entries) {
					if (slotIndex < entry.size) {
						TCLiveMod.mc.fontRendererObj.drawStringWithShadow(getString(slotIndex), x, y + 1, 0xffffffff);
						break;
					}
					slotIndex -= entry.size;
				}
			}

			@Override
			public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
				return false;
			}

			@Override
			public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
				
			}
			
			public boolean back(int pos) {
				if (pos > 0 && pos <= text.length()) {
					StringBuilder builder = new StringBuilder(text);
					builder.deleteCharAt(pos - 1);
					text = builder.toString();
					calcSize();
					return true;
				}
				return false;
			}
			
			public boolean write(int pos, String str) {
				if (pos >= 0 && pos <= text.length()) {
					StringBuilder builder = new StringBuilder(text);
					builder.insert(pos, str);
					text = builder.toString();
					calcSize();
					return true;
				}
				return false;
			}
			
			public int getCursorPos(int line, int width) {
				String str = text;
				int res = 0;
				while (line-- > 0) {
					int len = TCLiveMod.mc.fontRendererObj.trimStringToWidth(str, ListEditBox.lineLength).length();
					res += len;
					str = str.substring(len);
				}
				return res + TCLiveMod.mc.fontRendererObj.trimStringToWidth(str, width).length();
			}
			
			public Pos getDrawPos(int pos) {
				String str = text;
				int line = 0;
				while (true) {
					int len = TCLiveMod.mc.fontRendererObj.trimStringToWidth(str, ListEditBox.lineLength).length();
					if (pos <= len) {
						return new Pos(TCLiveMod.mc.fontRendererObj.getStringWidth(str.substring(0, pos)), line * listEditBox.slotHeight);
					}
					pos -= len;
					line++;
					str = str.substring(len);
				}
			}
			
			void calcSize() {
				size = 0;
				String str = text;
				while (!str.isEmpty()) {
					str = str.substring(TCLiveMod.mc.fontRendererObj.trimStringToWidth(str, ListEditBox.lineLength).length());
					size++;
				}
				size = Math.max(size, 1);
			}
			
			String getString(int line) {
				String str = text;
				while (line-- > 0) {
					str = str.substring(TCLiveMod.mc.fontRendererObj.trimStringToWidth(str, ListEditBox.lineLength).length());
				}
				return str.substring(0, TCLiveMod.mc.fontRendererObj.trimStringToWidth(str, ListEditBox.lineLength).length());
			}
			
		}

	    private final FontRenderer fontRenderer;
	    final static int lineLength = 140;
		
		int cursorLine, cursorPos;
		List<EditBoxEntry> entries = Lists.newArrayList();

		public ListEditBox(int x, int y, int width, int slotCnt, int slotHeight, int scrollWidth) {
			super(x, y, width, slotCnt, slotHeight, scrollWidth);
			fontRenderer = TCLiveMod.mc.fontRendererObj;
			entries.add(new EditBoxEntry(""));
			listEditBox = this;
		}

		@Override
		public IGuiListEntry getListEntry(int index) {
			for (EditBoxEntry entry : entries) {
				if (index < entry.size)
					return entry;
				index -= entry.size;
			}
			return null;
		}

		@Override
		protected int getSize() {
			int res = 0;
			for (EditBoxEntry entry : entries) {
				res += entry.size;
			}
			return res;
		}

		@Override
		protected void elementClicked(int slotIndex, int button, int mouseX, int mouseY) {
			int id = 0;
			for (EditBoxEntry entry : entries) {
				if (slotIndex < entry.size) {
					cursorLine = id;
					cursorPos = entry.getCursorPos(slotIndex, mouseX - this.x);
					break;
				}
				slotIndex -= entry.size;
				id++;
			}
		}
		
		public void writeText(String text) {
			if (entries.get(cursorLine).write(cursorPos, text))
				cursorPos += text.length();
		}
		
		public void enter() {
			EditBoxEntry entry = entries.get(cursorLine);
			entries.set(cursorLine, new EditBoxEntry(entry.text.substring(0, cursorPos)));
			entries.add(cursorLine + 1, new EditBoxEntry(entry.text.substring(cursorPos)));
			cursorPos = 0;
			cursorLine++;
	    	int lines = 0;
	    	for (int i = 0; i < cursorLine; i++)
	    		lines += entries.get(i).size;
			if (lines >= scrollPos + 15)
				scrollPos++;
		}
		
		public void back() {
			EditBoxEntry entry = entries.get(cursorLine);
			if (entry.back(cursorPos))
				cursorPos--;
			else if (cursorPos == 0 && cursorLine != 0) {
				entries.remove(entry);
				cursorLine--;
		    	int lines = 0;
		    	for (int i = 0; i < cursorLine; i++)
		    		lines += entries.get(i).size;
				if (lines <= scrollPos)
					scrollPos--;
				cursorPos = entries.get(cursorLine).text.length();
				entries.get(cursorLine).text += entry.text;
				entries.get(cursorLine).calcSize();
			}
		}
		
		protected void keyTyped(char typedChar, int keyCode) throws IOException {
			if (keyCode == Keyboard.KEY_BACK) {
				back();
			} else if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
				enter();
			} else if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
				this.writeText(Character.toString(typedChar));
			}
		}
		
		@Override
	    public void drawScreen(int mouseXIn, int mouseYIn, float partialTicks) {
	    	super.drawScreen(mouseXIn, mouseYIn, partialTicks);
	    	Pos pos = entries.get(cursorLine).getDrawPos(cursorPos);
	    	int x = pos.x + this.x;
	    	int lines = 0;
	    	for (int i = 0; i < cursorLine; i++)
	    		lines += entries.get(i).size;
	    	int y = pos.y + 1 + this.y + (lines - scrollPos) * slotHeight;
	    	if (y > this.y && y < this.y + this.height)
	    		Gui.drawRect(x - 1, y, x, y + 8, 0xffffff00);
	    }
		
	}
	
	public static void loadThumbnailPictures() {
		for (int i = 0; i < 4; i++) {
			thumbnailPictures[i] = new ThumbnailPicture(i);
			TCLiveMod.mc.getTextureManager().loadTickableTexture(thumbnailPictures[i].THUMBNAIL_TEXTURE, thumbnailPictures[i]);
		}
	}

    static {
    	for (int i = 1; i <= 50; i++) {
    		emotion_texes[i - 1] = new ResourceLocation("tclive:textures/pic" + i + ".png");
    	}
    }

}
