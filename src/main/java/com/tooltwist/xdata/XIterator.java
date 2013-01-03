package com.tooltwist.xdata;

import java.util.Iterator;


/**
 * This class acts as an adaptor to allow standard Java iteration over XDSelector objects.
 * <p>
 * It is required because the APIs of {@link XDSelector} and {@link Iterator} are incompatible
 * due to different signature and semantics of the method {@code next()}.
 * <p>
 * A XDSelector iterates internally with each call to {@link XDSelector#next()}. For each iteration, the
 * object to access the data remains the same (i.e. the XDSelector).
 * <p>
 * An iterator returns a different object each time {@link Iterator#next()} is called.
 * <p>
 * These differences prevent XDSelector objects from being able to implement {@code Iterable<XDSelector>}
 * themselves. This class will allow iteration z z z z ZZZZZ 
 * 
 * @author philipcallender
 *
 */
public class XIterator implements Iterator<XDSelector> {
	
	private XDSelector selectable;

	public XIterator(XDSelector selectable) {
		this.selectable = selectable;
	}

	public boolean hasNext() {
		boolean hasNext = selectable.hasNext();
		return hasNext;
	}

	/**
	 * Return the next item in the list.
	 * <p>
	 * Actually the same XDSelector is returned every time, but its internal
	 * pointer will be positioned to the next record in the list it maintains.
	 */
	public XDSelector next() {
		selectable.next();
		return selectable;
	}

	/**
	 * This method is not supported so will be ignored.
	 * It is only implemented to provide completeness of the Iterator interface. 
	 */
	public void remove() {
		// Method remove() is not supported
	}

}
