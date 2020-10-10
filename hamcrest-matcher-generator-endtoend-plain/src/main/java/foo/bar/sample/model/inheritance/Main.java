package foo.bar.sample.model.inheritance;

import java.util.List;

public class Main {
    private final List<Parent> parents;

    public Main(final List<Parent> parents) {
        this.parents = parents;
    }

    public List<Parent> getParents() {
        return parents;
    }
}
