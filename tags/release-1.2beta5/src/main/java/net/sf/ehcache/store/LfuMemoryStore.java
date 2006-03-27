/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */
package net.sf.ehcache.store;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

/**
 * Less Frequently Used (LFU) implementation of the memory store. Actually keeping track of the least used, then the
 * second least and so on is very expensive, 3 or 4 orders of magnitude more expensive than the others policies. Costs are
 * incurred on put, get and remove.
 * <p/>
 * Instead this implementation does not quarantee that
 * the element removed will actually be the least used. Rather, it lets you make statements about confidence intervals
 *  against the likelihood that an element is in some lowest percentile of the hit count distribution. Costs are only
 * incurred on overflow when an element to be evicted must be selected.
 * <p/>
 * * For those with a statistical background the branch of stats which deals with this is hypothesis testing and
 * the Student's T distribution. The higher your sample the greater confidence you can have in a hypothesis, in
 * this case whether or not the "lowest" value lies in the bottom half or quarter of the distribution. Adding
 * samples rapidly increases confidence but the return from extra sampling rapidly diminishes.
 * <p/>
 * Cost is not affected much by sample size, indicating it is probably the iteration that is causing most of the
 * time. If we had access to the array backing Map, all would work very fast.
 * <p/>
 * A 99.99% confidence interval can be achieved that the "lowest" element is actually in the bottom quarter of the
 * hit count distribution with a sample size of 30, which is the default.
 *
 * @author Greg Luck
 * @version $Id: LfuMemoryStore.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 *          Warning: Testing of this store reveals some problems with it. Do not use. It may be removed.
 */
public class LfuMemoryStore extends MemoryStore {

    private static final Log LOG = LogFactory.getLog(LfuMemoryStore.class.getName());

    private static final int DEFAULT_SAMPLE_SIZE = 30;

    private Random random = new Random();

    /**
     * Constructor for the LfuMemoryStore object
     */
    protected LfuMemoryStore(Cache cache, DiskStore diskStore) {
        super(cache, diskStore);
        map = new HashMap();
    }

    /**
     * Puts an element into the cache
     */
    public synchronized void doPut(Element elementJustAdded) {
        if (isFull()) {
            removeLfuElement(elementJustAdded);
        }
    }


    private void removeLfuElement(Element elementJustAdded) {

        if (LOG.isTraceEnabled()) {
            LOG.info("Cache is full. Removing LFU element ...");
        }

        // First element of the sorted list is the candidate for the removal
        Element element = findRelativelyUnused(elementJustAdded);

        // If the element is expired remove
        if (cache.isExpired(element)) {
            remove(element.getKey());
            notifyExpiry(element);
            return;
        }

        evict(element);
        remove(element.getKey());
    }

    /**
     * Find a "relatively" unused element, but not the element just added
     */
    Element findRelativelyUnused(Element elementJustAdded) {
        Element[] elements = sampleElements(calculateSampleSize());
        Element element = lowestElementFromArray(elements, elementJustAdded);
        return element;
    }

    private int calculateSampleSize() {
        if (map.size() < DEFAULT_SAMPLE_SIZE) {
            return map.size();
        } else {
            return DEFAULT_SAMPLE_SIZE;
        }

    }

    /**
     * Uses random numbers to sample the entire map
     *
     * @param sampleSize how many samples to take
     * @return an array of sampled elements
     */
    Element[] sampleElements(int sampleSize) {
        int[] offsets = generateRandomOffsets(sampleSize);
        Element[] elements = new Element[sampleSize];
        Iterator iterator = map.values().iterator();
        for (int i = 0; i < sampleSize; i++) {
            for (int j = 0; j < offsets[i]; j++) {
                iterator.next();
            }
            elements[i] = (Element) iterator.next();
        }
        return elements;
    }

    private Element lowestElementFromArray(Element[] elements, Element elementJustAdded) {
        //edge condition when Memory Store configured to size 0
        if (elements.length == 1) {
            return elementJustAdded;
        }
        Element lowestElement = null;
        for (int i = 0; i < elements.length; i++) {
            Element element = elements[i];
            if (lowestElement == null) {
                if (!element.equals(elementJustAdded)) {
                    lowestElement = element;
                }
            } else {
                if (element.getHitCount() < lowestElement.getHitCount() && !element.equals(elementJustAdded)) {
                    lowestElement = element;
                }
            }
        }
        return lowestElement;
    }

    private int[] generateRandomOffsets(int sampleSize) {
        int size = map.size();
        int[] offsets = new int[sampleSize];
        int maxOffset = size / sampleSize;
        for (int i = 0; i < sampleSize; i++) {
            offsets[i] = random.nextInt(maxOffset);
        }
        return offsets;
    }
}





