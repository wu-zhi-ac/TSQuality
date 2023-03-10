package com.example.mqtt;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.constant.CommonConstant;
import com.example.entity.DeviceInfo;
import com.example.entity.EigeDataEntity;
import com.example.model.MQTTStatsicModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @package: com.example.mqtt
 * @Author: lenovo
 * @CreateTime: 2023-03-10 09:31
 * @Description:
 * @Version: 1.0
 */
@Configuration
@Slf4j
public class MqttConfig {
    @Value("${spring.mqtt.url}")
    private String mqttHost;

    @Value("${spring.mqtt.username}")
    private String userName;

    @Value("${spring.mqtt.password}")
    private String passWord;

    @Value("#{'${mqtt.topiclist}'.split(',')}")
    private List<String> topicList;

    /**
     * 订阅的bean名称
     */
    public static final String CHANNEL_NAME_IN = "mqttInboundChannel";

    private ExecutorService executorPool = new ThreadPoolExecutor(20, 25, 1000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1024), Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());



    @Bean
    public MqttPahoClientFactory factory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(userName);
        options.setPassword(passWord.toCharArray());
        String [] serverURIs = mqttHost.split(",");
        options.setServerURIs(serverURIs);
        options.setConnectionTimeout(3000);
        options.setKeepAliveInterval(20);
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean(name = CHANNEL_NAME_IN)
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }


    @Bean
    public MessageProducer inbound() {

        String ipAddress = "";
        try {
            InetAddress ip4 = Inet4Address.getLocalHost();
            ipAddress = ip4.getHostAddress() + "@@" + RandomStringUtils.randomNumeric(2);

        } catch (UnknownHostException e) {
            log.error("Error to get IP Address {}", e.getMessage());
            ipAddress = RandomStringUtils.randomAlphabetic(12);
        }
        String clientId = ipAddress;
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(clientId,
                factory(), topicList.toArray(new String[topicList.size()]));
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(0);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    @ServiceActivator(inputChannel = CHANNEL_NAME_IN)
    public MessageHandler handler() {
        return message -> {
            String payload = message.getPayload().toString();
            String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC).toString();
            String factoryName = topic.split("/")[0];
            if (topic.contains("equipment/statisticData")) {
                executorPool.execute(new SaveWiredMsgHandler(topic, payload, factoryName));
            } else if (topic.contains("wireless/statisticData")) {
                executorPool.execute(new SaveWirelessMsgHandler(payload, factoryName));
            }
//            else if (topic.contains("wireless/realTimeData")) {
//                executorPool.execute(new MqttWirelessRawdataMsgHandler(payload));
//            } else if (topic.startsWith("realTimeData/rawdata")){ // 处理有线原始数据
//                executorPool.execute(new OriginDataMqttMsgHandler(payload));
//            }
        };
    }

    /**
     * 保存有线网关数据
     */
    class SaveWiredMsgHandler implements Runnable{

        private String topic;
        private String msg;
        private String factoryName;

        public SaveWiredMsgHandler(String topic, String msg, String factoryName) {
            this.topic = topic;
            this.msg = msg;
            this.factoryName = factoryName;
        }

        @Override
        public void run() {
            MQTTStatsicModel rawModel = JSONObject.parseObject(msg, MQTTStatsicModel.class);
            String data = rawModel.getData();
            JSONArray jsonArray = JSONObject.parseArray(data);
            if (jsonArray == null) {
                log.info("value is null.");
                return;
            }
            List<EigeDataEntity> list = new ArrayList();
            for (int i = 0; i < jsonArray.size(); i++) {

                JSONObject dataJson = jsonArray.getJSONObject(i);
                String channelId = dataJson.getString(CommonConstant.KEY_CHANNEL_ID);
                String channelType = dataJson.getString(CommonConstant.KEY_CHANNEL_TYPE);
                // 判断是否消费数据
//                boolean monitor = DataStandardManager.getInstance().getDeviceInfoMap().containsKey(channelId);
//                if (monitor) {
//                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    Long timeStamp = dataJson.getLong(CommonConstant.TIME_STAMP);
                    // 无线传感器使用系统当前时间
                    if (dataJson.getLong(CommonConstant.TIME_STAMP) == null) {
                        timeStamp = System.currentTimeMillis();
                    } else {
                        timeStamp = timeStamp * 1000L;
                    }

                    // 设置设备信息
//                    DeviceInfo deviceInfo = DataStandardManager.getInstance().getDeviceInfo(channelId);
//                    EigeDataEntity eigeDataEntity = new EigeDataEntity();
//                    eigeDataEntity.setDevNo(deviceInfo.getDevNo());
//                    eigeDataEntity.setDevName(deviceInfo.getDevName());
//                    eigeDataEntity.setPointNo(deviceInfo.getPointNO());
//                    eigeDataEntity.setUnitNo(deviceInfo.getUnitNO());
                    EigeDataEntity eigeDataEntity = new EigeDataEntity();
                    eigeDataEntity.setPointNo(channelId);
                    if ("TEMPERATURE".equals(channelType)) {
                        eigeDataEntity.setTemperature(dataJson.getDouble("DCValues"));

                        log.info(eigeDataEntity.toString(true));
                    } else if ("ACCELEROMETER".equals(channelType)) {
                        eigeDataEntity.setAccelerationPeakX(dataJson.getDouble("IntegratPk2Pk/2"));
                        eigeDataEntity.setAccelerationRmsX(dataJson.getDouble("IntegratRMS"));
                        eigeDataEntity.setSpeedPeakX(dataJson.getDouble("DiagnosisPeak"));
                        eigeDataEntity.setSpeedRmsX(dataJson.getDouble("RMSValues"));

                        log.info(eigeDataEntity.toString(false));
                    }
                    eigeDataEntity.setTs(timeStamp);
                    list.add(eigeDataEntity);
//                }

                if (!list.isEmpty()) {
                    // TODO 有线数据
//                    commonService.commonInsert(list, factoryName);
                }
            }
        }
    }

    /**
     * 消费无线网关数据
     */
    class SaveWirelessMsgHandler implements Runnable {
        private String msg;
        private String factoryName;

        public SaveWirelessMsgHandler(String msg, String factoryName) {
            this.msg = msg;
            this.factoryName = factoryName;
        }

        @Override
        public void run() {
            MQTTStatsicModel rawModel = JSONObject.parseObject(msg, MQTTStatsicModel.class);
            String data = rawModel.getData();
            JSONArray jsonArray = JSONObject.parseArray(data);
            // 获取ChannelId
            EigeDataEntity eigeDataEntity = new EigeDataEntity();
            eigeDataEntity.setTs(System.currentTimeMillis());
            boolean isInsert = true;
            List<EigeDataEntity> list = new ArrayList();
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject dataJson = jsonArray.getJSONObject(i);
                // 设置设备信息
                String channelId = dataJson.getString(CommonConstant.KEY_CHANNEL_ID);
                if (channelId.endsWith("-S")) {
//                    DeviceInfo tmDevice = DataStandardManager.getInstance().getDeviceInfo(channelId);
//                    if (tmDevice == null) {
//                        isInsert = false;
//                        continue;
//                    }
                    EigeDataEntity temperatureDataEntity = new EigeDataEntity();
//                    temperatureDataEntity.setDevNo(tmDevice.getDevNo());
//                    temperatureDataEntity.setDevName(tmDevice.getDevName());
//                    temperatureDataEntity.setPointNo(tmDevice.getPointNO());
//                    temperatureDataEntity.setUnitNo(tmDevice.getUnitNO());
                    temperatureDataEntity.setPointNo(channelId);
                    temperatureDataEntity.setTemperature(dataJson.getDouble("TemperatureTop"));
                    temperatureDataEntity.setTs(System.currentTimeMillis());
                    list.add(temperatureDataEntity);
                } else if (channelId.endsWith("-X")) {
                    eigeDataEntity.setAccelerationPeakX(dataJson.getDouble("diagnosisPk"));
                    eigeDataEntity.setAccelerationRmsX(dataJson.getDouble("rmsValues"));
                    eigeDataEntity.setSpeedPeakX(dataJson.getDouble("integratPk"));
                    eigeDataEntity.setSpeedRmsX(dataJson.getDouble("integratRMS"));
                } else if (channelId.endsWith("-Y")) {
                    eigeDataEntity.setAccelerationPeakY(dataJson.getDouble("diagnosisPk"));
                    eigeDataEntity.setAccelerationRmsY(dataJson.getDouble("rmsValues"));
                    eigeDataEntity.setSpeedPeakY(dataJson.getDouble("integratPk"));
                    eigeDataEntity.setSpeedRmsY(dataJson.getDouble("integratRMS"));
                } else if (channelId.endsWith("-Z")) {
//                    DeviceInfo deviceInfo = DataStandardManager.getInstance().getDeviceInfo(channelId);
//                    if (deviceInfo == null) {
//                        isInsert = false;
//                        continue;
//                    }
//                    eigeDataEntity.setDevNo(deviceInfo.getDevNo());
//                    eigeDataEntity.setDevName(deviceInfo.getDevName());
//                    eigeDataEntity.setPointNo(deviceInfo.getPointNO());
//                    eigeDataEntity.setUnitNo(deviceInfo.getUnitNO());
                    eigeDataEntity.setPointNo(channelId);
                    eigeDataEntity.setAccelerationPeakZ(dataJson.getDouble("diagnosisPk"));
                    eigeDataEntity.setAccelerationRmsZ(dataJson.getDouble("rmsValues"));
                    eigeDataEntity.setSpeedPeakZ(dataJson.getDouble("integratPk"));
                    eigeDataEntity.setSpeedRmsZ(dataJson.getDouble("integratRMS"));

                    // 冲击
                    eigeDataEntity.setEnvelopEnergy(dataJson.getDouble("envelopEnergy"));

                }
            }
            if (isInsert) {
                //TODO 无线数据


                list.add(eigeDataEntity);
//                commonService.commonInsert(list, factoryName);
            } else {
                log.info("get device info is null ...............");
            }

        }

    }

}
