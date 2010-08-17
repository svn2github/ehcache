package net.sf.ehcache.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Wraps a set to provide a list interface.
 * 
 * All list methods not application to set throws an {@link UnsupportedOperationException}
 * 
 * @author Nabib El-Rahman
 *
 */
public class SetWrapperList implements List {

	private final Collection delegate;

	/**
	 * Collection to delegate to.
	 * 
	 * @param delegate
	 */
	public SetWrapperList(Collection delegate) {
		this.delegate = delegate;
	}

	/**
     * {@inheritDoc}
     */
	public boolean add(Object obj) {
		return this.delegate.add(obj);
	}

	 /**
     * Unsupported
     */
	public void add(int paramInt, Object paramE) {
		throw new UnsupportedOperationException(
				"Delegates to set, operation not supported");
	}

	 /**
     * {@inheritDoc}
     */
	public boolean addAll(Collection coll) {
		return this.delegate.addAll(coll);
	}

	 /**
     * Unsupported
     */
	public boolean addAll(int paramInt, Collection paramCollection) {
		throw new UnsupportedOperationException(
				"Delegates to set, operation not supported");
	}

	 /**
     * {@inheritDoc}
     */
	public void clear() {
		this.delegate.clear();
	}

	 /**
     * {@inheritDoc}
     */
	public boolean contains(Object obj) {
		return this.delegate.contains(obj);
	}

	 /**
     * {@inheritDoc}
     */
	public boolean containsAll(Collection coll) {
		return this.delegate.containsAll(coll);
	}

	 /**
     * Unsupported
     */
	public Object get(int paramInt) {
		throw new UnsupportedOperationException(
				"Delegates to set, operation not supported");
	}

	 /**
     * Unsupported
     */
	public int indexOf(Object paramObject) {
		throw new UnsupportedOperationException(
				"Delegates to set, operation not supported");
	}

	 /**
     * {@inheritDoc}
     */
	public boolean isEmpty() {
		return this.delegate.isEmpty();
	}

	 /**
     * {@inheritDoc}
     */
	public Iterator iterator() {
		return this.delegate.iterator();
	}

	 /**
     * Unsupported
     */
	public int lastIndexOf(Object paramObject) {
		throw new UnsupportedOperationException(
				"Delegates to set, operation not supported");
	}

	 /**
     * Unsupported
     */
	public ListIterator listIterator() {
		throw new UnsupportedOperationException(
				"Delegates to set, operation not supported");
	}

	 /**
     * Unsupported
     */
	public ListIterator listIterator(int paramInt) {
		throw new UnsupportedOperationException(
				"Delegates to set, operation not supported");
	}

	 /**
     * {@inheritDoc}
     */
	public boolean remove(Object obj) {
		return this.delegate.remove(obj);
	}

	 /**
     * Unsupported
     */
	public Object remove(int paramInt) {
		throw new UnsupportedOperationException(
				"Delegates to set, operation not supported");
	}

	 /**
     * {@inheritDoc}
     */
	public boolean removeAll(Collection coll) {
		return this.delegate.removeAll(coll);
	}

	 /**
     * {@inheritDoc}
     */
	public boolean retainAll(Collection coll) {
		return this.delegate.retainAll(coll);
	}

	 /**
     * Unsupported
     */
	public Object set(int paramInt, Object paramE) {
		throw new UnsupportedOperationException(
				"Delegates to set, operation not supported");
	}

	 /**
     * {@inheritDoc}
     */
	public int size() {
		return this.delegate.size();
	}

	 /**
     * Unsupported
     */
	public List subList(int paramInt1, int paramInt2) {
		throw new UnsupportedOperationException(
				"Delegates to set, operation not supported");
	}

	 /**
     * {@inheritDoc}
     */
	public Object[] toArray() {
		return this.delegate.toArray();
	}

	 /**
     * {@inheritDoc}
     */
	public Object[] toArray(Object[] arr) {
		return this.delegate.toArray(arr);
	}

}
