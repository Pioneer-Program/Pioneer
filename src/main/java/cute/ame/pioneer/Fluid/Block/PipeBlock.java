package cute.ame.pioneer.Fluid.Block;

import cute.ame.pioneer.Fluid.Data.FluidConstants;

public class PipeBlock extends FluidVesselBlock
{
    public PipeBlock()
    {
        super(metal(2.0f));
    }

    @Override
    public float getVolumeLitres()
    {
        return FluidConstants.PIPE_VOLUME_L;
    }

    @Override
    public boolean merges()
    {
        return false;
    }

    @Override
    public float getConductance()
    {
        return 0.5f;
    }

    @Override
    public float getNominalBurstPressure()
    {
        return 10.0f;
    }
}
