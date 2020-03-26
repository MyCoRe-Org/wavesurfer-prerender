package org.mycore.wavesurfer.prerender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

public class AudioWaveFormRunnable implements Runnable {

    public static final int TIMEOUT_IN_MINUTES = 10;

    public static final int ROUND_TO_DIGITS_AFTER_POINT = 2;

    public static final Gson GSON =  new GsonBuilder().serializeNulls().create();

    private static final Logger LOGGER = LogManager.getLogger();

    protected MCRObjectID derivateID;

    protected String relativeFilePath;

    protected Path absoluteFilePath;

    protected Path output;

    public AudioWaveFormRunnable(MCRPath file) throws IOException {
        this.derivateID = MCRObjectID.getInstance(file.getOwner());
        this.relativeFilePath = file.getOwnerRelativePath();
        this.absoluteFilePath = file.toPhysicalPath().toAbsolutePath();
    }

    public static void delete(MCRPath file) throws IOException {
        final Path audioWaveFile = getResultFilePath(MCRObjectID.getInstance(file.getOwner()),
            file.getOwnerRelativePath());
        if (Files.exists(audioWaveFile)) {
            Files.delete(audioWaveFile);
        }
    }

    public static Path getResultFilePath(MCRObjectID derivate, String ownerRelativePath) {
        return getSlotDirPath(derivate)
            .resolve(derivate.toString())
            .resolve((ownerRelativePath.startsWith("/") ?  ownerRelativePath.substring(1) :ownerRelativePath) + ".json");
    }

    private static Path getWavesurferFilePath() {
        return Paths.get(MCRConfiguration.instance().getString("MCR.datadir")).resolve("static")
            .resolve("wavesurfer");
    }

    private static Path getSlotDirPath(MCRObjectID id) {
        final String numberAsString = id.getNumberAsString();

        return getWavesurferFilePath().resolve(numberAsString.substring(0, 3))
            .resolve(numberAsString.substring(3, 6));
    }

    protected void init() {
        this.output = getWavesurferFilePath();
        try {
            Files.createDirectories(output);
        } catch (IOException e) {
            throw new MCRException("Error while creating wavesurfer direcory " + output.toAbsolutePath().toString());
        }
    }

    @Override
    public void run() {
        final Path output = getResultFilePath(derivateID, this.relativeFilePath);

        final Path foldersToCreate = output.getParent();

        try {
            Files.createDirectories(foldersToCreate);
        } catch (IOException e) {
            throw new MCRException("Error while creating directory!");
        }

        Process solrProccess = null;
        try {
            solrProccess = new ProcessBuilder("audiowaveform", "-i", absoluteFilePath.toAbsolutePath().toString(),
                "-o", output.toAbsolutePath().toString(), "--pixels-per-second", "20", "--bits", "8")
                .redirectErrorStream(true).inheritIO()
                .start();
            solrProccess.waitFor(TIMEOUT_IN_MINUTES, TimeUnit.MINUTES);
        } catch (IOException | InterruptedException e) {
            throw new MCRException("Error while executing audiowaveform!", e);
        }
        WaveSurferJSON waveSurferJSON;
        try (BufferedReader br = Files.newBufferedReader(output)) {
            waveSurferJSON = new WaveSurferJSON(GSON.fromJson(br, WaveSurferJSON.class));
        } catch (IOException e) {
            throw new MCRException("Error while reading audiowaveform result!");
        }

        fixWavesurferJSON(waveSurferJSON);
        try (BufferedWriter bw = Files
            .newBufferedWriter(output, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            GSON.toJson(waveSurferJSON, WaveSurferJSON.class, bw);
            bw.flush();
        } catch (IOException| JsonIOException e) {
            throw new MCRException("Error while writing patched audiowaveform result!", e);
        }
    }

    private void fixWavesurferJSON(WaveSurferJSON json) {
        final Double[] data = json.data;
        final Optional<Double> max = Stream.of(data).max(Double::compareTo);
        double roundFactor = Math.pow(10, ROUND_TO_DIGITS_AFTER_POINT);
        max.ifPresent((maxValue) -> {
            LOGGER.info("Fixing Wavesurfer json with max int : " + maxValue);
            for (int i = 0; i < data.length; i++) {
                final double date = data[i];
                data[i] = Math.round((date / maxValue) * roundFactor) / roundFactor;
            }
        });
    }
}
