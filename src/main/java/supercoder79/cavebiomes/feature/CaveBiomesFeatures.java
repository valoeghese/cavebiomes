package supercoder79.cavebiomes.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import supercoder79.cavebiomes.config.ConfigData;
import supercoder79.cavebiomes.mixin.GenerationSettingsAccessor;

public final class CaveBiomesFeatures {
	private CaveBiomesFeatures() {
	}

	public static void addEnabledFeatures(ConfigData config) {
		// register features
		registerFeature("cavern_chest", CAVERN_CHEST);
		registerFeature("spelunkers_chest", SPELUNKERS_CHEST);

		registerFeature("nether_chest", NETHER_CHEST);

		registerFeature("cave_spawner", CAVE_SPAWNER);
		registerFeature("rare_cave_spawner", RARE_CAVE_SPAWNER);

		// add generation

		// ================
		//    OVERWORLD
		// ================

		if (config.generateUndergroundLootChests) {
			addFeatureTo(
					GenerationStep.Feature.VEGETAL_DECORATION,
					CAVERN_CHEST.configure(FeatureConfig.DEFAULT).decorate(Decorator.CHANCE.configure(new ChanceDecoratorConfig(config.undergroundFeatures.cavernChestRarity))),
					OVERWORLD);

			addFeatureTo(
					GenerationStep.Feature.VEGETAL_DECORATION,
					SPELUNKERS_CHEST.configure(FeatureConfig.DEFAULT).decorate(Decorator.CHANCE.configure(new ChanceDecoratorConfig(config.undergroundFeatures.spelunkersChestRarity))),
					OVERWORLD);
		}

		if (config.generateUndergroundSpawners) {
			addFeatureTo(
					GenerationStep.Feature.VEGETAL_DECORATION,
					CAVE_SPAWNER.configure(FeatureConfig.DEFAULT).decorate(Decorator.CHANCE.configure(new ChanceDecoratorConfig(config.undergroundFeatures.normalSpawnerRarity))),
					OVERWORLD);

			addFeatureTo(
					GenerationStep.Feature.VEGETAL_DECORATION,
					RARE_CAVE_SPAWNER.configure(FeatureConfig.DEFAULT).decorate(Decorator.CHANCE.configure(new ChanceDecoratorConfig(config.undergroundFeatures.rareSpawnerRarity))),
					OVERWORLD);
		}

		// ================
		//      NETHER
		// ================

		if (config.generateNetherLootChests) {
			addFeatureTo(
					GenerationStep.Feature.UNDERGROUND_DECORATION,
					NETHER_CHEST.configure(FeatureConfig.DEFAULT).decorate(Decorator.CHANCE.configure(new ChanceDecoratorConfig(config.undergroundFeatures.netherChestRarity))),
					NETHER);
		}
	}

	private static <C extends FeatureConfig, F extends Feature<C>, T extends ConfiguredFeature<C, F>> void addFeatureTo(final GenerationStep.Feature step, final T feature, final Predicate<Biome> predicate) {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			server.getRegistryManager().get(Registry.BIOME_KEY).forEach(biome -> {
				if (predicate.test(biome)) {
					addFeature(biome, step, feature);
				}
			});
		});
	}

	private static void registerFeature(String id, Feature<?> feature) {
		Registry.register(Registry.FEATURE, new Identifier("cavebiomes", id), feature);
	}

	private static void addFeature(Biome biome, GenerationStep.Feature step, ConfiguredFeature<?, ?> feature) {
		List<List<Supplier<ConfiguredFeature<?, ?>>>> featureSteps = biome.getGenerationSettings().getFeatures();

		// Mutable List
		List<List<Supplier<ConfiguredFeature<?, ?>>>> newFeatures = new ArrayList<>();

		for (GenerationStep.Feature featureStep : GenerationStep.Feature.values()) {
			int index = featureStep.ordinal();

			// create a mutable list
			List<Supplier<ConfiguredFeature<?, ?>>> features = index < featureSteps.size() ? new ArrayList<>(featureSteps.get(index)) : new ArrayList<>();

			// Add our feature
			features.add(() -> feature);

			// Add entry
			newFeatures.add(features);
		}

		// Replace list
		((GenerationSettingsAccessor) biome.getGenerationSettings()).setFeatures(newFeatures);
	}

	public static final Predicate<Biome> OVERWORLD = biome -> {
		Biome.Category category = biome.getCategory();
		return !(category == Biome.Category.NETHER || category == Biome.Category.THEEND);
	};

	public static final Predicate<Biome> NETHER = biome -> {
		Biome.Category category = biome.getCategory();
		return category == Biome.Category.NETHER;
	};

	public static final TreasureChestFeature CAVERN_CHEST = new TreasureChestFeature(0, TreasureChestFeature.Type.CAVERN);
	public static final TreasureChestFeature SPELUNKERS_CHEST = new TreasureChestFeature(1, TreasureChestFeature.Type.SPELUNKERS);

	public static final TreasureChestFeature NETHER_CHEST = new TreasureChestFeature(2, TreasureChestFeature.Type.NETHER);

	public static final MobSpawnerFeature CAVE_SPAWNER = new MobSpawnerFeature(3, 50, EntityType.ZOMBIE, EntityType.SPIDER);
	public static final MobSpawnerFeature RARE_CAVE_SPAWNER = new MobSpawnerFeature(4, 35, EntityType.SKELETON, EntityType.SKELETON, EntityType.SKELETON, EntityType.CAVE_SPIDER, EntityType.CREEPER);
}
