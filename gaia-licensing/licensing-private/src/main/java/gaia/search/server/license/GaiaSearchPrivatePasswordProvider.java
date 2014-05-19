package gaia.search.server.license;

import net.nicholaswilliams.java.licensing.encryption.PasswordProvider;

public class GaiaSearchPrivatePasswordProvider implements PasswordProvider {
	@Override
	public char[] getPassword()
	{
		return new char[] {
				0x00000067, 0x00000061, 0x00000069, 0x00000061, 0x00000073, 0x00000065, 0x00000061, 0x00000072, 
				0x00000063, 0x00000068, 0x00000070, 0x00000072, 0x00000069, 0x00000076, 0x00000061, 0x00000074, 
				0x00000065
		};
	}

}
