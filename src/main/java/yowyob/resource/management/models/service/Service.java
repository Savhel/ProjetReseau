package yowyob.resource.management.models.service;

import java.util.UUID;

import jakarta.annotation.PostConstruct;
import lombok.*;

import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;

import yowyob.resource.management.models.product.Product;
import yowyob.resource.management.models.service.enums.ServiceStatus;

@Table
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Service extends Product {

    @Setter
    @PrimaryKey
    private UUID id = UUID.randomUUID();

    @Setter
    @Column("state")
    private short state;

    private ServiceStatus status;

    @PostConstruct
    private void initStatus() {
        this.status = ServiceStatus.fromValue(this.state);
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
        this.state = this.status.value();
    }
}
