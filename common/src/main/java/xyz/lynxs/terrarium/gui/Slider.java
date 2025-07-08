package xyz.lynxs.terrarium.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.Nullable;


public class Slider extends AbstractSliderButton {
    private final double min;
    private final double max;
    private final Component name;
    private final Format format;
    @Nullable
    private final Callback callback;

    public Slider(int x, int y, int width, int height, float initialValue, float min, float max, Component name, Format format, @Nullable Callback callback) {
        super(x, y, width, height, Component.empty(), 0.0);
        this.name = name;
        this.format = format;
        this.min = min;
        this.max = max;
        this.value = this.getSliderValue(initialValue);
        this.callback = callback;
        this.updateMessage();
    }


    public double getSliderValue(float value) {
        return (Mth.clamp(value, this.min, this.max) - this.min) / (this.max - this.min);
    }

    public double lerpValue(double value) {
        return Mth.lerp(value, this.min, this.max);
    }

    public double scaleValue(double value) {
        return this.format.scale(this.lerpValue(value));
    }

    @Override
    public void applyValue() {
        if(this.callback != null) {
            this.value = this.callback.apply(this, this.value);
        }
    }

    @Override
    protected void updateMessage() {
        this.setMessage(CommonComponents.optionNameValue(this.name, Component.literal(this.format.getMessage(this.scaleValue((float) this.value)))));
    }

    public static enum Format {
        INT {
            @Override
            public double scale(double input) {
                return (int) input;
            }

            @Override
            public String getMessage(double input) {
                return String.valueOf((int) input);
            }
        },
        FLOAT {
            @Override
            public double scale(double input) {
                return input;
            }

            @Override
            public String getMessage(double input) {
                return String.format("%.3f", input);
            }
        };

        public abstract double scale(double input);

        public abstract String getMessage(double input);
    }

    public interface Callback {
        double apply(Slider slider, double value);
    }
}