//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package hxy.dragon.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;

public final class Streams {
    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private Streams() {
    }

    public static long copy(InputStream inputStream, OutputStream outputStream, boolean closeOutputStream) throws IOException {
        return copy(inputStream, outputStream, closeOutputStream, new byte[8192]);
    }

    public static long copy(InputStream inputStream, OutputStream outputStream, boolean closeOutputStream, byte[] buffer) throws IOException {
        OutputStream out = outputStream;
        InputStream in = inputStream;

        try {
            long total = 0L;

            while (true) {
                int res = in.read(buffer);
                if (res == -1) {
                    if (out != null) {
                        if (closeOutputStream) {
                            out.close();
                        } else {
                            out.flush();
                        }

                        out = null;
                    }

                    in.close();
                    in = null;
                    long var13 = total;
                    return var13;
                }

                if (res > 0) {
                    total += (long) res;
                    if (out != null) {
                        out.write(buffer, 0, res);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(in);
            if (closeOutputStream) {
                IOUtils.closeQuietly(out);
            }

        }
    }

    public static String asString(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(inputStream, baos, true);
        return baos.toString();
    }

    public static String asString(InputStream inputStream, String encoding) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(inputStream, baos, true);
        return baos.toString(encoding);
    }

    public static String checkFileName(String fileName) {
        if (fileName != null && fileName.indexOf(0) != -1) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < fileName.length(); ++i) {
                char c = fileName.charAt(i);
                switch (c) {
                    case '\u0000':
                        sb.append("\\0");
                        break;
                    default:
                        sb.append(c);
                }
            }

            throw new RuntimeException(fileName + "Invalid file name: " + sb);
        } else {
            return fileName;
        }
    }
}
