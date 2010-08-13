package net.sf.ehcache.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * This Iterator iterates over a collection of iterators.
 * 
 * @author Nabib
 *
 * @param <T>
 */
public class AggregateIterator<T> implements Iterator<T> {

	private final Collection<?> removeColl;
	protected final Iterator<Iterator<T>> iterators;
	protected Iterator<T> currentIterator;
	protected T next = null;

	private Iterator<T> getNextIterator() {
		return iterators.next();
	}

	/**
	 * 
	 * @param removeColl collection of removed entries to check against
	 * @param iterators collection of iterators 
	 */
	public AggregateIterator(Collection<?> removeColl,
			List<Iterator<T>> iterators) {
		this.removeColl = removeColl;
		this.iterators = iterators.iterator();
		while (this.iterators.hasNext()) {
			this.currentIterator = getNextIterator();
			if (this.currentIterator.hasNext()) {
				next = this.currentIterator.next();
				return;
			}
		}
	}

	 /**
     * {@inheritDoc}
     */
	public boolean hasNext() {
		return next != null;
	}

	 /**
     * {@inheritDoc}
     */
	public T next() {
		if (next == null) {
			throw new NoSuchElementException();
		} else {
			T returnNext = next;
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
	 * Is not supported.
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
