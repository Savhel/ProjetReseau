package yowyob.resource.management.models.resource;

import com.datastax.oss.driver.api.core.type.DataType;
import lombok.*;
import jakarta.annotation.PostConstruct;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;

import yowyob.resource.management.models.product.Product;
import yowyob.resource.management.models.resource.enums.ResourceStatus;

import java.util.UUID;


@Table
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Resource extends Product {

    @PrimaryKey
    @Setter
    private UUID id;

    @Setter
    @Column("state")
    private short state;

    @Column("status")
    @CassandraType(type = CassandraType.Name.TEXT)
    private ResourceStatus status;

    @PostConstruct
    private void initStatus() {
        this.status = ResourceStatus.fromValue(this.state);
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
        this.state = this.status.value();
    }
}