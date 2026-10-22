package cute.ame.pioneer.Computer.Logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class Variable<T> {

    private Codec<T> codec;
    private T value;
    private boolean isAssigned;

    Variable(Codec<T> codec) {
        value = null;
        isAssigned = false;
    }

    Variable(T value) {
        this.value = value;
        this.isAssigned = true;
    }

    Variable(Variable<T> variable) {
        this(variable.value);
    }

    public void assign(T value) {
        this.value = value;
        this.isAssigned = true;
    }

    public void assign(Variable<T> other) {
        assign(other.value);
    }

    public void clear() {
        this.value = null;
        this.isAssigned = false;
    }

    public T getValue() {
        if (!isAssigned || value == null)
            throw new IllegalStateException("Variable is not assigned");
        return value;
    }

    public boolean compareTo(T other, ComparisonType type) {
        if (!this.value.getClass().isAssignableFrom(Comparable.class))
            return false;
        Comparable<T> comparable = (Comparable<T>) this.value;
        int result = comparable.compareTo(other);
        return switch (type) {
            case EQUALS -> result == 0;
            case NOT_EQUALS -> result != 0;
            case GREATER_THAN -> result > 0;
            case LESS_THAN -> result < 0;
            case GREATER_OR_EQUAL ->  result >= 0;
            case LESS_OR_EQUAL ->  result <= 0;
        };
    }

    public boolean compareTo(Variable<T> other, ComparisonType type) {
        return compareTo(other.value, type);
    }



}
