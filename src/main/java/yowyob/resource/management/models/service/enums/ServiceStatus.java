package yowyob.resource.management.models.service.enums;

public enum ServiceStatus {
    PLANNED((short) 0),

    PUBLISHED((short) 1),

    ONGOING((short) 2),

    FINISHED((short) 3),

    CANCELLED((short) 4);

    private final short value;

    ServiceStatus(short value) { this.value = value; }

    public short value() { return this.value; }

    public static ServiceStatus fromValue(short code) throws IllegalArgumentException {
        for (ServiceStatus status : ServiceStatus.values()) {
            if (status.value() == code) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid state value: " + code);
    }

}
