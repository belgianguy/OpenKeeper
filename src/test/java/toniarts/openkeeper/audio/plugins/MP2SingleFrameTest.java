package toniarts.openkeeper.audio.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioKey;
import com.jme3.audio.AudioStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.audio.plugins.decoder.Context;
import toniarts.openkeeper.audio.plugins.decoder.MpxReader;
import toniarts.openkeeper.audio.plugins.decoder.UnsupportedMediaException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MP2SingleFrameTest {

    private int verificationDepth;

    @BeforeEach
    void saveDecoderContext() {
        verificationDepth = Context.getVerificationDepth();
    }

    @AfterEach
    void restoreDecoderContext() {
        Context.setVerificationDepth(verificationDepth);
    }

    @Test
    void loadsSingleFrameWithAndWithoutZeroTrailer() throws Exception {
        for (int padding = 0; padding <= 3; padding++) {
            byte[] encoded = Arrays.copyOf(silentFrame(), silentFrame().length + padding);
            AudioBuffer audio = (AudioBuffer) new MP2Loader().load(asset(encoded, false));
            assertEquals(1, audio.getChannels());
            assertEquals(22050, audio.getSampleRate());
            assertEquals(16, audio.getBitsPerSample());
            byte[] pcm = new byte[audio.getData().remaining()];
            audio.getData().duplicate().get(pcm);
            assertArrayEquals(new byte[384 * Short.BYTES], pcm);
        }
    }

    @Test
    void streamsSingleFrame() throws Exception {
        AudioStream audio = (AudioStream) new MP2Loader().load(asset(silentFrame(), true));
        try {
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            byte[] buffer = new byte[2048];
            int count;
            while ((count = audio.readSamples(buffer, 0, buffer.length)) > 0) {
                pcm.write(buffer, 0, count);
            }
            assertArrayEquals(new byte[384 * Short.BYTES], pcm.toByteArray());
        } finally {
            audio.close();
        }
    }

    @Test
    void stillLoadsMultipleFrames() throws Exception {
        byte[] frame = silentFrame();
        byte[] encoded = Arrays.copyOf(frame, frame.length * 2);
        System.arraycopy(frame, 0, encoded, frame.length, frame.length);
        AudioBuffer audio = (AudioBuffer) new MP2Loader().load(asset(encoded, false));
        assertEquals(2 * 384 * Short.BYTES, audio.getData().remaining());
    }

    @Test
    void rejectsIncompleteFramesAndNonzeroTrailers() {
        byte[] frame = silentFrame();
        byte[] partialHeader = Arrays.copyOf(frame, frame.length + 2);
        partialHeader[frame.length] = (byte) 0xff;
        partialHeader[frame.length + 1] = (byte) 0xf7;
        byte[] junkTrailer = Arrays.copyOf(frame, frame.length + 1);
        junkTrailer[frame.length] = 42;
        for (byte[] invalid : new byte[][]{
            new byte[0], Arrays.copyOf(frame, 4), Arrays.copyOf(frame, frame.length - 1),
            partialHeader, junkTrailer
        }) {
            assertThrows(UnsupportedMediaException.class, () -> {
                try (InputStream input = new BufferedInputStream(new ByteArrayInputStream(invalid))) {
                    new MpxReader().readInformation(input, true);
                }
            });
        }
    }

    private static AssetInfo asset(byte[] encoded, boolean streamed) {
        return new AssetInfo(null, new AudioKey("synthetic.mp2", streamed)) {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(encoded);
            }
        };
    }

    private static byte[] silentFrame() {
        // Synthetic MPEG-2 Layer I: 64 kbps, 22050 Hz, mono, no CRC.
        // Zero bit allocations encode silence; no original game data is used.
        int padding = 1;
        int length = (12 * 64000 / 22050 + padding) * 4;
        int header = (0x7ff << 21) | (2 << 19) | (3 << 17) | (1 << 16)
                | (4 << 12) | (padding << 9) | (3 << 6);
        byte[] frame = new byte[length];
        ByteBuffer.wrap(frame).putInt(header);
        return frame;
    }
}
