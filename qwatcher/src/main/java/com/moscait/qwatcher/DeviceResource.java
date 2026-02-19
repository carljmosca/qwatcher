package com.moscait.qwatcher;

import com.moscait.qwatcher.service.BluetoothService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/api/devices")
public class DeviceResource {

    @Inject
    BluetoothService bluetoothService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addDevice(Map<String, String> payload) {
        String id = payload.get("id");
        String name = payload.get("name");

        if (id == null || id.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("ID is required").build();
        }

        if (name == null || name.isBlank()) {
            name = "Unknown Device";
        }

        bluetoothService.addManualDevice(id, name);
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/connect")
    @Produces(MediaType.APPLICATION_JSON)
    public Response connect(@PathParam("id") String id) {
        boolean success = bluetoothService.connectDevice(id);
        if (success) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to connect").build();
        }
    }

    @POST
    @Path("/{id}/disconnect")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disconnect(@PathParam("id") String id) {
        boolean success = bluetoothService.disconnectDevice(id);
        if (success) {
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Failed to disconnect").build();
        }
    }

    @POST
    @Path("/{id}/control")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response controlDevice(@PathParam("id") String id, Map<String, String> payload) {
        String command = payload.get("command");
        if (command == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Command required").build();
        }
        try {
            bluetoothService.controlDevice(id, command);
            return Response.ok().build();
        } catch (Exception e) {
            return Response.serverError().entity("Error: " + e.getMessage()).build();
        }
    }
}
