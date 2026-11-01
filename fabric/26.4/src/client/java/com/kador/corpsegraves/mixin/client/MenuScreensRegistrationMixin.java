package com.kador.corpsegraves.mixin.client;

import com.kador.corpsegraves.client.GraveScreen;
import com.kador.corpsegraves.grave.GraveMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.4 moved menu-screen constructors into a registry created by
 * MenuScreens.bootstrap. Register our screen right after vanilla's entries.
 */
@Mixin(MenuScreens.class)
public abstract class MenuScreensRegistrationMixin {
	@Inject(method = "bootstrap", at = @At("RETURN"))
	private static void corpsegraves$register(Registry<MenuScreens.ScreenConstructor<?, ?>> registry,
											CallbackInfoReturnable<MenuScreens.ScreenConstructor<?, ?>> cir) {
		MenuScreens.register(registry, GraveMenu.TYPE, GraveScreen::new);
	}
}
