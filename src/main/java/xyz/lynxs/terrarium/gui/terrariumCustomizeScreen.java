package xyz.lynxs.terrarium.gui;


import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.lynxs.terrarium.TerrariumConfig;
import xyz.lynxs.terrarium.preset.presetConfig;

import java.nio.file.Path;

import static xyz.lynxs.terrarium.Terrarium.CONFIG;
import static xyz.lynxs.terrarium.Terrarium.MOD_ID;
import static xyz.lynxs.terrarium.Util.gridToLatLon;
import static xyz.lynxs.terrarium.Util.truncate;

public class terrariumCustomizeScreen extends Screen {
    private final CreateWorldScreen parent;
    private  ButtonWidget doneButton, cancelButton;
    private Slider zoomSlider, worldHeightSlider, xOffsetSlider, zOffsetSlider, noiseScaleSlider, monthSlider;
    private double[] latlon;
    private final presetConfig config;


    public terrariumCustomizeScreen(CreateWorldScreen parent) {
        super(ScreenTexts.EMPTY);
        this.parent = parent;
        config = new presetConfig();
        latlon = gridToLatLon(config.adjustXoffset, config.adjustZoffset,(int) (256 * Math.pow(2, config.zoom)));
    }

    @Override
    protected void init() {
        int buttonWidth = 120;
        int buttonHeight = 20;
        int buttonPadding = 5;



        bottomPanel(buttonWidth, buttonHeight, buttonPadding);
        Settings();
    }
    private void Settings(){
        int sliderPadding = 10;
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
        xOffsetSlider = new Slider(sliderPadding,this.textRenderer.fontHeight + sliderPadding * 3 + sliderHeight * 3, sliderWidth, sliderHeight, config.adjustXoffset, 0, (int) (256 * Math.pow(2, config.zoom)), Text.translatable(MOD_ID + ".text.slider.x_offset"), Slider.Format.INT, (slider, value)-> {
            config.adjustXoffset =  ((int) slider.scaleValue(value) - ((int) (slider.scaleValue(value) )% 16));
            latlon = gridToLatLon(config.adjustXoffset, config.adjustZoffset, (int) (256 * Math.pow(2, config.zoom)));
            return value;
        });
        zOffsetSlider = new Slider(sliderPadding,this.textRenderer.fontHeight + sliderPadding * 4 + sliderHeight * 4, sliderWidth, sliderHeight, config.adjustZoffset, 0, (int) (256 * Math.pow(2, config.zoom)), Text.translatable(MOD_ID + ".text.slider.z_offset"), Slider.Format.INT, (slider, value)-> {
            config.adjustZoffset = ((int) slider.scaleValue(value) - ((int)(slider.scaleValue(value)) % 16));
            latlon = gridToLatLon(config.adjustXoffset, config.adjustZoffset, (int) (256 * Math.pow(2, config.zoom)));
            return value;
        });
        noiseScaleSlider = new Slider(sliderPadding,this.textRenderer.fontHeight + sliderPadding * 5 + sliderHeight * 5, sliderWidth, sliderHeight, (float) config.noise_biome_scale, 0.000001f, 1.0f, Text.translatable(MOD_ID + ".text.slider.biome_noise"), Slider.Format.FLOAT, (slider, value)-> {
            config.noise_biome_scale = slider.scaleValue(value);
            return value;
        });
        monthSlider = new Slider(sliderPadding,this.textRenderer.fontHeight + sliderPadding * 6 + sliderHeight * 6, sliderWidth, sliderHeight, config.month, 0, 11, Text.translatable(MOD_ID + ".text.slider.month"), Slider.Format.INT, (slider, value)-> {
            config.month = (int) slider.scaleValue(value);
            return value;
        });

        this.addDrawableChild(zoomSlider);
        this.addDrawableChild(noiseScaleSlider);
        this.addDrawableChild(worldHeightSlider);
        this.addDrawableChild(xOffsetSlider);
        this.addDrawableChild(zOffsetSlider);
        this.addDrawableChild(monthSlider);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Minecraft doesn't have a "label" widget, so we'll have to draw our own text.
        // We'll subtract the font height from the Y position to make the text appear above the button.
        // Subtracting an extra 10 pixels will give the text some padding.
        // textRenderer, text, x, y, color, hasShadow
        context.drawText(this.textRenderer, Text.translatable(MOD_ID + ".text.config_title").formatted(Formatting.BLUE).asOrderedText(), 10, this.textRenderer.fontHeight, 0xFFFFFFFF, true);
        context.drawText(this.textRenderer, Text.translatable(MOD_ID + ".text.lat_lon", truncate(latlon[0],4), truncate(latlon[1], 4)), 100, this.textRenderer.fontHeight, 0xFFFFFFFF, true);
    }

    private void bottomPanel(int buttonWidth, int buttonHeight, int buttonPadding){
        assert this.client != null;

        doneButton = ButtonWidget.builder(ScreenTexts.DONE, (btn) -> {
            CONFIG = config;
            TerrariumConfig.save(CONFIG, Path.of("./saves/" + parent.getWorldCreator().getWorldDirectoryName() + "/terrarium.json"), false);
            this.client.setScreen(parent);
        }).dimensions((this.width / 2) - (buttonWidth + buttonPadding), this.height - (buttonPadding + buttonHeight) , buttonWidth, buttonHeight).build();
        cancelButton = ButtonWidget.builder(ScreenTexts.CANCEL, (btn) -> {
            CONFIG = new presetConfig();
            this.client.setScreen(parent);
        }).dimensions((this.width/2) + buttonPadding, this.height - (buttonPadding + buttonHeight), buttonWidth, buttonHeight).build();



        this.addDrawableChild(doneButton);
        this.addDrawableChild(cancelButton);
    }
}
