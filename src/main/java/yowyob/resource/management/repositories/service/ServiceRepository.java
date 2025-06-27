package yowyob.resource.management.repositories.service;

import java.util.UUID;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import yowyob.resource.management.models.service.Services;

public interface ServiceRepository extends ReactiveCassandraRepository<Services, UUID> {
}
