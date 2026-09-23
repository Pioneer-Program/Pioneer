package cute.ame.pioneer.Fluid.Block;

import cute.ame.celsius.Fluid.Data.FluidConstants;

public class TankBlock extends PioneerVesselBlock
{
    public TankBlock()
    {
        super(metal(3.0f));
    }

    @Override
    public float getVolumeLitres()
    {
        return FluidConstants.TANK_VOLUME_L;
    }

    @Override
    public boolean merges()
    {
        return true;
    }

    @Override
    public float getConductance()
    {
        return 1.0f;
    }

    @Override
    public float getNominalBurstPressure()
    {
        return 25.0f;
    }
}
