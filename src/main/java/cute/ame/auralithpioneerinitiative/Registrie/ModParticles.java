package cute.ame.auralithpioneerinitiative.Registrie;

import cute.ame.auralithpioneerinitiative.Auralithpioneerinitiative;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles
{
  public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Auralithpioneerinitiative.MODID);

  public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ARID_GROUND_SCATTER = PARTICLE_TYPES.register("arid_ground_scatter", () -> new SimpleParticleType(false));
}