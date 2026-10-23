package cute.ame.pioneer.Computer.Logic.Adapater;

import com.google.gson.*;
import cute.ame.pioneer.Computer.Logic.Action;
import cute.ame.pioneer.Computer.Logic.Condition;
import cute.ame.pioneer.Computer.Logic.Function;
import cute.ame.pioneer.Computer.Logic.UserFunction;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Type;

public class UserFunctionAdapter implements JsonSerializer<UserFunction>, JsonDeserializer<UserFunction> {

    @Override
    public JsonElement serialize(UserFunction userFunction, Type type, JsonSerializationContext context) {
        JsonObject json = new JsonObject();
        json.addProperty("name", userFunction.getName());
        String description = userFunction.getDescription();
        if (description != null && !description.isEmpty())
            json.addProperty("description", description);
        json.addProperty("type", userFunction.getType().toString());
        JsonObject obj = context.serialize(userFunction.getFunction()).getAsJsonObject();
        for  (String key : obj.keySet()) {
            if (key.equals("action") || key.equals("condition") || key.equals("type"))
                continue;
            json.add(key, obj.get(key));
        }
        json.add("action", context.serialize(userFunction.getAction()));
        json.add("condition", context.serialize(userFunction.getCondition()));
        return json;
    }

    @Override
    public UserFunction deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String name = jsonObject.get("name").getAsString();
        String description = "";
        if (jsonObject.has("description"))
            description = jsonObject.get("description").getAsString();
        String functionType = jsonObject.get("type").getAsString();
        JsonObject functionObject = new JsonObject();
        boolean start = false;
        for (String key : jsonObject.keySet()) {
            if (key.equals("type"))
                start = true;
            if (!start)
                continue;
            functionObject.add(key, jsonObject.get(key));
            if (key.equals("action"))
                break;
        }
        Function function = context.deserialize(functionObject, Function.class);
        Action action = context.deserialize(jsonObject.get("action"), Action.class);
        Condition condition = context.deserialize(jsonObject.get("condition"), Condition.class);
        function.setAction(action);
        function.setCondition(condition);
        function.setType(ResourceLocation.parse(functionType));
        return new UserFunction(name, description.isEmpty() ? null : description, function);
    }

}
