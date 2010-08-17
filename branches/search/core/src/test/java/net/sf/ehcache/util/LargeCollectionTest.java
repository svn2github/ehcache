package net.sf.ehcache.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class LargeCollectionTest {

	@Test
	public void testIteration() throws IOException {
		
		AggregateIterator<String> iterator = newIterator();
		int i = 0;
		while(iterator.hasNext()) {
			String s = iterator.next();
			Assert.assertNotSame("key1", s);
			Assert.assertNotSame("key2", s);
			i++;
		}
		Assert.assertEquals(198, i);
		
		LargeSet set = new LargeSet() {

			@Override
			public Iterator sourceIterator() {
				return newIterator();
			}

			@Override
			public int sourceSize() {
				return 198;
			}
			
		};
		
		Assert.assertEquals(198, set.size());
		
		HashSet additionalSet =  new HashSet();
		
		for(int j = 0 ; j < 100; j++) {
			additionalSet.add("keyb" + j);
		}
		
		HashSet removeSet = new HashSet(); 
		
		for(int j = 0; j < 50; j++) {
			removeSet.add("keyb" + j);
		}
		
		int beforeSize = set.size();
		set.addAll(additionalSet);
		set.removeAll(removeSet);
		
		Assert.assertEquals(beforeSize + 50, set.size());
//		iterator = newIterator();
//		while(iterator.hasNext()) {
//			iterator.next();
//			iterator.remove();
//		}
		
		
	}

	private AggregateIterator<String> newIterator() {
		HashSet removeSet = new HashSet();
		removeSet.add("key1");
		removeSet.add("key2");
		
		HashSet sourceSet1 = new HashSet();
		for(int i = 0; i < 100; i++) {
			sourceSet1.add("key" + i);
		}
		
		HashSet sourceSet2 = new HashSet();
		for(int i = 0; i < 100; i++) {
			sourceSet1.add("keya" + i);
		}
		List sources = new ArrayList(4);
		sources.add(sourceSet1.iterator());
		sources.add(sourceSet2.iterator());
		
		AggregateIterator<String> iterator = new AggregateIterator<String>(removeSet, sources);
		return iterator;
	}
}
