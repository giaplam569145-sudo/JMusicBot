/*
 * Copyright 2018 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.entities;

/**
 * A generic class to hold a pair of objects.
 *
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Pair<K, V>
{
    private final K key;
    private final V value;
    
    /**
     * Constructs a new Pair.
     *
     * @param key   The key object.
     * @param value The value object.
     */
    public Pair(K key, V value)
    {
        this.key = key;
        this.value = value;
    }
    
    /**
     * Gets the key of the pair.
     *
     * @return The key.
     */
    public K getKey()
    {
        return key;
    }
    
    /**
     * Gets the value of the pair.
     *
     * @return The value.
     */
    public V getValue()
    {
        return value;
    }
}
