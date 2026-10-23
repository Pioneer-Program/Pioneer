package cute.ame.pioneer.Computer.Logic.Adapater;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import cute.ame.pioneer.Computer.Logic.Axiom;
import cute.ame.pioneer.Computer.Logic.AxiomFactory;
import cute.ame.pioneer.Computer.Logic.UserFunction;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.Map;


public class AxiomAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!Axiom.class.isAssignableFrom(type.getRawType()) || type.getRawType() == UserFunction.class)
            return null;
        return new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                Axiom axiom = (Axiom) value;
                TypeAdapter<T> delegate = gson.getDelegateAdapter(AxiomAdapterFactory.this, type);
                JsonObject json = delegate.toJsonTree(value).getAsJsonObject();
                out.beginObject();
                out.name("type").value(axiom.getType().toString());
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    if (entry.getKey().equals("type"))
                        continue;
                    out.name(entry.getKey());
                    gson.getAdapter(JsonElement.class).write(out, entry.getValue());
                }
                out.endObject();
            }

            @Override
            public T read(JsonReader in) {
                JsonElement element = Streams.parse(in);
                JsonObject jsonObject = element.getAsJsonObject();
                ResourceLocation axiomType = ResourceLocation.parse(jsonObject.get("type").getAsString());
                jsonObject.remove("type");
                Class<? extends Axiom> axiomClass = AxiomFactory.getAxiomClass(axiomType);
                @SuppressWarnings("unchecked")
                TypeAdapter<T> delegate = (TypeAdapter<T>) gson.getDelegateAdapter(AxiomAdapterFactory.this, TypeToken.get(axiomClass));
                Axiom axiom = (Axiom) delegate.fromJsonTree(jsonObject);
                axiom.setType(axiomType);
                return (T) axiom;
            }
        };
    }
}
