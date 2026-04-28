package edu.yu.cs.com1320.project.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.yu.cs.com1320.project.*;

/**
 * Instances of HashTable should be constructed with two type parameters, one for the type of the keys in the table and one for the type of the values
 *
 * @param <Key>
 * @param <Value>
 */
public class HashTableImpl<Key, Value> implements HashTable<Key, Value> {
    private personObject[] dataArray;
    private int size;
    private int totalSalary;
    //Constructor
    @SuppressWarnings("unchecked")
    public HashTableImpl() {
        this.dataArray = (personObject[]) new Entry[5];
    }
    /**
     * @param k the key whose value should be returned
     * @return the value that is stored in the HashTable for k, or null if there is no such key in the table
     */
    @Override
    public Value get(Key k) {
        if (k == null) {
            return null;
        }
        int index = hashValue(k);
        Entry<Key, Value> current = this.dataArray[index];
        while (current != null) {
            if (current.key.equals(k)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    /**
     * @param k the key at which to store the value
     * @param v the value to store
     *          To delete an entry, put a null value.
     * @return if the key was already present in the HashTable, return the previous value stored for the key. If the key was not already present, return null.
     */
    @Override
    public Value put(Key key, Value value, Int ID) {
        int index = hashValue(key);
        personObject current = this.dataArray[index];
        Entry<Key, Value> previous = null;

        while (current != null) {
            if (current.getID().equals(ID)) {
                Value oldValue = current.value;
                if (value == null) {
                        //deletes it
                        if (previous == null) {
                            this.dataArray[index] = current.next;
                        }
                        else {
                            previous.next = current.next;
                        }
                        this.size--;
                    //overwrite an old value
                    } else {
                        current.value = value;
                    }
                    return oldValue;
                }
                //complete overwrite
                previous = current;
                current = current.next;
            }
            //if the key isnt found
            if (value == null) {
                return null;
            }
            if (this.size >= dataArray.length) {
                resize();
                index = hashValue(key);
            }
            this.dataArray[index] = new Entry<>(key, value, this.dataArray[index]);
            this.size++;
            return null;
        }

    /**
     * @param key the key whose presence in the hashtabe we are inquiring about
     * @return true if the given key is present in the hashtable as a key, false if not
     * @throws NullPointerException if the specified key is null
     */
    @Override
    public boolean containsKey(Key key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return get(key) != null;
    }

    /**
     * @return an unmodifiable set of all the keys in this HashTable
     * @see java.util.Collections#unmodifiableSet(Set)
     */
    @Override
    public Set<Key> keySet() {
        Set<Key> keys = new HashSet<>();
        for (int i = 0; i < dataArray.length; i++) {
            Entry current = dataArray[i];
            while (current != null) {
                keys.add((Key) current.key);
                current = current.next;
            }
        }
        return Collections.unmodifiableSet(keys);
    }

    /**
     * @return an unmodifiable collection of all the values in this HashTable
     * @see java.util.Collections#unmodifiableCollection(Collection)
     */
    @Override
    public Collection<Value> values() {
        Set<Value> values = new HashSet<>();
        for (int i = 0; i < dataArray.length; i++) {
            Entry current = dataArray[i];
            while (current != null) {
                values.add((Value) current.value);
                current = current.next;
            }
        }
        return Collections.unmodifiableSet(values);
    }

    /**
     * @return how entries there currently are in the HashTable
     */
    @Override
    public int size() {
        return size;
    }
    //resizing
    @SuppressWarnings("unchecked")
    private void resize() {
        Entry<Key, Value>[] oldArray = this.dataArray;
        Entry<Key, Value>[] newArray = (Entry<Key, Value>[]) new Entry[oldArray.length * 2];

        this.dataArray = newArray;

        for (Entry<Key, Value> bucket : oldArray) {
            Entry<Key, Value> current = bucket;

            while (current != null) {
                Entry<Key, Value> next = current.next;
                int index = hashValue(current.key);
                current.next = newArray[index];
                newArray[index] = current;
                current = next;
            }
        }
    /**
    * @return List of majorObjects
    */
    @SuppressWarnings("unchecked")
    public ArrayList<majorObject> getMajors() {
        //makes a major list
        ArrayList<majorObject> majors = new ArrayList<>();
        //iterates through the buckets in the array
        for (Entry<Key, Value> bucket : dataArray) {
            //iterates through the people in the array
            Entry<Key, Value> current = bucket;
            //creates a major object
            majorObject major = new majorObject(Value.getMajor);
            //adds the salary of each person to the totalSalary of the major object
            while (current != null) {
                major.addSalary(Value.getSalary);
                current = current.next;
            }
            //adds the major to the list
            majors.add(major);
        }
        return majors;

    }

    private int hashValue(Key key) {
        return Math.abs(key.hashCode() % this.dataArray.length);
    }
    //the linked list class
    private static class Entry<Key, Value> {
        private Key key;
        private Value value;
        Entry<Key, Value> next;

        public Entry(Key key, Value value, Entry<Key, Value> next) {
            this.key = key;
            this.value = value;
            this.next = next;
    }
}
}