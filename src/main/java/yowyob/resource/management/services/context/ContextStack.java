package yowyob.resource.management.services.context;

import org.springframework.stereotype.Component;
import yowyob.resource.management.commons.Command;

import java.util.Stack;

@Component
public class ContextStack extends Stack<Command> {
    @Override
    public synchronized Command push(Command item) {
        return super.push(item);
    }
}
