/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This Iterator iterates over a collection of iterators.
 * @author Nabib
 * @param <T>
 */
public class AggregateIterator < T > implements Iterator < T > {

  /**
   * Collection of removed elements.
   */
  private final Collection < ? > removeColl;

  /**
   * Iterators of iterators.
   */
  private final Iterator < Iterator < T > > iterators;

  /**
   * Current iterator position.
   */
  private Iterator < T > currentIterator;

  /**
   * Next element position.
   */
  private T next;

  /**
   * Current element position.
   */
  private T current;

 /**
  * @param collRemove
  *            collection of removed entries to check against
  * @param listIterators
  *            collection of iterators
  */
  public AggregateIterator(final Collection < ? > collRemove,
       final List < Iterator < T > > listIterators) {
    this.removeColl = collRemove;
    this.iterators = listIterators.iterator();
    while (this.iterators.hasNext()) {
      this.currentIterator = getNextIterator();
      while (this.currentIterator.hasNext()) {
        next = this.currentIterator.next();
        if (!removeColl.contains(next)) {
          return;
        }
      }
    }
    next = null;
  }

   /**
    * {@inheritDoc}
    */
   public final boolean hasNext() {
     return next != null;
   }

   /**
    * {@inheritDoc}
    */
   public final T next() {
     if (next == null) {
       throw new NoSuchElementException();
     } else {
       T returnNext = next;
       current = returnNext;
       next = null;
       if (this.currentIterator == null) {
         throw new NoSuchElementException();
       }

       while (this.currentIterator.hasNext()) {

       T nextCandidate = this.currentIterator.next();
       if (removeColl.contains(nextCandidate)) {
         continue;
       } else {
         next = nextCandidate;
         return returnNext;
       }
       }
       while (this.iterators.hasNext()) {
         this.currentIterator = this.iterators.next();
         while (this.currentIterator.hasNext()) {

            T nextCandidate = this.currentIterator.next();
            if (removeColl.contains(nextCandidate)) {
             continue;
            } else {
             next = nextCandidate;
             return returnNext;
            }
        }
      }
      return returnNext;
      }
    }

   /**
    * {@inheritDoc}
    */
   public final void remove() {
     if (current == null) {
        throw new IllegalStateException();
     }
     this.removeColl.remove(current);
     current = null;
   }

   /**
    * Get next Iterator.
    * @return Iterator
    */
   private Iterator < T > getNextIterator() {
     return iterators.next();
   }


}
