package org.schabi.newpipe.extractor.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import static org.schabi.newpipe.extractor.utils.Utils.UTF_8;

public class ProtoBuilder {
    ByteArrayOutputStream byteBuffer;

    public ProtoBuilder() {
        this.byteBuffer = new ByteArrayOutputStream();
    }

    public byte[] toBytes() {
        return byteBuffer.toByteArray();
    }

    public String toUrlencodedBase64() {
        try {
            final String b64 = encodeUrl(toBytes());
            return URLEncoder.encode(b64, UTF_8);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeVarint(final long val) {
        try {
            if (val == 0) {
                byteBuffer.write(new byte[]{(byte) 0});
            } else {
                long v = val;
                while (v != 0) {
                    byte b = (byte) (v & 0x7f);
                    v >>= 7;

                    if (v != 0) {
                        b |= (byte) 0x80;
                    }
                    byteBuffer.write(new byte[]{b});
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void field(final int field, final byte wire) {
        final long fbits = ((long) field) << 3;
        final long wbits = ((long) wire) & 0x07;
        final long val = fbits | wbits;
        writeVarint(val);
    }

    public void varint(final int field, final long val) {
        field(field, (byte) 0);
        writeVarint(val);
    }

    public void string(final int field, final String string) {
        try {
            final byte[] strBts = string.getBytes(UTF_8);
            bytes(field, strBts);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void bytes(final int field, final byte[] bytes) {
        field(field, (byte) 2);
        writeVarint(bytes.length);
        try {
            byteBuffer.write(bytes);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

  private static final byte[] MAP = new byte[] {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
      'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
      'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4',
      '5', '6', '7', '8', '9', '+', '/'
  };

  private static final byte[] URL_MAP = new byte[] {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
      'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
      'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4',
      '5', '6', '7', '8', '9', '-', '_'
  };

  private static String encode(final byte[] in) {
    return encode(in, MAP);
  }

  private static String encodeUrl(final byte[] in) {
    return encode(in, URL_MAP);
  }

  private static String encode(final byte[] in, final byte[] map) {
    final int length = (in.length + 2) / 3 * 4;
    final byte[] out = new byte[length];
    int index = 0;
    final int end = in.length - in.length % 3;
    for (int i = 0; i < end; i += 3) {
      out[index++] = map[(in[i] & 0xff) >> 2];
      out[index++] = map[((in[i] & 0x03) << 4) | ((in[i + 1] & 0xff) >> 4)];
      out[index++] = map[((in[i + 1] & 0x0f) << 2) | ((in[i + 2] & 0xff) >> 6)];
      out[index++] = map[(in[i + 2] & 0x3f)];
    }
    switch (in.length % 3) {
      case 1:
        out[index++] = map[(in[end] & 0xff) >> 2];
        out[index++] = map[(in[end] & 0x03) << 4];
        out[index++] = '=';
        out[index++] = '=';
        break;
      case 2:
        out[index++] = map[(in[end] & 0xff) >> 2];
        out[index++] = map[((in[end] & 0x03) << 4) | ((in[end + 1] & 0xff) >> 4)];
        out[index++] = map[((in[end + 1] & 0x0f) << 2)];
        out[index++] = '=';
        break;
    }
    try {
      return new String(out, "US-ASCII");
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

}
