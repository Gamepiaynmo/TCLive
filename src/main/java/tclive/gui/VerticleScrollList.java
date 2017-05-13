package tclive.gui;

import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;

public abstract class VerticleScrollList {

    protected final Minecraft mc = Minecraft.getMinecraft();
    
    public int x, y, width, slotCnt, slotHeight, scrollWidth, height;
    protected int scrollPos = 0;

	private int mouseX;

	private int mouseY;
    
    public VerticleScrollList(int x, int y, int width, int slotCnt, int slotHeight, int scrollWidth) {
    	this.x = x;
    	this.y = y;
    	this.width = width;
    	this.slotCnt = slotCnt;
    	this.slotHeight = slotHeight;
    	this.scrollWidth = scrollWidth;
    	height = slotCnt * slotHeight;
    }
    
    public void init(int x, int y, int width, int slotCnt, int slotHeight, int scrollWidth) {
    	this.x = x;
    	this.y = y;
    	this.width = width;
    	this.slotCnt = slotCnt;
    	this.slotHeight = slotHeight;
    	this.scrollWidth = scrollWidth;
    	height = slotCnt * slotHeight;
    }

    public abstract GuiListExtended.IGuiListEntry getListEntry(int index);
    protected abstract int getSize();
    protected abstract void elementClicked(int slotIndex, int button, int mouseX, int mouseY);
    
    private int getMaxScroll() { return Math.max(0, getSize() - slotCnt); }
    
    public int getSlotIndexFromScreenCoords(int posX, int posY) {
    	if (posX < x || posX > x + width || posY < y || posY > y + height) return -1;
    	int res = (posY - y) / slotHeight + scrollPos;
    	return res < 0 ? -1 : res >= getSize() ? -1 : res;
    }
    
    public boolean isMouseInArea(int posX, int posY) {
    	return !(posX < x || posX >= x + width + scrollWidth || posY < y || posY >= y + height);
    }
    
    public void handleMouseInput() {
    	if (!isMouseInArea(mouseX, mouseY)) return;
    	if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
    		int selected = getSlotIndexFromScreenCoords(mouseX, mouseY);
    		if (selected != -1) {
    			getListEntry(selected).mousePressed(selected, mouseX, mouseY, 0, mouseX - x, (mouseY - y) % slotHeight);
    			elementClicked(selected, Mouse.getEventButton(), mouseX, mouseY);
    		}
    	}
    	int dwheel = Mouse.getEventDWheel();
    	int dpos = dwheel > 0 ? -1 : dwheel < 0 ? 1 : 0;
    	int newPos = MathHelper.clamp(scrollPos + dpos, 0, getMaxScroll());
    	if (newPos != scrollPos) {
    		onScroll(newPos - scrollPos);
    		scrollPos = newPos;
    	}
    }
    
    public void onScroll(int dscroll) {}
    
    public void drawScreen(int mouseXIn, int mouseYIn, float partialTicks) {
    	mouseX = mouseXIn; mouseY = mouseYIn;
        Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer vertexbuffer = tessellator.getBuffer();
        
        scrollPos = MathHelper.clamp(scrollPos, 0, getMaxScroll());
        GlStateManager.disableTexture2D();
        int maxScroll = getMaxScroll();
        int scrollmin = y + (maxScroll == 0 ? 0 : height * scrollPos / getSize());
        int scrollmax = y + (maxScroll == 0 ? height : height * (scrollPos + slotCnt) / getSize());
        int i = x + width, j = i + scrollWidth;
        vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        vertexbuffer.pos((double)i, (double)y, 0.0D).tex(0.0D, 1.0D).color(0, 0, 0, 255).endVertex();
        vertexbuffer.pos((double)j, (double)y, 0.0D).tex(1.0D, 1.0D).color(0, 0, 0, 255).endVertex();
        vertexbuffer.pos((double)j, (double)(y + height), 0.0D).tex(1.0D, 0.0D).color(0, 0, 0, 255).endVertex();
        vertexbuffer.pos((double)i, (double)(y + height), 0.0D).tex(0.0D, 0.0D).color(0, 0, 0, 255).endVertex();
        tessellator.draw();
        vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        vertexbuffer.pos((double)i, (double)scrollmax, 0.0D).tex(0.0D, 1.0D).color(128, 128, 128, 255).endVertex();
        vertexbuffer.pos((double)j, (double)scrollmax, 0.0D).tex(1.0D, 1.0D).color(128, 128, 128, 255).endVertex();
        vertexbuffer.pos((double)j, (double)scrollmin, 0.0D).tex(1.0D, 0.0D).color(128, 128, 128, 255).endVertex();
        vertexbuffer.pos((double)i, (double)scrollmin, 0.0D).tex(0.0D, 0.0D).color(128, 128, 128, 255).endVertex();
        tessellator.draw();
        vertexbuffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        vertexbuffer.pos((double)(i + 1), (double)(scrollmax - 1), 0.0D).tex(0.0D, 1.0D).color(192, 192, 192, 255).endVertex();
        vertexbuffer.pos((double)(j - 1), (double)(scrollmax - 1), 0.0D).tex(1.0D, 1.0D).color(192, 192, 192, 255).endVertex();
        vertexbuffer.pos((double)(j - 1), (double)scrollmin + 1, 0.0D).tex(1.0D, 0.0D).color(192, 192, 192, 255).endVertex();
        vertexbuffer.pos((double)(i + 1), (double)scrollmin + 1, 0.0D).tex(0.0D, 0.0D).color(192, 192, 192, 255).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        
        int size = Math.min(getSize(), scrollPos + slotCnt);
		int selected = getSlotIndexFromScreenCoords(mouseX, mouseY);
        for (i = scrollPos; i < size; i++) {
        	getListEntry(i).drawEntry(i, x, y + slotHeight * (i - scrollPos), width, slotHeight, mouseXIn, mouseYIn, selected == i);
        }
    }
}
