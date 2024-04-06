package club.hellin.forceblocks.utils;

import java.util.ArrayList;
import java.util.List;

public abstract class ComponentManager<T> {
    private final List<T> components = new ArrayList<>();

    public abstract void init();

    public void register(final T component) {
        if (this.components.contains(component))
            return;

        this.components.add(component);
    }

    public void remove(final T component) {
        this.components.remove(component);
    }

    /**
     * Returns mutable list of Components
     * @return
     */
    public List<T> get() {
        return this.components;
    }
}