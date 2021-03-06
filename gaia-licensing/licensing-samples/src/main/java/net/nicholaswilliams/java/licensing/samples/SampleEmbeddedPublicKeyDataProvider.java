/*
 * SampleEmbeddedPublicKeyDataProvider.java from LicenseManager modified Monday, March 5, 2012 22:05:18 CST (-0600).
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

package net.nicholaswilliams.java.licensing.samples;

import net.nicholaswilliams.java.licensing.encryption.PublicKeyDataProvider;
import net.nicholaswilliams.java.licensing.exception.KeyNotFoundException;

@SuppressWarnings("unused")
public final class SampleEmbeddedPublicKeyDataProvider implements PublicKeyDataProvider
{
	@Override
	public byte[] getEncryptedPublicKeyData() throws KeyNotFoundException
	{
		return new byte[] {
				0x0000003C, 0x00000031, 0x00000062, 0xFFFFFFA7, 0xFFFFFFBC, 0xFFFFFFA3, 0x0000001A, 0x0000007E,
				0x00000058, 0x0000001C, 0xFFFFFFB0, 0x0000007E, 0xFFFFFF90, 0xFFFFFF97, 0xFFFFFFF0, 0x0000005E,
				0x00000036, 0xFFFFFF8A, 0xFFFFFFA6, 0xFFFFFFB1, 0xFFFFFF8F, 0x0000007A, 0xFFFFFFCE, 0xFFFFFFE6,
				0xFFFFFFBD, 0xFFFFFFAF, 0xFFFFFF80, 0x00000057, 0x00000044, 0xFFFFFFD1, 0xFFFFFF89, 0xFFFFFF9B,
				0x0000000B, 0x00000073, 0xFFFFFFBB, 0xFFFFFF91, 0xFFFFFFAE, 0xFFFFFFF3, 0x0000006D, 0xFFFFFFB2,
				0x00000037, 0x00000074, 0x00000053, 0xFFFFFFB8, 0x00000029, 0x00000017, 0xFFFFFFA8, 0x00000021,
				0xFFFFFF9D, 0x00000023, 0xFFFFFFF9, 0x0000000C, 0x00000058, 0xFFFFFF8F, 0xFFFFFFA9, 0xFFFFFFD6,
				0xFFFFFFC0, 0xFFFFFFF8, 0x0000005A, 0x00000013, 0xFFFFFFC3, 0x00000052, 0xFFFFFFBB, 0xFFFFFFE6,
				0x00000049, 0x0000006E, 0xFFFFFFEF, 0x0000005E, 0x00000072, 0xFFFFFFDC, 0x0000001D, 0x0000005B,
				0x0000002D, 0x00000049, 0x00000007, 0xFFFFFFA3, 0x0000007A, 0xFFFFFF98, 0x00000002, 0xFFFFFFC9,
				0xFFFFFFF2, 0x00000049, 0xFFFFFF9E, 0x00000074, 0x00000076, 0x00000034, 0x0000000E, 0x00000045,
				0x00000033, 0x00000021, 0xFFFFFFE8, 0xFFFFFFF5, 0x00000003, 0xFFFFFFE1, 0x00000019, 0xFFFFFFC5,
				0xFFFFFFA4, 0x00000053, 0x0000007F, 0xFFFFFFE0, 0x00000004, 0xFFFFFFC9, 0xFFFFFF86, 0x00000019,
				0x00000066, 0x00000002, 0x00000032, 0xFFFFFFF7, 0x0000007F, 0x0000004D, 0xFFFFFF9F, 0xFFFFFFB5,
				0xFFFFFFC4, 0xFFFFFFB7, 0x00000074, 0xFFFFFFDE, 0x0000006E, 0xFFFFFFEF, 0x00000052, 0xFFFFFFD2,
				0xFFFFFFDC, 0x00000048, 0xFFFFFF9B, 0x00000050, 0xFFFFFFAB, 0x00000060, 0xFFFFFFFF, 0x0000005D,
				0x00000067, 0xFFFFFF9E, 0x0000000E, 0xFFFFFFBE, 0xFFFFFF9C, 0x0000005D, 0xFFFFFFBE, 0x00000021,
				0xFFFFFFC8, 0x00000001, 0xFFFFFFAA, 0xFFFFFFA7, 0x0000000C, 0x00000055, 0xFFFFFFD8, 0x00000022,
				0xFFFFFFD2, 0xFFFFFF9A, 0x00000065, 0xFFFFFF8B, 0xFFFFFF9E, 0xFFFFFF8F, 0xFFFFFFF8, 0x00000075,
				0x00000076, 0x00000070, 0x00000074, 0xFFFFFFB3, 0xFFFFFFF1, 0x00000018, 0xFFFFFFD0, 0xFFFFFFAA,
				0x00000042, 0xFFFFFFCF, 0x00000013, 0x0000007E, 0xFFFFFFE0, 0x00000003, 0xFFFFFFE4, 0x00000044,
				0x00000025, 0xFFFFFFB0, 0x0000002D, 0xFFFFFF9D, 0xFFFFFFCE, 0xFFFFFFF7, 0x0000007F, 0xFFFFFF8B,
				0xFFFFFFEC, 0x00000028, 0x00000027, 0x00000054, 0x0000004D, 0x00000020, 0xFFFFFF8A, 0x00000042,
				0x00000013, 0xFFFFFFEC, 0xFFFFFFAB, 0x00000050, 0xFFFFFF96, 0x00000064, 0x00000049, 0x0000001B,
				0x00000039, 0x0000000B, 0xFFFFFFF4, 0xFFFFFFB2, 0xFFFFFFA7, 0x0000005B, 0xFFFFFF8A, 0x0000001B,
				0x00000040, 0x0000006C, 0xFFFFFF89, 0xFFFFFFE7, 0xFFFFFFF6, 0x00000062, 0x00000050, 0xFFFFFFEC,
				0xFFFFFFF6, 0x00000027, 0x00000006, 0xFFFFFFE4, 0xFFFFFFBF, 0xFFFFFFF2, 0xFFFFFFC5, 0xFFFFFFD4,
				0x00000054, 0x0000004A, 0x00000002, 0xFFFFFFE7, 0x00000025, 0xFFFFFF8A, 0xFFFFFFC7, 0xFFFFFFC1,
				0xFFFFFFAF, 0x0000003C, 0xFFFFFFF3, 0xFFFFFF96, 0xFFFFFFB0, 0x0000004E, 0xFFFFFFDB, 0x00000019,
				0x00000033, 0x0000004A, 0xFFFFFF8F, 0xFFFFFF9A, 0xFFFFFFD0, 0xFFFFFF9F, 0xFFFFFFAD, 0x0000005E,
				0xFFFFFFBD, 0xFFFFFF8C, 0xFFFFFFCB, 0xFFFFFFE1, 0xFFFFFFC0, 0xFFFFFFDD, 0x0000002C, 0xFFFFFFFC,
				0xFFFFFFBA, 0x0000005F, 0x00000056, 0x00000053, 0x00000059, 0x00000024, 0xFFFFFFAE, 0xFFFFFFC2,
				0xFFFFFFB4, 0xFFFFFFEC, 0x00000028, 0xFFFFFF8F, 0x0000005A, 0x00000063, 0x00000028, 0xFFFFFFD4,
				0xFFFFFFC7, 0x00000053, 0xFFFFFFF7, 0x00000074, 0x00000045, 0x0000000E, 0xFFFFFFC3, 0x0000006C,
				0x00000049, 0x00000059, 0x00000059, 0x00000076, 0x0000007B, 0x00000005, 0xFFFFFF92, 0xFFFFFFFE,
				0xFFFFFFCA, 0x00000037, 0xFFFFFFB0, 0x0000005C, 0xFFFFFFFE, 0x0000000C, 0x0000000E, 0x00000015,
				0xFFFFFF9C, 0xFFFFFFDF, 0x00000078, 0xFFFFFF87, 0x00000066, 0x00000022, 0x00000063, 0xFFFFFFC2,
				0xFFFFFFA9, 0xFFFFFF82, 0xFFFFFFAB, 0x00000074, 0xFFFFFF9E, 0x0000002C, 0xFFFFFF91, 0xFFFFFFCB
		};
	}
}
