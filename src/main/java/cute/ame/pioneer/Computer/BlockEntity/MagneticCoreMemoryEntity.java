    package cute.ame.pioneer.Computer.BlockEntity;

    import com.google.gson.Gson;
    import com.google.gson.GsonBuilder;
    import com.google.gson.reflect.TypeToken;
    import cute.ame.pioneer.Computer.Logic.Adapater.AxiomAdapterFactory;
    import cute.ame.pioneer.Computer.Logic.Adapater.UserFunctionAdapter;
    import cute.ame.pioneer.Computer.Logic.BuiltIn.Action.IncrementVariable;
    import cute.ame.pioneer.Computer.Logic.BuiltIn.Function.ActionOverTime;
    import cute.ame.pioneer.Computer.Logic.UserFunction;
    import cute.ame.pioneer.Computer.Logic.Variable;
    import cute.ame.pioneer.Pioneer;
    import cute.ame.pioneer.Registrie.ModBlockEntities;
    import net.minecraft.core.BlockPos;
    import net.minecraft.core.HolderLookup;
    import net.minecraft.nbt.CompoundTag;
    import net.minecraft.world.level.Level;
    import net.minecraft.world.level.block.entity.BlockEntity;
    import net.minecraft.world.level.block.state.BlockState;
    import net.minecraft.world.level.storage.LevelResource;
    import org.jetbrains.annotations.NotNull;

    import java.io.*;
    import java.nio.charset.StandardCharsets;
    import java.nio.file.Files;
    import java.nio.file.Path;
    import java.util.*;
    import java.util.stream.Stream;

    public class MagneticCoreMemoryEntity extends BlockEntity {

        private HashMap<String, Variable<?>> variables = new HashMap<>();
        private List<UserFunction> userFunctions = new ArrayList<>();
        private UUID uuid;
        private Path dataFolder;
        private boolean loaded;

        public MagneticCoreMemoryEntity(BlockPos pos, BlockState blockState) {
            super(ModBlockEntities.MAGNETIC_CORE_MEMORY.get(), pos, blockState);
            if (level == null || level.isClientSide)
                return;
            loaded = false;
        }

        private UUID computeUUID() {
            String key = this.level.dimension().location() + ":" + this.worldPosition.getX() + "," + this.worldPosition.getY() + "," + this.worldPosition.getZ();
            return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        }

        private void loadRequireData() {
            uuid = computeUUID();
            Path worldFolder = level.getServer().getWorldPath(LevelResource.ROOT);
            dataFolder = worldFolder.resolve(Pioneer.MODID).resolve("magnetic_core_memory").resolve(uuid.toString());
            dataFolder.toFile().mkdirs();
        }

        private void loadMemory() {
            if (level == null || level.isClientSide)
                return;
            loadRequireData();
            File functionsFile = dataFolder.resolve("functions.json").toFile();
            File variablesFile =  dataFolder.resolve("variables.dat").toFile();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(UserFunction.class, new UserFunctionAdapter())
                    .registerTypeAdapterFactory(new AxiomAdapterFactory())
                    .create();
            if (functionsFile.exists()) {
                try (Reader reader = Files.newBufferedReader(functionsFile.toPath())) {
                    userFunctions = gson.fromJson(reader, new TypeToken<List<UserFunction>>() {}.getType());
                } catch (IOException error) {
                    Pioneer.LOGGER.warn("Could not read " + functionsFile.getPath(), error);
                }
            } else {
                ActionOverTime actionOverTime = new ActionOverTime(new IncrementVariable(), null, 40);
                UserFunction test = new UserFunction("Count up mission timer", null, actionOverTime);
                userFunctions.add(test);
            }
            if (variablesFile.exists()) {
                try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(variablesFile.getPath()))) {
                    variables = (HashMap<String, Variable<?>>) input.readObject();
                } catch (IOException | ClassNotFoundException error) {
                    Pioneer.LOGGER.warn("Could not read " + variablesFile.getPath(), error);
                }
            }
            loaded = true;
        }

        @Override
        public void onLoad() {
            super.onLoad();
            Pioneer.LOGGER.info("onLoad: level={}, client={}",
                    level, level != null && level.isClientSide);
            if (level == null || level.isClientSide)
                return;
            loadMemory();
        }

        @Override
        protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
            super.loadAdditional(tag, registries);
            if (level == null || level.isClientSide)
                return;
            loadMemory();
        }

        @Override
        protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
            super.saveAdditional(tag, registries);
            if (level == null || level.isClientSide || !loaded)
                return;
            loadRequireData();
            File functionsFile = dataFolder.resolve("functions.json").toFile();
            File variablesFile = dataFolder.resolve("variables.dat").toFile();
            try {
                functionsFile.createNewFile();
            } catch (IOException error) {
                Pioneer.LOGGER.error("Could not create " + functionsFile.getPath(), error);
                return;
            }
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(UserFunction.class, new UserFunctionAdapter())
                    .registerTypeAdapterFactory(new AxiomAdapterFactory())
                    .setPrettyPrinting()
                    .create();
            try {
                Files.writeString(Path.of(functionsFile.getPath()), gson.toJson(userFunctions));
                ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(variablesFile.toPath()));
                stream.writeObject(variables);
                stream.close();
            } catch (IOException error) {
                Pioneer.LOGGER.error("Could not write " + functionsFile.getPath(), error);
            }
        }

        public void deleteData() {
            if (level == null || level.isClientSide)
                return;
            loadRequireData();
            try (Stream<Path> paths = Files.walk(dataFolder)) {
                paths.filter(path -> !path.equals(dataFolder))
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException error) {
                            throw new UncheckedIOException(error);
                        }
                    });
            } catch (IOException error) {
                Pioneer.LOGGER.error("Could not delete " + dataFolder, error);
            }
            dataFolder.toFile().delete();
        }

        public static void tick(Level level, BlockPos pos, BlockState state, MagneticCoreMemoryEntity memory) {
            if (level.isClientSide || !memory.loaded) return;
            memory.userFunctions.forEach(userFunction -> {
                userFunction.execute(memory.variables);
            });
        }

    }
