package org.mycore.wavesurfer.prerender;

import com.google.gson.annotations.SerializedName;

public class WaveSurferJSON {
    public Integer version;

    public Integer channels;

    @SerializedName("sample_rate")
    public Integer sample_rate;

    @SerializedName("samples_per_pixel")
    public Integer samples_per_pixel;

    public Integer bits;

    public Integer length;

    @SerializedName("data")
    public Double[] data;

    public WaveSurferJSON(Integer version, Integer channels, Integer sample_rate, Integer samples_per_pixel,
        Integer bits, Integer length, Double[] data) {
        this.version = version;
        this.channels = channels;
        this.sample_rate = sample_rate;
        this.samples_per_pixel = samples_per_pixel;
        this.bits = bits;
        this.length = length;
        this.data = data;
    }

    public WaveSurferJSON(WaveSurferJSON wsj) {
        this(wsj.version, wsj.channels, wsj.sample_rate, wsj.samples_per_pixel, wsj.bits, wsj.length, wsj.data);
    }
}
