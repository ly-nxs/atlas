package xyz.lynxs.terrarium.world.gen.noise;

import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.math.random.Random;


public class Noises {

    private static PerlinNoiseSampler perlinNoiseSampler;
    private static SimplexNoiseSampler simplexNoiseSampler;


    public static void init(long seed){
        perlinNoiseSampler = new PerlinNoiseSampler(Random.create(seed));
        simplexNoiseSampler = new SimplexNoiseSampler(Random.create(seed));
    }



    private static double samplePerlin(int x, int z, double scale){
        return perlinNoiseSampler.sample(x * scale, 0, z * scale);
    }

    private static double sampleSimplex(int x, int z, int scale){
        return simplexNoiseSampler.sample(x * scale, 0, z * scale);
    }

    public static double baseTerrainNoise(int x, int z){
        return Math.abs(samplePerlin(x, z, 0.01));
    }
}
