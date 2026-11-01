package com.kador.corpsegraves.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface TextDisplayAccessor {
	@Invoker("setText")
	void corpsegraves$setText(Component text);

	@Invoker("setLineWidth")
	void corpsegraves$setLineWidth(int width);

	@Invoker("setBackgroundColor")
	void corpsegraves$setBackgroundColor(int color);

	@Invoker("setFlags")
	void corpsegraves$setFlags(byte flags);
}
