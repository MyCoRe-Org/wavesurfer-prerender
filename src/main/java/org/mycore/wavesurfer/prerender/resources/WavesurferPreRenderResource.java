package org.mycore.wavesurfer.prerender.resources;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.wavesurfer.prerender.AudioWaveFormRunnable;

@javax.ws.rs.Path("/wavesurfer/{derivate}/{filePath}.json")
public class WavesurferPreRenderResource {

    @GET
    public Response getWPR(@PathParam("derivate") String derivate, @PathParam("filePath") String filePath) {
        final MCRPath path = MCRPath.getPath(derivate, filePath);
        if (Files.exists(path)) {
            final Path resultFilePath = AudioWaveFormRunnable
                .getResultFilePath(MCRObjectID.getInstance(path.getOwner()), path.getOwnerRelativePath());

            return Response.ok()
                .entity((StreamingOutput) (OutputStream os) -> {
                    Files.copy(resultFilePath, os);
                }).type("application/json").build();

        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
