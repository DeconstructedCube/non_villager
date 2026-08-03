package com.bsfdsagfadg.non_villager;

import com.google.common.collect.ImmutableSet;

import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Registers the Naturalist villager profession and its trades.
 *
 * <p>Profession itself goes through the vanilla registry (no Fabric API
 * wrapper exists for it in 1.21.11); trades go through Fabric API's
 * {@link TradeOfferHelper}.
 */
public final class ModProfessions {
	public static final ResourceKey<VillagerProfession> NATURALIST = ResourceKey.create(
			Registries.VILLAGER_PROFESSION,
			Identifier.fromNamespaceAndPath(NonVillagerMod.MOD_ID, "naturalist"));

	public static final ResourceKey<PoiType> NATURALIST_POI_KEY = ResourceKey.create(
			Registries.POINT_OF_INTEREST_TYPE,
			Identifier.fromNamespaceAndPath(NonVillagerMod.MOD_ID, "naturalist_poi"));

	public static PoiType NATURALIST_POI;

	private ModProfessions() {
	}

	public static void register() {
		registerProfession();
		registerTrades();
	}

	private static void registerProfession() {
		// Use End Rod as the job block (for its "implications" matching the mod's theme)
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

		// Level 3 (Journeyman) — buys fertile nectar (NeedsofNature potion),
		// sells the mixed liquid bottle.
		TradeOfferHelper.registerVillagerOffers(NATURALIST, 3, trades -> {
			trades.add((level, trader, random) -> {
				ItemCost nectar = new ItemCost(Items.POTION, 1).withComponents(builder -> builder.expect(
						DataComponents.POTION_CONTENTS,
						new PotionContents(needsofNaturePotion("fertile_nectar"))));
				return new MerchantOffer(nectar, new ItemStack(Items.EMERALD, 5), 12, 6, 0.05F);
			});
			trades.add((level, trader, random) -> new MerchantOffer(
					new ItemCost(Items.EMERALD, 8), needsofNatureStack("mixed_liquid_bottle"), 4, 10, 0.2F));
		});

		// Level 4 (Expert) — flower mix is bought and sold.
		TradeOfferHelper.registerVillagerOffers(NATURALIST, 4, trades -> {
			trades.add((level, trader, random) -> new MerchantOffer(
					new ItemCost(needsofNatureItem("flower_mix"), 2), new ItemStack(Items.EMERALD, 3), 12, 8, 0.05F));
			trades.add((level, trader, random) -> new MerchantOffer(
					new ItemCost(Items.EMERALD, 5), needsofNatureStack("flower_mix"), 12, 8, 0.05F));
		});

		// Level 5 (Master) — the entity liquid bottle.
		TradeOfferHelper.registerVillagerOffers(NATURALIST, 5, trades -> {
			trades.add((level, trader, random) -> new MerchantOffer(
					new ItemCost(Items.EMERALD, 12), needsofNatureStack("entity_liquid_bottle"), 4, 12, 0.2F));
		});
	}

	private static Item needsofNatureItem(String path) {
		Item item = BuiltInRegistries.ITEM.getValue(
				Identifier.fromNamespaceAndPath("needsofnature", path));
		if (item == Items.AIR) {
			NonVillagerMod.LOGGER.warn("NeedsofNature item '{}' not found; trade will be broken.", path);
		}
		return item;
	}

	private static ItemStack needsofNatureStack(String path) {
		return new ItemStack(needsofNatureItem(path));
	}

	private static Holder<Potion> needsofNaturePotion(String path) {
		ResourceKey<Potion> key = ResourceKey.create(Registries.POTION,
				Identifier.fromNamespaceAndPath("needsofnature", path));
		return BuiltInRegistries.POTION.getOrThrow(key);
	}
}
