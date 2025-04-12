package yowyob.resource.management.repositories.resource;

import java.util.UUID;

import yowyob.resource.management.models.resource.Resource;
import org.springframework.data.cassandra.repository.CassandraRepository;


public interface ResourceRepository extends CassandraRepository<Resource, UUID> {
}
