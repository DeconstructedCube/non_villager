package com.bsfdsagfadg.non_villager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;

import com.bsfdsagfadg.non_villager.mixin.PoiTypesAccessor;

/**
 * Registers the Naturalist villager profession and its trades.
 *
 * <p>Profession itself goes through the vanilla registry (no Fabric API
 * wrapper exists for it in 1.21.11); trades go through Fabric API's
 * {@link TradeOfferHelper}.
 *
 * <p>NeedsofNature "liquid bottles" are not registered items — they are vanilla
 * potion stacks built by the mod's own {@code NonItemSystem.createLiquidBottleStack}.
 * We invoke that method via reflection so the traded items are byte-for-byte
 * identical to the mod's (same components, tint, stacking, tooltips), which keeps
 * textures working without Optifine and makes the bottles stackable.
 */
public final class ModProfessions {
	public static final ResourceKey<VillagerProfession> NATURALIST = ResourceKey.create(
			Registries.VILLAGER_PROFESSION,
			Identifier.fromNamespaceAndPath(NonVillagerMod.MOD_ID, "naturalist"));

	public static final ResourceKey<PoiType> NATURALIST_POI_KEY = ResourceKey.create(
			Registries.POINT_OF_INTEREST_TYPE,
			Identifier.fromNamespaceAndPath(NonVillagerMod.MOD_ID, "naturalist_poi"));

	public static PoiType NATURALIST_POI;

	/** Fallback animals if the mod's liquid gain map is unavailable. */
	private static final List<Identifier> FALLBACK_ENTITIES = List.of(
			Identifier.withDefaultNamespace("cow"),
			Identifier.withDefaultNamespace("pig"),
			Identifier.withDefaultNamespace("sheep"),
			Identifier.withDefaultNamespace("horse"),
			Identifier.withDefaultNamespace("donkey"),
			Identifier.withDefaultNamespace("mule"),
			Identifier.withDefaultNamespace("fox"),
			Identifier.withDefaultNamespace("wolf"),
			Identifier.withDefaultNamespace("cat"),
			Identifier.withDefaultNamespace("dolphin"),
			Identifier.withDefaultNamespace("polar_bear"),
			Identifier.withDefaultNamespace("rabbit"));

	/** Lazily resolved entity types that can produce liquid, from the mod's own data. */
	private static List<Identifier> liquidEntities;

	private ModProfessions() {
	}

	public static void register() {
		registerProfession();
		registerTrades();
	}

	private static void registerProfession() {
		// The leatherworker owns every cauldron block state (PoiTypes.CAULDRONS).
		// PoiTypes rejects double registration, so strip the powder snow cauldron from
		// the TYPE_BY_STATE map — the single runtime lookup, since PoiManager creates
		// POI records solely via PoiTypes.forState — then claim it for ourselves.
		PoiTypesAccessor.getTypeByState().keySet()
				.removeIf(state -> state.getBlock() == Blocks.POWDER_SNOW_CAULDRON);

		NATURALIST_POI = net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper.register(
				Identifier.fromNamespaceAndPath(NonVillagerMod.MOD_ID, "naturalist_poi"),
				1, 1, Blocks.POWDER_SNOW_CAULDRON);

		Registry.register(BuiltInRegistries.VILLAGER_PROFESSION, NATURALIST, new VillagerProfession(
				Component.translatable("entity.minecraft.villager.naturalist"),
				poi -> poi.is(NATURALIST_POI_KEY),
				poi -> poi.is(NATURALIST_POI_KEY),
				ImmutableSet.of(),
				ImmutableSet.of(),
				SoundEvents.VILLAGER_WORK_FARMER));
	}

	private static void registerTrades() {
		// Level 1 (Novice)
		TradeOfferHelper.registerVillagerOffers(NATURALIST, 1, trades -> {
			trades.add((level, trader, random) -> new MerchantOffer(
					new ItemCost(Items.HONEY_BOTTLE, 3), new ItemStack(Items.EMERALD), 12, 2, 0.05F));
			trades.add((level, trader, random) -> new MerchantOffer(
					new ItemCost(Items.EMERALD), new ItemStack(Items.CRIMSON_FUNGUS), 12, 2, 0.05F));
		});

		// Level 2 (Apprentice)
		TradeOfferHelper.registerVillagerOffers(NATURALIST, 2, trades -> {
			trades.add((level, trader, random) -> new MerchantOffer(
					new ItemCost(Items.HONEYCOMB, 2), new ItemStack(Items.EMERALD), 12, 4, 0.05F));
			trades.add((level, trader, random) -> new MerchantOffer(
					new ItemCost(Items.EMERALD), new ItemStack(Items.WARPED_FUNGUS), 12, 4, 0.05F));
		});

		// Level 3 (Journeyman) — buys fertile nectar (exact potion match), sells the mixed liquid bottle.
		TradeOfferHelper.registerVillagerOffers(NATURALIST, 3, trades -> {
			trades.add((level, trader, random) -> {
				Holder<Potion> nectar = needsofNaturePotion("fertile_nectar");
				if (nectar == null) return null;
				ItemStack nectarBottle = buildFertileNectarBottle(nectar);
				if (nectarBottle == null) return null;
				ItemCost cost = new ItemCost(nectarBottle.getItemHolder(), 1,
						DataComponentExactPredicate.allOf(nectarBottle.getComponents()));
				return new MerchantOffer(cost, new ItemStack(Items.EMERALD, 5), 12, 6, 0.05F);
			});
			trades.add((level, trader, random) -> {
				ItemStack bottle = buildLiquidBottle(null);
				if (bottle == null) return null;
				return new MerchantOffer(new ItemCost(Items.EMERALD, 8), bottle, 4, 10, 0.2F);
			});
		});

		// Level 4 (Expert) — flower mix is bought and sold. Three flowers craft one mix,
		// so it is a cheap commodity: 4 mixes for 1 emerald, or 2 mixes for 1 emerald.
		TradeOfferHelper.registerVillagerOffers(NATURALIST, 4, trades -> {
			trades.add((level, trader, random) -> {
				Item flowerMix = needsofNatureItem("flower_mix");
				if (flowerMix == Items.AIR) return null;
				return new MerchantOffer(new ItemCost(flowerMix, 4), new ItemStack(Items.EMERALD), 12, 8, 0.05F);
			});
			trades.add((level, trader, random) -> {
				Item flowerMix = needsofNatureItem("flower_mix");
				if (flowerMix == Items.AIR) return null;
				return new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(flowerMix, 2), 12, 8, 0.05F);
			});
			trades.add((level, trader, random) -> {
				Item collector = needsofNatureItem("horse_liquid_collector");
				if (collector == Items.AIR) return null;
				return new MerchantOffer(new ItemCost(Items.EMERALD, 12), new ItemStack(collector), 4, 10, 0.05F);
			});
		});

		// Level 5 (Master) — TWO random animal's liquid bottles.
		TradeOfferHelper.registerVillagerOffers(NATURALIST, 5, trades -> {
			trades.add((level, trader, random) -> {
				ItemStack bottle = buildLiquidBottle(getRandomEntityId(random));
				if (bottle == null) return null;
				return new MerchantOffer(new ItemCost(Items.EMERALD, 12), bottle, 4, 12, 0.2F);
			});
			trades.add((level, trader, random) -> {
				ItemStack bottle = buildLiquidBottle(getRandomEntityId(random));
				if (bottle == null) return null;
				return new MerchantOffer(new ItemCost(Items.EMERALD, 12), bottle, 4, 12, 0.2F);
			});
		});
	}

	private static Identifier getRandomEntityId(RandomSource random) {
		List<Identifier> candidates = liquidEntities();
		return candidates.get(random.nextInt(candidates.size()));
	}

	/**
	 * Resolves the entity types that can produce liquid by invoking the mod's
	 * {@code NonLiquidSystem.resolveEffectiveLiquidGainMap} via reflection. The result
	 * comes from the mod's own liquid gain data (liquid_gains.json) and is cached;
	 * falls back to {@link #FALLBACK_ENTITIES} when the mod is unavailable.
	 */
	private static List<Identifier> liquidEntities() {
		if (liquidEntities == null) {
			List<Identifier> resolved = new ArrayList<>();
			try {
				Class<?> modClass = Class.forName("com.nonid.NonLiquidSystem");
				Method gainMapMethod = null;
				for (Method method : modClass.getDeclaredMethods()) {
					if ("resolveEffectiveLiquidGainMap".equals(method.getName())
							&& method.getParameterCount() == 0
							&& method.getReturnType() == Map.class) {
						gainMapMethod = method;
						break;
					}
				}
				if (gainMapMethod != null) {
					gainMapMethod.setAccessible(true);
					Map<?, ?> map = (Map<?, ?>) gainMapMethod.invoke(null);
					for (Object key : map.keySet()) {
						Identifier parsed = Identifier.tryParse(String.valueOf(key));
						if (parsed != null) resolved.add(parsed);
					}
				} else {
					NonVillagerMod.LOGGER.warn("resolveEffectiveLiquidGainMap not found in com.nonid.NonLiquidSystem");
				}
			} catch (Exception e) {
				NonVillagerMod.LOGGER.error("Failed to resolve NeedsofNature liquid entities", e);
			}
			liquidEntities = resolved.isEmpty() ? FALLBACK_ENTITIES : List.copyOf(resolved);
		}
		return liquidEntities;
	}

	/**
	 * Invokes the mod's own {@code NonItemSystem.createLiquidBottleStack} via reflection,
	 * so the sold item is identical to what the mod itself produces (same components,
	 * tint, max stack size 16, food, tooltip, custom name). {@code null} produces the
	 * mixed bottle; an entity id produces the {@code <entity> 精液瓶} variant.
	 * Returns {@code null} when the mod is unavailable.
	 */
	private static ItemStack buildLiquidBottle(Identifier entityTypeId) {
		try {
			Class<?> modClass = Class.forName("com.nonid.NonItemSystem");
			Method createMethod = null;
			for (Method method : modClass.getDeclaredMethods()) {
				if ("createLiquidBottleStack".equals(method.getName())
						&& method.getParameterCount() == 1
						&& method.getParameterTypes()[0] == Identifier.class
						&& method.getReturnType() == ItemStack.class) {
					createMethod = method;
					break;
				}
			}
			if (createMethod != null) {
				createMethod.setAccessible(true);
				return (ItemStack) createMethod.invoke(null, entityTypeId);
			}
			NonVillagerMod.LOGGER.error("Could not find createLiquidBottleStack in com.nonid.NonItemSystem");
		} catch (Exception e) {
			NonVillagerMod.LOGGER.error("Failed to create NeedsofNature liquid bottle", e);
		}
		return null;
	}

	/**
	 * Builds the exact fertile nectar bottle the mod's own crafting recipe produces
	 * (potion contents + food + max stack 16 + honey drink consumable) by invoking
	 * {@code NonItemSystem.createPotionVariantStack}. Used both as the trade's cost
	 * predicate (exact component set) and as the client-side display stack, so the
	 * offer renders the mod's nectar model. Returns {@code null} when unavailable.
	 */
	private static ItemStack buildFertileNectarBottle(Holder<Potion> nectar) {
		try {
			Class<?> modClass = Class.forName("com.nonid.NonItemSystem");
			for (Method method : modClass.getDeclaredMethods()) {
				if ("createPotionVariantStack".equals(method.getName())
						&& method.getParameterCount() == 2
						&& method.getParameterTypes()[0] == Item.class
						&& method.getParameterTypes()[1] == Holder.class
						&& method.getReturnType() == ItemStack.class) {
					method.setAccessible(true);
					return (ItemStack) method.invoke(null, Items.POTION, nectar);
				}
			}
			NonVillagerMod.LOGGER.error("createPotionVariantStack not found in com.nonid.NonItemSystem");
		} catch (Exception e) {
			NonVillagerMod.LOGGER.error("Failed to build NeedsofNature fertile nectar bottle", e);
		}
		return null;
	}

	private static Item needsofNatureItem(String path) {
		Item item = BuiltInRegistries.ITEM.getValue(
				Identifier.fromNamespaceAndPath("needsofnature", path));
		if (item == Items.AIR) {
			NonVillagerMod.LOGGER.warn("NeedsofNature item '{}' not found; its trades will be skipped.", path);
		}
		return item;
	}

	private static Holder<Potion> needsofNaturePotion(String path) {
		ResourceKey<Potion> key = ResourceKey.create(Registries.POTION,
				Identifier.fromNamespaceAndPath("needsofnature", path));
		Holder<Potion> holder = BuiltInRegistries.POTION.get(key).orElse(null);
		if (holder == null) {
			NonVillagerMod.LOGGER.warn("NeedsofNature potion '{}' not found; its trades will be skipped.", path);
		}
		return holder;
	}
}
