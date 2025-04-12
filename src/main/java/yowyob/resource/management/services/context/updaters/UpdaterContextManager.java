package yowyob.resource.management.services.context.updaters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yowyob.resource.management.events.Event;
import yowyob.resource.management.exceptions.invalid.InvalidEventClassException;
import yowyob.resource.management.services.resource.ResourceUpdater;
import yowyob.resource.management.services.service.ServiceUpdater;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

@Service
public class UpdaterContextManager {
    private final ServiceUpdater serviceUpdater;
    private final ResourceUpdater resourceUpdater;

    private static final Logger logger = LoggerFactory.getLogger(UpdaterContextManager.class);


    @Autowired
    public UpdaterContextManager(ServiceUpdater serviceUpdater, ResourceUpdater resourceUpdater) {
        this.serviceUpdater = serviceUpdater;
        this.resourceUpdater = resourceUpdater;
    }

    public void init() {
        pauseUpdaters();
        logger.info("Context has been successfully initialized");
    }

    public void clear() {
        resumeUpdaters();
        logger.info("Context has been successfully cleared");
    }

    private void pauseUpdaters() {
        this.serviceUpdater.pause();
        this.resourceUpdater.pause();
    }

    private void resumeUpdaters() {
        this.serviceUpdater.resume();
        this.resourceUpdater.resume();
    }
}
