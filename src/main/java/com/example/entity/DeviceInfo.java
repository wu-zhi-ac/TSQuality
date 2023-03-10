package com.example.entity;

import cn.hutool.core.annotation.Alias;
import lombok.Data;

/**
 * 设备mac地址--devId配置实体
 *
 * @author sunhaitong
 * @date 2021/8/24
 */
@Data
public class DeviceInfo {
    public DeviceInfo() {
    }

    @Alias("机组号")
    private String unitNO;

    @Alias("设备9码")
    private String devNo;

    @Alias("设备9码中文名")
    private String devName;

    /**
     * 测点编号
     */
    @Alias("测点编号")
    private String pointNO;

    @Alias("传感器编码")
    private String channelId;

}
