package yowyob.resource.management.helpers;

import lombok.Data;

@Data
public class Tuple<F, S> {
    private F first;
    private S second;

    public Tuple(F first, S second) {
        this.first = first;
        this.second = second;
    }
}
