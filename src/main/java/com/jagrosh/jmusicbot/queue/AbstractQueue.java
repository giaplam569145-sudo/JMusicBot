/*
 * Copyright 2022 John Grosh (jagrosh).
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
package com.jagrosh.jmusicbot.queue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * An abstract base class for different queue implementations.
 *
 * @param <T> The type of the items in the queue.
 * @author Wolfgang Schwendtbauer
 */
public abstract class AbstractQueue<T extends Queueable>
{
    protected AbstractQueue(AbstractQueue<T> queue)
    {
        this.list = queue != null ? queue.getList() : new LinkedList<>();
    }

    protected final List<T> list;

    public abstract int add(T item);

    /**
     * Adds an item at a specific index in the queue.
     *
     * @param index The index at which to add the item.
     * @param item  The item to add.
     */
    public void addAt(int index, T item)
    {
        if(index >= list.size())
            list.add(item);
        else
            list.add(index, item);
    }

    /**
     * Gets the size of the queue.
     *
     * @return The number of items in the queue.
     */
    public int size() {
        return list.size();
    }

    /**
     * Pulls the next item from the queue.
     *
     * @return The next item.
     */
    public T pull() {
        return list.remove(0);
    }

    /**
     * Checks if the queue is empty.
     *
     * @return True if the queue is empty, false otherwise.
     */
    public boolean isEmpty()
    {
        return list.isEmpty();
    }

    /**
     * Gets the list of items in the queue.
     *
     * @return The list of items.
     */
    public List<T> getList()
    {
        return list;
    }

    /**
     * Gets an item at a specific index.
     *
     * @param index The index of the item.
     * @return The item at the specified index.
     */
    public T get(int index) {
        return list.get(index);
    }

    /**
     * Removes an item at a specific index.
     *
     * @param index The index of the item to remove.
     * @return The removed item.
     */
    public T remove(int index)
    {
        return list.remove(index);
    }

    /**
     * Removes all items with a specific identifier.
     *
     * @param identifier The identifier of the items to remove.
     * @return The number of items removed.
     */
    public int removeAll(long identifier)
    {
        int count = 0;
        for(int i=list.size()-1; i>=0; i--)
        {
            if(list.get(i).getIdentifier()==identifier)
            {
                list.remove(i);
                count++;
            }
        }
        return count;
    }

    /**
     * Clears the queue.
     */
    public void clear()
    {
        list.clear();
    }

    /**
     * Shuffles the items with a specific identifier.
     *
     * @param identifier The identifier of the items to shuffle.
     * @return The number of items shuffled.
     */
    public int shuffle(long identifier)
    {
        List<Integer> iset = new ArrayList<>();
        for(int i=0; i<list.size(); i++)
        {
            if(list.get(i).getIdentifier()==identifier)
                iset.add(i);
        }
        for(int j=0; j<iset.size(); j++)
        {
            int first = iset.get(j);
            int second = iset.get((int)(Math.random()*iset.size()));
            T temp = list.get(first);
            list.set(first, list.get(second));
            list.set(second, temp);
        }
        return iset.size();
    }

    /**
     * Skips a number of items in the queue.
     *
     * @param number The number of items to skip.
     */
    public void skip(int number)
    {
        if (number > 0) {
            list.subList(0, number).clear();
        }
    }

    /**
     * Moves an item to a different position in the list.
     *
     * @param from The position of the item.
     * @param to   The new position of the item.
     * @return The moved item.
     */
    public T moveItem(int from, int to)
    {
        T item = list.remove(from);
        list.add(to, item);
        return item;
    }
}
