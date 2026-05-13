package finalProject;

import java.util.Collection;
import java.util.Set;

public interface HashTable<Key, Value> {
    /** @return the value stored for k, or null if not found */
    Value get(Key k);

    /**
     * Store v at key k. Pass null as v to delete.
     * @return the previous value for k, or null if the key was absent
     */
    Value put(Key k, Value v);

    /** @throws NullPointerException if key is null */
    boolean containsKey(Key key);

    Set<Key> keySet();

    Collection<Value> values();

    int size();
}
