package com.bsfdsagfadg.non_villager;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NonVillagerMod implements ModInitializer {
	public static final String MOD_ID = "non_villager";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModProfessions.register();
		LOGGER.info("NoN Villager Professions initialized.");
	}
}
