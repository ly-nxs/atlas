package xyz.lynxs.terrarium.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollArea;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.List;

public abstract class TerrariumScrollableWidget extends AbstractScrollArea{
    private double scrollAmount;
    private boolean scrolling;
    private final int contentHeight;
    @Nullable
    private Runnable onScroll;
    private final List<AbstractWidget> childWidgets = new ArrayList<>();


    public TerrariumScrollableWidget(Minecraft client, int x, int y, int width, int height, int contentHeight) {
        super(x, y, width, height, Component.empty());
        this.contentHeight = contentHeight;
    }
    public <T extends AbstractWidget> void addChild(T widget) {
        this.childWidgets.add(widget);
    }

    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        // Enable scissor test for content clipping
        context.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);

        // Apply scroll translation
        context.pose().pushMatrix();
        context.pose().translate(0.0F, (float) -this.scrollAmount, new Matrix3x2f());

        // Render background
        this.renderBackground(context, mouseX, mouseY, delta);

        // Render content
        this.renderContents(context, mouseX, mouseY, delta);

        // Restore matrix state
        context.pose().popMatrix();
        context.disableScissor();

        // Render scroll bar
        this.renderScrollBar(context);
    }

    protected void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Default background - override if needed

    }


    protected void renderContents(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Render child widgets
        context.pose().pushMatrix();
        context.pose().translate( 0.0F, (float) -this.scrollAmount, new Matrix3x2f());

        for (AbstractWidget widget : childWidgets) {
            widget.render(context, mouseX, (int) (mouseY + this.getScrollAmount()), delta);
        }

        context.pose().popMatrix();
    }

    protected void renderScrollBar(GuiGraphics context) {
        int scrollBarWidth = 6;
        int scrollBarX = this.getX() + this.width - scrollBarWidth - 2;

        // Scroll track
        context.fill(scrollBarX, this.getY(),
                scrollBarX + scrollBarWidth,
                this.getY() + this.height,
                0xFF606060);

        // Calculate thumb size and position
        int thumbHeight = Math.max(30, (int)((float)this.height / this.contentHeight * this.height));
        int thumbY = this.getY() + (int)((this.scrollAmount / (this.contentHeight - this.height)) * (this.height - thumbHeight));

        // Scroll thumb
        context.fill(scrollBarX, thumbY,
                scrollBarX + scrollBarWidth,
                thumbY + thumbHeight,
                0xFFC0C0C0);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (!this.visible || !this.active) {
            return false;
        }
        // First try child widgets
        for (AbstractWidget widget : childWidgets) {
            if (widget.mouseClicked(new MouseButtonEvent(event.x(), event.y() + 2 * this.getScrollAmount(), event.buttonInfo()), bl)) {
                return true;
            }
        }

        // Then handle scroll bar
        if (bl && event.x() >= this.getX() + this.width - 6) {
            this.scrolling = true;
            return true;
        }

        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (!this.visible || !this.active) {
            return false;
        }

        for (AbstractWidget widget : childWidgets) {
            if (widget.mouseClicked(new MouseButtonEvent(event.x() + deltaX, event.y(),event.buttonInfo()), true)) {
                return true;
            }
        }

        if (this.scrolling) {
            double scrollableHeight = this.contentHeight - this.height;
            double scrollDelta = deltaY / this.height * scrollableHeight;
            this.setScrollAmount(this.scrollAmount + scrollDelta);
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }


    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.visible || !this.active) {
            return false;
        }
        this.setScrollAmount(this.scrollAmount - verticalAmount * 20);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.scrolling = false;
        return super.mouseReleased(event);
    }

    public double getScrollAmount() {
        return this.scrollAmount;
    }

    public void setScrollAmount(double amount) {
        this.scrollAmount = Mth.clamp(amount, 0.0, Math.max(0, this.contentHeight - this.height));
        if (this.onScroll != null) {
            this.onScroll.run();
        }
    }

    public void setOnScroll(@Nullable Runnable onScroll) {
        this.onScroll = onScroll;
    }
}