package com.example.model;

import lombok.Data;
import lombok.ToString;

/**
 * mqtt静态模型
 *
 * @author sunhaitong
 * @date 2021/8/16
 */

@Data
@ToString
public class MQTTStatsicModel {

    private String dataWatchNo;

    /**
     * 数据的时间戳
     */
    private Long timeStamp;

    private String dataNodeNo;

    /**
     * 该设备的所有通道数据
     */
    private String data;
}
