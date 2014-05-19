/*
 * FailedToDecryptException.java from LicenseManager modified Friday, September 21, 2012 07:46:54 CDT (-0500).
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

package net.nicholaswilliams.java.licensing.exception;

/**
 * This exception is thrown when the data was corrupt and undecryptable or when
 * the provided decryption password was incorrect. It is impossible to know
 * which is the actual cause.
 *
 * @author Nick Williams
 * @since 1.0.0
 * @version 1.0.0
 */
@SuppressWarnings("unused")
public class FailedToDecryptException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public FailedToDecryptException()
	{
		super("Failed to decrypt the data. Either the password was incorrect or the data was corrupt.");
	}

	public FailedToDecryptException(String message)
	{
		super(message);
	}

	public FailedToDecryptException(Throwable cause)
	{
		super("Failed to decrypt the data. Either the password was incorrect or the data was corrupt.", cause);
	}

	public FailedToDecryptException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
