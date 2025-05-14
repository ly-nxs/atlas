package xyz.lynxs.terrarium.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class TerrariumScrollableWidget extends ScrollableWidget {
    private double scrollAmount;
    private boolean scrolling;
    private final int contentHeight;
    @Nullable
    private Runnable onScroll;
    private final List<ClickableWidget> childWidgets = new ArrayList<>();


    public TerrariumScrollableWidget(MinecraftClient client, int x, int y, int width, int height, int contentHeight) {
        super(x, y, width, height, Text.empty());
        this.contentHeight = contentHeight;
    }
    public <T extends ClickableWidget> T addChild(T widget) {
        this.childWidgets.add(widget);
        return widget;
    }

    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        // Enable scissor test for content clipping
        context.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);

        // Apply scroll translation
        context.getMatrices().push();
        context.getMatrices().translate(0.0, -this.scrollAmount, 0.0);

        // Render background
        this.renderBackground(context, mouseX, mouseY, delta);

        // Render content
        this.renderContents(context, mouseX, mouseY, delta);

        // Restore matrix state
        context.getMatrices().pop();
        context.disableScissor();

        // Render scroll bar
        this.renderScrollBar(context);
    }

    protected void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Default background - override if needed

    }


    protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render child widgets
        context.getMatrices().push();
        context.getMatrices().translate(0, -this.scrollAmount, 0);

        for (ClickableWidget widget : childWidgets) {
            widget.render(context, mouseX, (int) (mouseY + this.getScrollAmount()), delta);
        }

        context.getMatrices().pop();
    }

    protected void renderScrollBar(DrawContext context) {
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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.visible || !this.active) {
            return false;
        }
        // First try child widgets
        for (ClickableWidget widget : childWidgets) {
            if (widget.mouseClicked(mouseX, mouseY + 2 * this.getScrollAmount(), button)) {
                return true;
            }
        }

        // Then handle scroll bar
        if (button == 0 && mouseX >= this.getX() + this.width - 6) {
            this.scrolling = true;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY , button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!this.visible || !this.active) {
            return false;
        }
        for (ClickableWidget widget : childWidgets) {
            if (widget.mouseClicked(mouseX, mouseY + 2 * this.getScrollAmount(), button)) {
                return true;
            }
        }
        if (this.scrolling) {
            double scrollableHeight = this.contentHeight - this.height;
            double scrollDelta = deltaY / this.height * scrollableHeight;
            this.setScrollAmount(this.scrollAmount + scrollDelta);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY , button);
    }


    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!this.visible || !this.active) {
            return false;
        }
        this.setScrollAmount(this.scrollAmount - verticalAmount * 20);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.scrolling = false;
        return super.mouseReleased(mouseX, mouseY + this.getScrollAmount(), button);
    }

    public double getScrollAmount() {
        return this.scrollAmount;
    }

    public void setScrollAmount(double amount) {
        this.scrollAmount = MathHelper.clamp(amount, 0.0, Math.max(0, this.contentHeight - this.height));
        if (this.onScroll != null) {
            this.onScroll.run();
        }
    }

    public void setOnScroll(@Nullable Runnable onScroll) {
        this.onScroll = onScroll;
    }
}