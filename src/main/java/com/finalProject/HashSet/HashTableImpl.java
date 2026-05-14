package finalProject.HashSet;

import finalProject.HashTable;
import finalProject.majorObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class HashTableImpl<Key, Value> implements HashTable<Key, Value> {
    private Entry<Key, Value>[] dataArray;
    private int size;

    @SuppressWarnings("unchecked")
    public HashTableImpl(int initialCapacity) {
        this.dataArray = (Entry<Key, Value>[]) new Entry[Math.max(initialCapacity, 1)];
    }

    @Override
    public Value get(Key k) {
        if (k == null) return null;
        Entry<Key, Value> current = dataArray[hashValue(k)];
        while (current != null) {
            if (current.key.equals(k)) return current.value;
            current = current.next;
        }
        return null;
    }

    @Override
    public Value put(Key key, Value valueIn) {
        int index = hashValue(key);
        Entry<Key, Value> current = dataArray[index];
        Entry<Key, Value> previous = null;

        while (current != null) {
            if (current.key.equals(key)) {
                Value old = current.value;
                if (valueIn == null) {
                    if (previous == null) dataArray[index] = current.next;
                    else previous.next = current.next;
                    size--;
                } else {
                    current.value = valueIn;
                }
                return old;
            }
            previous = current;
            current = current.next;
        }

        if (valueIn == null) return null;
        dataArray[index] = new Entry<>(key, valueIn, dataArray[index]);
        size++;
        return null;
    }

    @Override
    public boolean containsKey(Key key) {
        if (key == null) throw new NullPointerException();
        return get(key) != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Key> keySet() {
        Set<Key> keys = new HashSet<>();
        for (Entry<Key, Value> bucket : dataArray) {
            Entry<Key, Value> current = bucket;
            while (current != null) {
                keys.add(current.key);
                current = current.next;
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Value> values() {
        Set<Value> vals = new HashSet<>();
        for (Entry<Key, Value> bucket : dataArray) {
            Entry<Key, Value> current = bucket;
            while (current != null) {
                vals.add(current.value);
                current = current.next;
            }
        }
        return Collections.unmodifiableSet(vals);
    }

    @Override
    public int size() { return size; }

    /** Returns all majorObjects stored in the table, sorted descending by average salary. */
    @SuppressWarnings("unchecked")
    public ArrayList<majorObject> getMajors() {
        ArrayList<majorObject> majors = new ArrayList<>();
        for (Entry<Key, Value> bucket : dataArray) {
            Entry<Key, Value> current = bucket;
            while (current != null) {
                majors.add((majorObject) current.value);
                current = current.next;
            }
        }
        majors.sort(Collections.reverseOrder());
        return majors;
    }

    private int hashValue(Key key) {
        return Math.abs(key.hashCode() % dataArray.length);
    }

    private static class Entry<Key, Value> {
        Key key;
        Value value;
        Entry<Key, Value> next;

        Entry(Key key, Value value, Entry<Key, Value> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }
}
