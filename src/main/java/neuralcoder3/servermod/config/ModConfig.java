package neuralcoder3.servermod.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class ModConfig {
    public static final Codec<ModConfig> CODEC = RecordCodecBuilder.create(instance -> 
        instance.group(
            Codec.INT.fieldOf("port").orElse(8080).forGetter(config -> config.port),
            Codec.BOOL.fieldOf("enabled").orElse(true).forGetter(config -> config.enabled)
        ).apply(instance, ModConfig::new)
    );

    public final int port;
    public final boolean enabled;

    public ModConfig(int port, boolean enabled) {
        this.port = port;
        this.enabled = enabled;
    }

    public static ModConfig createDefault() {
        return new ModConfig(8080, true);
    }
}
