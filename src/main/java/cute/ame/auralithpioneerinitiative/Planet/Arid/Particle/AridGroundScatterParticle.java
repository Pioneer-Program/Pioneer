package cute.ame.auralithpioneerinitiative.Planet.Arid.Particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public final class AridGroundScatterParticle extends TextureSheetParticle
{
  private static final float GRAVITY  = 0.034f;
  private static final float FRICTION = 0.88f;

  AridGroundScatterParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz)
  {
    super(level, x, y, z);
    this.xd = vx;
    this.yd = vy;
    this.zd = vz;

    this.quadSize = 0.022f + random.nextFloat() * 0.042f;
    this.lifetime = 15 + random.nextInt(14);

    float bright = 0.68f + random.nextFloat() * 0.20f;
    this.rCol = bright;
    this.gCol = bright * 0.58f + random.nextFloat() * 0.04f;
    this.bCol = bright * 0.30f + random.nextFloat() * 0.04f;

    this.alpha = 0f;
    this.hasPhysics = false;
    this.gravity = GRAVITY;
  }

  @Override
  public void tick()
  {
    this.xo = this.x;
    this.yo = this.y;
    this.zo = this.z;
    if (this.age++ >= this.lifetime) { this.remove(); return; }

    float life = (float) age / lifetime;
    this.alpha = life < 0.07f ? life / 0.07f : Math.max(0f, 1f - (life - 0.07f) / 0.93f);
    if (life > 0.70f) this.quadSize *= 0.962f;

    this.yd -= GRAVITY;
    this.x += this.xd;
    this.y += this.yd;
    this.z += this.zd;
    this.xd *= FRICTION;
    this.yd *= 0.94f;
    this.zd *= FRICTION;

    if (this.yd < 0 && this.y <= this.yo - 0.05)
    {
      this.yd  = 0;
      this.xd *= 0.45f;
      this.zd *= 0.45f;
    }
  }

  @Override
  public ParticleRenderType getRenderType()
  {
    return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
  }

  public static class Provider implements ParticleProvider.Sprite<SimpleParticleType>
  {
    @Override
    public TextureSheetParticle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz)
    {
      return new AridGroundScatterParticle(level, x, y, z, vx, vy, vz);
    }
  }
}