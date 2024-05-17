package hxy.dragon.util;

import org.junit.jupiter.api.Test;

import hxy.dragon.extend.FeishuRobotUtil;

public class FeishuRobotUtilTest {
	
	@Test
	public void testSend() {
		FeishuRobotUtil.feishuRobotDetail("测试消息发送");
	}

}
