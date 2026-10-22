package cute.ame.pioneer.Computer.Logic;

public enum ComparisonType {

    EQUALS,
    NOT_EQUALS,
    LESS_THAN,
    GREATER_THAN,
    LESS_OR_EQUAL,
    GREATER_OR_EQUAL;

    public static final String[] COMPARISON_OPERATORS = {
            "==",
            "!=",
            "<",
            ">",
            "<=",
            ">="
    };

    public String toComparisonOperator() {
        return COMPARISON_OPERATORS[ordinal()];
    }

    public static ComparisonType fromComparisonOperator(String operator) {
        for (int i = 0; i < COMPARISON_OPERATORS.length; i++) {
            if (COMPARISON_OPERATORS[i].equals(operator))
                return values()[i];
        }
        return null;
    }


}
