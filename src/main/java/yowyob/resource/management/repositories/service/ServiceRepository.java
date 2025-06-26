package yowyob.resource.management.repositories.service;

import java.util.UUID;

import yowyob.resource.management.models.service.Services;
import org.springframework.data.cassandra.repository.CassandraRepository;

public interface ServiceRepository extends CassandraRepository<Services, UUID> {
}
