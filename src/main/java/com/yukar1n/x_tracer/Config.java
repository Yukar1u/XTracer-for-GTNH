package com.yukar1n.x_tracer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraftforge.common.config.Configuration;

public class Config {
    public static String greeting = "Hello World";

    // 创建哈希表存储客户端配置
    private static final Map<UUID, ClientConfig> playerConfigs = new HashMap<>();

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        greeting = configuration.getString("greeting", Configuration.CATEGORY_GENERAL, greeting, "How shall I greet?");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    // 获取客户端配置，没有则新建
    public static ClientConfig getPlayerConfig(UUID playerID) {
        return playerConfigs.computeIfAbsent(playerID, k -> new ClientConfig());
    }

    // 删除客户端配置
    public static void removePlayerConfig(UUID playerID) {
        playerConfigs.remove(playerID);
    }
}
