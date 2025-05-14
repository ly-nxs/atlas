package xyz.lynxs.terrarium.gui;


import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import xyz.lynxs.terrarium.TerrariumConfig;
import xyz.lynxs.terrarium.preset.PresetConfig;

import java.nio.file.Path;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;
import static xyz.lynxs.terrarium.Terrarium.MOD_ID;
import static xyz.lynxs.terrarium.Util.*;

public class TerrariumCustomizeScreen extends Screen {
    private final CreateWorldScreen parent;
    private  ButtonWidget doneButton, cancelButton;
    private Slider zoomSlider, worldHeightSlider, xOffsetSlider, zOffsetSlider, noiseScaleSlider, monthSlider, startingYSlider;
    private final PresetConfig config;
    private TerrariumScrollableWidget scrollWidget;

    public TerrariumCustomizeScreen(CreateWorldScreen parent) {
        super(Text.translatable(MOD_ID + ".text.config_title"));
        
        this.parent = parent;
        config = new PresetConfig();
    }

    @Override
    protected void init() {
        int buttonWidth = 120;
        int buttonHeight = 20;
        int buttonPadding = 5;

        int panelWidth = this.width;
        int panelHeight = this.height - 75;
        int contentHeight = this.height; // Total height of all your content

        // Create scroll widget
        this.scrollWidget = new TerrariumScrollableWidget(
                this.client, // MinecraftClient instance
                this.width / 2 - panelWidth / 2, // Center horizontally
                30, // Top position
                panelWidth,
                panelHeight,
                contentHeight
        ) {

            @Override
            protected void appendClickableNarrations(NarrationMessageBuilder builder) {

            }


            /**
             * @return 
             */
            @Override
            protected int getContentsHeight() {
                return 0;
            }


            @Override
            protected double getDeltaYPerScroll() {
                return 10;
            }

            @Override
            protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
                // Your content rendering here
                super.renderContents(context,mouseX, (int) (mouseY + this.getScrollAmount()),delta);
            }

        };

        // Add to screen
        this.addDrawableChild(scrollWidget);
        bottomPanel(buttonWidth, buttonHeight, buttonPadding);
        Settings();
    }

    private void Settings(){
        int sliderPadding = 5;
        int sliderWidth = this.width - 2 * sliderPadding;
        int sliderHeight = 20;
        zoomSlider = new Slider(sliderPadding,this.textRenderer.fontHeight + sliderPadding + sliderHeight, sliderWidth, sliderHeight, config.zoom, 0, 15, Text.translatable(MOD_ID + ".text.slider.zoom"), Slider.Format.INT, (slider, value)-> {
            config.zoom = (int) slider.scaleValue(value);
            return value;
        });
        worldHeightSlider = new Slider(sliderPadding,this.textRenderer.fontHeight + sliderPadding * 2 + sliderHeight * 2, sliderWidth, sliderHeight, config.worldHeight, 4, 4096, Text.translatable(MOD_ID + ".text.slider.world_height"), Slider.Format.INT, (slider, value)-> {
            config.worldHeight = (int) slider.scaleValue(value);
            return value;
        });
        startingYSlider = new Slider(sliderPadding,this.textRenderer.fontHeight + sliderPadding * 3 + sliderHeight * 3, sliderWidth, sliderHeight, config.startingY, -64, 64, Text.translatable(MOD_ID + ".text.slider.starting_y"), Slider.Format.INT, (slider, value)-> {
            config.startingY = (int) slider.scaleValue(value);
            return value;
        });
        xOffsetSlider = new Slider(sliderPadding,this.textRenderer.fontHeight + sliderPadding * 4 + sliderHeight * 4, sliderWidth, sliderHeight, 0, -180, 180, Text.translatable(MOD_ID + ".text.slider.x_offset"), Slider.Format.FLOAT, (slider, value)-> {
            config.adjustXoffset = lonToX(slider.scaleValue(value), (int) (256 * Math.pow(2, config.zoom)));
            return value;
        });
        zOffsetSlider = new Slider(sliderPadding,this.textRenderer.fontHeight + sliderPadding * 5 + sliderHeight * 5, sliderWidth, sliderHeight, 0, -82, 82, Text.translatable(MOD_ID + ".text.slider.z_offset"), Slider.Format.FLOAT, (slider, value)-> {
            config.adjustZoffset = latToZ(slider.scaleValue(value), (int) (256 * Math.pow(2, config.zoom)));
            return value;
        });
        noiseScaleSlider = new Slider(sliderPadding,this.textRenderer.fontHeight + sliderPadding * 6 + sliderHeight * 6, sliderWidth, sliderHeight, (float) config.noise_biome_scale, 0.000001f, 1.0f, Text.translatable(MOD_ID + ".text.slider.biome_noise"), Slider.Format.FLOAT, (slider, value)-> {
            config.noise_biome_scale = slider.scaleValue(value);
            return value;
        });

        this.scrollWidget.addChild(zoomSlider);
        this.scrollWidget.addChild(noiseScaleSlider);
        this.scrollWidget.addChild(worldHeightSlider);
        this.scrollWidget.addChild(startingYSlider);
        this.scrollWidget.addChild(xOffsetSlider);
        this.scrollWidget.addChild(zOffsetSlider);

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer,
                this.title,
                this.width / 2,
                10,
                0xFFFFFF);
    }

    private void bottomPanel(int buttonWidth, int buttonHeight, int buttonPadding){
        assert this.client != null;

        doneButton = ButtonWidget.builder(ScreenTexts.DONE, (btn) -> {
            CONFIG = config;
            TerrariumConfig.save(CONFIG, Path.of("./saves/" + parent.getWorldCreator().getWorldDirectoryName() + "/terrarium.json"), false);
            this.client.setScreen(parent);
        }).dimensions((this.width / 2) - (buttonWidth + buttonPadding), this.height - (buttonPadding + buttonHeight) , buttonWidth, buttonHeight).build();
        cancelButton = ButtonWidget.builder(ScreenTexts.CANCEL, (btn) -> {
            CONFIG = new PresetConfig();
            this.client.setScreen(parent);
        }).dimensions((this.width/2) + buttonPadding, this.height - (buttonPadding + buttonHeight), buttonWidth, buttonHeight).build();



        this.addDrawableChild(doneButton);
        this.addDrawableChild(cancelButton);
    }
}
