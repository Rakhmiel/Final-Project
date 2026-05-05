package edu.yu.cs.com1320.project.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import edu.yu.cs.com1320.project.HashTable;
import edu.yu.cs.com1320.project.majorObject;
import edu.yu.cs.com1320.project.personObject;

/**
 * Instances of HashTable should be constructed with two type parameters, one for the type of the keys in the table and one for the type of the values
 *
 * @param <Key>
 * @param <Value>
 */
public class HashTableImpl<Key, Value> implements HashTable<Key, Value> {
    private Entry<Key, Value>[] dataArray;
    private int size;
    //Constructor
    /**
     * @param arrayLength the amount of groups of objects you want to insert (majors, etc)
     */
    @SuppressWarnings("unchecked")
    public HashTableImpl(int arrayLength) {
        this.dataArray = (Entry<Key, Value>[]) new Entry[arrayLength];
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
    @SuppressWarnings("unchecked")
    public personObject put(Key key, Value valueIn) {
        int index = hashValue(key);
        personObject current = (personObject) this.dataArray[index].value;
        personObject previous = null;
        personObject value = (personObject) valueIn;

        while (current != null) {
            if (current.getID() == value.getID()) {
                personObject oldValue = current;
                if (valueIn == null) {
                        //deletes it
                        if (previous == null) {
                            this.dataArray[index] = this.dataArray[index].next;
                        }
                        else {
                            this.dataArray[index - 1] = this.dataArray[index].next;
                        }
                        this.size--;
                    //overwrite an old value
                    } else {
                        current = (personObject) valueIn;
                    }
                    return oldValue;
                }
                //complete overwrite

                previous = (personObject) this.dataArray[index].value;
                current = (personObject) this.dataArray[index].next.value;
            }
            //if the key isnt found
            if (value == null) {
                return null;
            }
            /* if (this.size >= dataArray.length) {
                resize();
                index = hashValue(key);
            }
                */
            this.dataArray[index] = new Entry<>(key, valueIn, this.dataArray[index]);
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
    /*
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
    }
    */
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
            if (current == null) {
                personObject currentPerson = (personObject) current.value;
                //creates a major object
                majorObject major = new majorObject(currentPerson.getMajor());
                //adds the salary of each person to the totalSalary of the major object
                while (current != null) {
                    major.addSalary(currentPerson.getSalary());
                    current = current.next;
                    currentPerson = (personObject) current.value;
                }
                //adds the major to the list
                majors.add(major);
            }
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