package gaia.crawl.http.robots;

import java.io.Serializable;

public abstract class BaseRobotsParser implements Serializable {
	public abstract BaseRobotRules parseContent(String paramString1, byte[] paramArrayOfByte, String paramString2,
			String paramString3);

	public abstract BaseRobotRules failedFetch(int paramInt);
}
