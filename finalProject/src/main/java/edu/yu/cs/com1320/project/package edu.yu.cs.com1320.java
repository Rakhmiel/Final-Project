package edu.yu.cs.com1320.project;

import edu.yu.cs.com1320.project.HashSet.*;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;

public class comparisonProgram {
    private HashTableImpl storage;
    public static void main(String[] args) {
        int majors = args[0];
        storage = new HashTableImpl(majors);
    }


}