/*
 * ImmutableListIterator.java from LicenseManager modified Tuesday, February 21, 2012 10:59:35 CST (-0600).
 *
 * Copyright 2010-2013 the original author or authors.
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

package net.nicholaswilliams.java.licensing.immutable;

import java.util.ListIterator;

/**
 * Wraps a list iterator such that it cannot be modified.
 *
 * @author Nick Williams
 * @version 1.0.0
 * @since x.x.x
 */
public final class ImmutableListIterator<E> implements Immutable, ListIterator<E>
{
	private final ListIterator<E> internal;

	private final ValidObject validObject;

	ImmutableListIterator(ListIterator<E> iterator, ValidObject validObject)
	{
		this.internal = iterator;
		this.validObject = validObject;
	}

	@Override
	public boolean hasNext()
	{
		synchronized(this.validObject)
		{
			this.validObject.checkValidity();
			return this.internal.hasNext();
		}
	}

	@Override
	public boolean hasPrevious()
	{
		synchronized(this.validObject)
		{
			this.validObject.checkValidity();
			return this.internal.hasPrevious();
		}
	}

	@Override
	public E next()
	{
		synchronized(this.validObject)
		{
			this.validObject.checkValidity();
			return this.internal.next();
		}
	}

	@Override
	public int nextIndex()
	{
		synchronized(this.validObject)
		{
			this.validObject.checkValidity();
			return this.internal.nextIndex();
		}
	}

	@Override
	public E previous()
	{
		synchronized(this.validObject)
		{
			this.validObject.checkValidity();
			return this.internal.previous();
		}
	}

	@Override
	public int previousIndex()
	{
		synchronized(this.validObject)
		{
			this.validObject.checkValidity();
			return this.internal.previousIndex();
		}
	}

	@Override
	public void add(E e)
	{
		throw new UnsupportedOperationException("This iterator cannot be modified.");
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("This iterator cannot be modified.");
	}

	@Override
	public void set(E e)
	{
		throw new UnsupportedOperationException("This iterator cannot be modified.");
	}
}
