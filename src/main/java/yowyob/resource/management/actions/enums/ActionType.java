package yowyob.resource.management.actions.enums;

public enum ActionType {
    CREATE((short) 0),

    READ((short) 1),

    UPDATE((short) 2),

    DELETE((short) 3),

    CUSTOM((short) 4);

    private final short value;

    ActionType(short value) { this.value = value; }

    public short value() { return this.value; }
}
