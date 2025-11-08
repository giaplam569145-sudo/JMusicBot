/*
 * Copyright 2016 John Grosh (jagrosh).
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

import java.util.HashSet;
import java.util.Set;

/**
 * A queue that attempts to fairly distribute tracks among users.
 *
 * @param <T> The type of the items in the queue.
 * @author John Grosh (jagrosh)
 */
public class FairQueue<T extends Queueable> extends AbstractQueue<T>
{
    public FairQueue(AbstractQueue<T> queue)
    {
        super(queue);
    }

    protected final Set<Long> set = new HashSet<>();

    /**
     * Adds an item to the queue, attempting to place it fairly.
     *
     * @param item The item to add.
     * @return The index at which the item was added.
     */
    @Override
    public int add(T item)
    {
        int lastIndex;
        for(lastIndex=list.size()-1; lastIndex>-1; lastIndex--)
            if(list.get(lastIndex).getIdentifier() == item.getIdentifier())
                break;
        lastIndex++;
        set.clear();
        for(; lastIndex<list.size(); lastIndex++)
        {
            if(set.contains(list.get(lastIndex).getIdentifier()))
                break;
            set.add(list.get(lastIndex).getIdentifier());
        }
        list.add(lastIndex, item);
        return lastIndex;
    }

}
