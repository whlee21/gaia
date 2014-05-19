package gaia.curator.example1;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;

public class CuratorStart {
	public void startCurator() {
		CuratorFramework curatorFramework;
		curatorFramework = CuratorFrameworkFactory.builder().connectionTimeoutMs(1000)
				.retryPolicy(new RetryNTimes(10, 500)).connectString(Config.zkConnectionString).build();
		curatorFramework.start();
	}
}
