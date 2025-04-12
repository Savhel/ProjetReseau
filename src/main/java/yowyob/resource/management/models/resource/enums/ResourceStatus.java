package yowyob.resource.management.models.resource.enums;

public enum ResourceStatus {
    FREE((short) 0),

    AFFECTED((short) 1),

    IN_USE((short) 2);

    private final short value;

    ResourceStatus(short value) { this.value = value; }

    public short value() { return this.value; }

    public static ResourceStatus fromValue(short code) throws IllegalArgumentException {
        for (ResourceStatus status : ResourceStatus.values()) {
            if (status.value() == code) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid state value: " + code);
    }
}
