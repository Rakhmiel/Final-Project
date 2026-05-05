package finalProject;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import finalProject.HashSet.*;
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
        int majors = Integer.parseInt(args[0]);
        storage = new HashTableImpl(majors);
    }


}