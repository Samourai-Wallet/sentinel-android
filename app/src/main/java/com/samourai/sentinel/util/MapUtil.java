package com.samourai.sentinel.util;

import java.util.*;

public class MapUtil {

    private static MapUtil instance = null;

    private MapUtil()   { ; }

    public static MapUtil getInstance() {

        if(instance == null)    {
            instance = new MapUtil();
        }

        return instance;
    }

    public <K, V extends Comparable<? super V>> HashMap<K, V> sortByValue(HashMap<K, V> map) {
        List<HashMap.Entry<K, V>> list = new LinkedList<HashMap.Entry<K, V>>(map.entrySet());
        Collections.sort( list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo( o2.getValue() );
            }
        });

        HashMap<K, V> result = new LinkedHashMap<K, V>();
        for (HashMap.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

}