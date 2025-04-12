package yowyob.resource.management.events.enums;

public enum EventClass {
    Resource((short) 0),

    Service((short) 1);

    private final short value;

    EventClass(short value) { this.value = value; }

    public short value() { return this.value; }
}
