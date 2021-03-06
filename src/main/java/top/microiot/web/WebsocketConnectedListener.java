package top.microiot.web;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import top.microiot.domain.Alarm;
import top.microiot.domain.AlarmType;
import top.microiot.domain.Device;
import top.microiot.repository.AlarmRepository;
import top.microiot.repository.DeviceRepository;
import top.microiot.security.CustomUserDetails;

@Component
public class WebsocketConnectedListener implements ApplicationListener<SessionConnectedEvent> {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private DeviceRepository deviceRepository;
	@Autowired
	private AlarmRepository alarmRepository;
	@Autowired
	private SimpMessagingTemplate template;
	
	@Override
	public void onApplicationEvent(SessionConnectedEvent event) {
		SessionConnectedEvent e = (SessionConnectedEvent)event;
		Date date = new Date(e.getTimestamp());
		
		if((Authentication)e.getUser() != null) {
			CustomUserDetails user = (CustomUserDetails) ((Authentication)e.getUser()).getPrincipal();
			if (user.isDevice()){
				Device device = deviceRepository.findByDeviceAccountUsername(user.getUsername());
				logger.debug("connected device is " + user.getUsername() + " at " + date);
				Alarm last = alarmRepository.queryLastAlarm(device.getId());
				if(last == null || (last != null && last.getReportTime().before(date))) {
					if(!device.isConnected()){
						device.setConnected(true);
						deviceRepository.save(device);
					}
					Alarm alarm = new Alarm(device, AlarmType.CONNECTED_ALARM, null, date);
					alarm = alarmRepository.save(alarm);
					String destination = "/topic/alarm." + device.getId();
					template.convertAndSend(destination, alarm);
				}
			}
		}
	}

}
