package net.sf.ehcache.util;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Nabib
 * 
 * @param <E>
 */
public abstract class LargeCollection<E> extends AbstractCollection<E> {

	private final Collection<E> addSet;
	private final Collection<Object> removeSet;

	public LargeCollection() {
		this.addSet = new HashSet<E>();
		this.removeSet = new HashSet<Object>();
	}

	@Override
	public boolean add(E obj) {
		if (removeSet.contains(obj)) {
			this.removeSet.remove(obj);
		}
		return this.addSet.add(obj);
	}

	@Override
	public boolean contains(Object obj) {
		return !removeSet.contains(obj) ? addSet.contains(obj)
				|| super.contains(obj) : false;
	}

	@Override
	public boolean remove(Object obj) {
		if (addSet.contains(obj)) {
			addSet.remove(obj);
		}
		return removeSet.add(obj);
	}

	@Override
	public boolean removeAll(Collection<?> removeCandidates) {
		boolean remove = true;
		for(Iterator iter = removeCandidates.iterator(); iter.hasNext();) {
			remove = remove(iter.next()) & remove;
		}
		return remove;
	}

	protected Iterator<E> additionalIterator() {
		return addSet.iterator();
	}

	public Iterator<E> iterator() {
		List<Iterator<E>> iterators = new ArrayList<Iterator<E>>();
		iterators.add(sourceIterator());
		iterators.add(additionalIterator());
		return new AggregateIterator<E>(removeSet, iterators);
	}

	public int size() {
		return sourceSize() + addSet.size();
	}

	public abstract Iterator<E> sourceIterator();

	public abstract int sourceSize();

}