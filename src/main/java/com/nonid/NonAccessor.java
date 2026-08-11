package com.nonid;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Holder;
import net.minecraft.world.item.alchemy.Potion;

public class NonAccessor {
    public static Map<?, ?> getLiquidGainMap() {
        return NonLiquidSystem.resolveEffectiveLiquidGainMap();
    }

    public static ItemStack createLiquidBottleStack(Identifier entityTypeId) {
        return NonItemSystem.createLiquidBottleStack(entityTypeId);
    }

    public static ItemStack createPotionVariantStack(Item item, Holder<Potion> potion) {
        return NonItemSystem.createPotionVariantStack(item, potion);
    }
}