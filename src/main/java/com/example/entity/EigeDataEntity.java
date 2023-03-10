package com.example.entity;

//import com.baomidou.mybatisplus.annotation.IdType;
//import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class EigeDataEntity {

//    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String devNo;
    private String devName;
    private String unitNo;
    private String pointNo;
    private Long ts;
    private Double temperature; // 温度

        /**
         * 水平
     */
    private Double accelerationPeakX;
    private Double accelerationRmsX;
    private Double speedPeakX;
    private Double speedRmsX;

    /**
     * 竖直
     */
    private Double accelerationPeakY;
    private Double accelerationRmsY;
    private Double speedPeakY;
    private Double speedRmsY;

    /**
     * 轴向
     */
    private Double accelerationPeakZ;  // 加速度峰值
    private Double accelerationRmsZ; // 加速度有效值
    private Double speedPeakZ; // 速度峰值
    private Double speedRmsZ; // 速度有效值

    /**
     * 冲击
     */
    private Double envelopEnergy;

    public String toString(boolean isTemperature) {
        if (isTemperature) {
            return devNo + "-" + pointNo + ": " + temperature;
        } else {
            return devNo + "-" + pointNo +
                    ", IntegratPk2Pk/2:" + accelerationPeakX +
                    ", IntegratRMS: " + accelerationRmsX +
                    ", DiagnosisPeak: " + speedPeakX +
                    ", RMSValues" + speedRmsX;
        }
    }
}
