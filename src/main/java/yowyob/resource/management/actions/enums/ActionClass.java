package yowyob.resource.management.actions.enums;

public enum ActionClass {
    Resource((short) 0),

    Service((short) 1);

    private final short value;

    ActionClass(short value) { this.value = value; }

    public short value() { return this.value; }
}

