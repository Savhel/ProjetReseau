package yowyob.resource.management.services.context;

import org.springframework.stereotype.Component;
import yowyob.resource.management.commons.Command;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.EmptyStackException;

@Component
public class ContextStack {
    private final ConcurrentLinkedDeque<Command> stack = new ConcurrentLinkedDeque<>();

    public Command push(Command item) {
        stack.addFirst(item);
        return item;
    }

    public Command pop() {
        Command item = stack.pollFirst();
        if (item == null) {
            throw new EmptyStackException();
        }
        return item;
    }

    public Command peek() {
        Command item = stack.peekFirst();
        if (item == null) {
            throw new EmptyStackException();
        }
        return item;
    }

    public boolean empty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }
}
