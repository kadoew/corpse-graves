package com.kador.corpsegraves.mixin;

import net.minecraft.world.entity.Interaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Interaction.class)
public interface InteractionAccessor {
	@Invoker("setWidth")
	void corpsegraves$setWidth(float width);

	@Invoker("setHeight")
	void corpsegraves$setHeight(float height);

	@Invoker("setResponse")
	void corpsegraves$setResponse(boolean response);
}
