package org.mycore.wavesurfer.prerender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.events.MCREvent;
import org.mycore.common.events.MCREventHandlerBase;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.niofs.MCRContentTypes;
import org.mycore.datamodel.niofs.MCRPath;

public class AudioWaveFormEventHandler extends MCREventHandlerBase {

    public static final LinkedBlockingQueue<Runnable> WORK_QUEUE = new LinkedBlockingQueue<>();

    final ThreadPoolExecutor tpe = new ThreadPoolExecutor(5, 10, 10, TimeUnit.MINUTES, WORK_QUEUE);

    @Override
    protected void handlePathUpdated(MCREvent evt, Path path, BasicFileAttributes attrs) {
        handlePathDeleted(evt, path, attrs);

        final String mimeType;
        try {
            mimeType = MCRContentTypes.probeContentType(path);
        } catch (IOException e) {
            throw new MCRException("Error while detecting MIME-Type", e);
        }
        final List<String> allowedTypes = MCRConfiguration.instance().getStrings("MCR.Wavesurfer.MimeType");
        if (allowedTypes.contains(mimeType)) {
            try {
                tpe.submit(new AudioWaveFormRunnable((MCRPath) path));
            } catch (IOException e) {
                throw new MCRException("Error while executing!", e);
            }
        }
    }

    @Override
    protected void handlePathDeleted(MCREvent evt, Path path, BasicFileAttributes attrs) {
        try {
            AudioWaveFormRunnable.delete((MCRPath) path);
        } catch (IOException e) {
            throw new MCRException("Error while deleting old wave form file", e);
        }
    }

    @Override
    protected void handlePathRepaired(MCREvent evt, Path path, BasicFileAttributes attrs) {
        handlePathUpdated(evt, path, attrs);
    }

    @Override
    protected void handleDerivateRepaired(MCREvent evt, MCRDerivate der) {
        try (Stream<Path> ps = Files.walk(MCRPath.getPath(der.getId().toString(), "/"))) {
            final Predicate<Path> isDirectory = Files::isDirectory;
            ps.filter(isDirectory.negate()).forEach(path -> handlePathUpdated(evt, path, null));
        } catch (IOException e) {
            throw new MCRException("Error while repairing derivate!", e);
        }

    }
}
