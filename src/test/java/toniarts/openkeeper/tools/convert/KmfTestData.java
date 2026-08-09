package toniarts.openkeeper.tools.convert;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import toniarts.openkeeper.tools.convert.kmf.KmfFile;
import toniarts.openkeeper.tools.convert.kmf.Material;

public final class KmfTestData {

    private KmfTestData() {
    }

    public static Material material(int flags, float emissive, float specular, String environmentMap) {
        ByteBuffer buffer = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        putFourCc(buffer, "KMSH");
        int fileSizeOffset = buffer.position();
        buffer.putInt(0);
        buffer.putInt(17);

        putFourCc(buffer, "HEAD");
        buffer.putInt(16);
        buffer.putInt(1);
        buffer.putInt(1);

        int materialListStart = buffer.position();
        putFourCc(buffer, "MATL");
        int materialListSizeOffset = buffer.position();
        buffer.putInt(0);
        buffer.putInt(1);

        int materialStart = buffer.position();
        putFourCc(buffer, "MAT2");
        int materialSizeOffset = buffer.position();
        buffer.putInt(0);
        putString(buffer, "Test material");
        buffer.putInt(1);
        putString(buffer, "Test texture");
        buffer.putInt(flags);
        buffer.putFloat(emissive);
        buffer.putFloat(specular);
        putString(buffer, environmentMap);

        int materialEnd = buffer.position();
        buffer.putInt(materialSizeOffset, materialEnd - materialStart);
        buffer.putInt(materialListSizeOffset, materialEnd - materialListStart);
        putFourCc(buffer, "DONE");
        buffer.putInt(fileSizeOffset, buffer.position());

        return new KmfFile(Arrays.copyOf(buffer.array(), buffer.position())).getMaterials().getFirst();
    }

    private static void putFourCc(ByteBuffer buffer, String value) {
        buffer.put(value.getBytes(StandardCharsets.US_ASCII));
    }

    private static void putString(ByteBuffer buffer, String value) {
        buffer.put(value.getBytes(StandardCharsets.US_ASCII));
        buffer.put((byte) 0);
    }
}
