package com.moscait.qwatcher;

import com.moscait.qwatcher.model.HostStatus;
import com.moscait.qwatcher.service.HostService;
import com.moscait.qwatcher.service.InternetMonitorService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/host")
public class HostResource {

    @Inject
    HostService hostService;

    @Inject
    InternetMonitorService monitorService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public HostStatus getStatus() {
        return new HostStatus(
                hostService.getUptime(),
                hostService.isInternetAvailable(),
                hostService.getDevices());
    }

    @GET
    @Path("/monitor")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getMonitorStatus() {
        return monitorService.getMonitorStatus();
    }

    @jakarta.ws.rs.POST
    @Path("/monitor")
    @jakarta.ws.rs.Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object updateMonitorSettings(java.util.Map<String, Object> settings) {
        String targetId = (String) settings.get("targetDeviceId");
        int threshold = Integer.parseInt(settings.get("offlineThresholdMinutes").toString());
        int delay = Integer.parseInt(settings.get("powerCycleDelayMinutes").toString());

        monitorService.updateSettings(targetId, threshold, delay);
        return monitorService.getMonitorStatus();
    }
}
