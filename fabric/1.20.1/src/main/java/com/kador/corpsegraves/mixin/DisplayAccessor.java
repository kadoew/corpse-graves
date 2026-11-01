package com.kador.corpsegraves.mixin;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {
	@Invoker("setBillboardConstraints")
	void corpsegraves$setBillboard(Display.BillboardConstraints constraints);

	@Invoker("setViewRange")
	void corpsegraves$setViewRange(float range);

	@Invoker("setTransformation")
	void corpsegraves$setTransformation(Transformation transformation);

	@Invoker("setBrightnessOverride")
	void corpsegraves$setBrightness(Brightness brightness);
}
