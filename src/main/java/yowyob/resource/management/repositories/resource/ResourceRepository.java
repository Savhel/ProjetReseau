package yowyob.resource.management.repositories.resource;

import java.util.UUID;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import yowyob.resource.management.models.resource.Resource;


public interface ResourceRepository extends ReactiveCassandraRepository<Resource, UUID> {
}
