package cute.ame.pioneer.Data;

import cute.ame.pioneer.Pioneer;
import cute.ame.pioneer.Registrie.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Pioneer.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        PipeBlockModel.register(
            this,
            ModBlocks.VACUUM_PIPE.get(),
            modLoc("block/vacuum_pipe/vacuum_pipe_connected"),
            modLoc("block/vacuum_pipe/vacuum_pipe")
        );

        itemModels().withExistingParent("vacuum_pipe", modLoc("block/vacuum_pipe/item"));
    }
}
