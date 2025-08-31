/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.schabi.newpipe.extractor.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import static org.schabi.newpipe.extractor.utils.Utils.ISO_8859_1;

/**
 * This class consists exclusively of static methods for obtaining
 * encoders and decoders for the Base64 encoding scheme. The
 * implementation of this class supports the following types of Base64
 * as specified in
 * <a href="http://www.ietf.org/rfc/rfc4648.txt">RFC 4648</a> and
 * <a href="http://www.ietf.org/rfc/rfc2045.txt">RFC 2045</a>.
 *
 * <ul>
 * <li><a id="basic"><b>Basic</b></a>
 * <p> Uses "The Base64 Alphabet" as specified in Table 1 of
 *     RFC 4648 and RFC 2045 for encoding and decoding operation.
 *     The encoder does not add any line feed (line separator)
 *     character. The decoder rejects data that contains characters
 *     outside the base64 alphabet.</p></li>
 *
 * <li><a id="url"><b>URL and Filename safe</b></a>
 * <p> Uses the "URL and Filename safe Base64 Alphabet" as specified
 *     in Table 2 of RFC 4648 for encoding and decoding. The
 *     encoder does not add any line feed (line separator) character.
 *     The decoder rejects data that contains characters outside the
 *     base64 alphabet.</p></li>
 *
 * <li><a id="mime"><b>MIME</b></a>
 * <p> Uses the "The Base64 Alphabet" as specified in Table 1 of
 *     RFC 2045 for encoding and decoding operation. The encoded output
 *     must be represented in lines of no more than 76 characters each
 *     and uses a carriage return {@code '\r'} followed immediately by
 *     a linefeed {@code '\n'} as the line separator. No line separator
 *     is added to the end of the encoded output. All line separators
 *     or other characters not found in the base64 alphabet table are
 *     ignored in decoding operation.</p></li>
 * </ul>
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a
 * method of this class will cause a {@link java.lang.NullPointerException
 * NullPointerException} to be thrown.
 *
 * @author  Xueming Shen
 * @since   1.8
 */

public final class Base64 {

    private Base64() {
    }


    /**
     * Returns a {@link Encoder} that encodes using the
     * <a href="#url">URL and Filename safe</a> type base64
     * encoding scheme.
     *
     * @return  A Base64 encoder.
     */
    public static Encoder getUrlEncoder() {
        return Encoder.RFC4648_URLSAFE;
    }

    /**
     * Returns a {@link Decoder} that decodes using the
     * <a href="#url">URL and Filename safe</a> type base64
     * encoding scheme.
     *
     * @return  A Base64 decoder.
     */
    public static Decoder getUrlDecoder() {
         return Decoder.RFC4648_URLSAFE;
    }

    /**
     * This class implements an encoder for encoding byte data using
     * the Base64 encoding scheme as specified in RFC 4648 and RFC 2045.
     *
     * <p> Instances of {@link Encoder} class are safe for use by
     * multiple concurrent threads.
     *
     * <p> Unless otherwise noted, passing a {@code null} argument to
     * a method of this class will cause a
     * {@link java.lang.NullPointerException NullPointerException} to
     * be thrown.
     *
     * @since   1.8
     */
    public static final class Encoder {

        private final byte[] newline;
        private final int linemax;
        private final boolean isURL;
        private final boolean doPadding;

        private Encoder(final boolean isURL, final byte[] newline, final int linemax,
                        final boolean doPadding) {
            this.isURL = isURL;
            this.newline = newline;
            this.linemax = linemax;
            this.doPadding = doPadding;
        }

        /**
         * This array is a lookup table that translates 6-bit positive integer
         * index values into their "Base64 Alphabet" equivalents as specified
         * in "Table 1: The Base64 Alphabet" of RFC 2045 (and RFC 4648).
         */
        private static final char[] toBase64 = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
        };

        /**
         * It's the lookup table for "URL and Filename safe Base64" as specified
         * in Table 2 of the RFC 4648, with the '+' and '/' changed to '-' and
         * '_'. This table is used when BASE64_URL is specified.
         */
        private static final char[] toBase64URL = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'
        };

        static final Encoder RFC4648_URLSAFE = new Encoder(true, null, -1, true);

        private int outLength(final int srclen) {
            int len = 0;
            if (doPadding) {
                len = 4 * ((srclen + 2) / 3);
            } else {
                final int n = srclen % 3;
                len = 4 * (srclen / 3) + (n == 0 ? 0 : n + 1);
            }
            if (linemax > 0) { // line separators
                len += (len - 1) / linemax * newline.length;
            }
            return len;
        }

        /**
         * Encodes all bytes from the specified byte array into a newly-allocated
         * byte array using the {@link Base64} encoding scheme. The returned byte
         * array is of the length of the resulting bytes.
         *
         * @param src the byte array to encode
         * @return A newly-allocated byte array containing the resulting
         * encoded bytes.
         */
        private byte[] encode(final byte[] src) {
            final int len = outLength(src.length);          // dst array size
            final byte[] dst = new byte[len];
            final int ret = encode0(src, 0, src.length, dst);
            if (ret != dst.length) {
                return Arrays.copyOf(dst, ret);
            }
            return dst;
        }

        /**
         * Encodes the specified byte array into a String using the {@link Base64}
         * encoding scheme.
         *
         * <p> This method first encodes all input bytes into a base64 encoded
         * byte array and then constructs a new String by using the encoded byte
         * array and the {@link java.nio.charset.StandardCharsets#ISO_8859_1
         * ISO-8859-1} charset.
         *
         * <p> In other words, an invocation of this method has exactly the same
         * effect as invoking
         * {@code new String(encode(src), StandardCharsets.ISO_8859_1)}.
         *
         * @param src the byte array to encode
         * @return A String containing the resulting Base64 encoded characters
         */
        @SuppressWarnings("deprecation")
        public String encodeToString(final byte[] src) {
            final byte[] encoded = encode(src);
            return new String(encoded, 0, 0, encoded.length);
        }

        private int encode0(final byte[] src, final int off, final int end, final byte[] dst) {
            char[] base64 = isURL ? toBase64URL : toBase64;
            int sp = off;
            int slen = (end - off) / 3 * 3;
            final int sl = off + slen;
            if (linemax > 0 && slen > linemax / 4 * 3) {
                slen = linemax / 4 * 3;
            }
            int dp = 0;
            while (sp < sl) {
                final int sl0 = Math.min(sp + slen, sl);
                for (int sp0 = sp, dp0 = dp; sp0 < sl0;) {
                    final int bits = (src[sp0++] & 0xff) << 16
                            | (src[sp0++] & 0xff) << 8
                            | (src[sp0++] & 0xff);
                    dst[dp0++] = (byte) base64[(bits >>> 18) & 0x3f];
                    dst[dp0++] = (byte) base64[(bits >>> 12) & 0x3f];
                    dst[dp0++] = (byte) base64[(bits >>> 6) & 0x3f];
                    dst[dp0++] = (byte) base64[bits & 0x3f];
                }
                final int dlen = (sl0 - sp) / 3 * 4;
                dp += dlen;
                sp = sl0;
                if (dlen == linemax && sp < end) {
                    for (final byte b : newline) {
                        dst[dp++] = b;
                    }
                }
            }
            if (sp < end) {               // 1 or 2 leftover bytes
                final int b0 = src[sp++] & 0xff;
                dst[dp++] = (byte) base64[b0 >> 2];
                if (sp == end) {
                    dst[dp++] = (byte) base64[(b0 << 4) & 0x3f];
                    if (doPadding) {
                        dst[dp++] = '=';
                        dst[dp++] = '=';
                    }
                } else {
                    final int b1 = src[sp++] & 0xff;
                    dst[dp++] = (byte) base64[(b0 << 4) & 0x3f | (b1 >> 4)];
                    dst[dp++] = (byte) base64[(b1 << 2) & 0x3f];
                    if (doPadding) {
                        dst[dp++] = '=';
                    }
                }
            }
            return dp;
        }
    }

    /**
     * This class implements a decoder for decoding byte data using the
     * Base64 encoding scheme as specified in RFC 4648 and RFC 2045.
     *
     * <p> The Base64 padding character {@code '='} is accepted and
     * interpreted as the end of the encoded byte data, but is not
     * required. So if the final unit of the encoded byte data only has
     * two or three Base64 characters (without the corresponding padding
     * character(s) padded), they are decoded as if followed by padding
     * character(s). If there is a padding character present in the
     * final unit, the correct number of padding character(s) must be
     * present, otherwise {@code IllegalArgumentException} (
     * {@code IOException} when reading from a Base64 stream) is thrown
     * during decoding.
     *
     * <p> Instances of {@link Decoder} class are safe for use by
     * multiple concurrent threads.
     *
     * <p> Unless otherwise noted, passing a {@code null} argument to
     * a method of this class will cause a
     * {@link java.lang.NullPointerException NullPointerException} to
     * be thrown.
     * <p> If the decoded byte output of the needed size can not
     *     be allocated, the decode methods of this class will
     *     cause an {@link java.lang.OutOfMemoryError OutOfMemoryError}
     *     to be thrown.
     *
     * @see     Encoder
     * @since   1.8
     */
    public static class Decoder {

        private final boolean isURL;
        private final boolean isMIME;

        private Decoder(boolean isURL, boolean isMIME) {
            this.isURL = isURL;
            this.isMIME = isMIME;
        }

        /**
         * Lookup table for decoding unicode characters drawn from the
         * "Base64 Alphabet" (as specified in Table 1 of RFC 2045) into
         * their 6-bit positive integer equivalents.  Characters that
         * are not in the Base64 alphabet but fall within the bounds of
         * the array are encoded to -1.
         *
         */
        private static final int[] fromBase64 = new int[256];
        static {
            Arrays.fill(fromBase64, -1);
            for (int i = 0; i < Encoder.toBase64.length; i++)
                fromBase64[Encoder.toBase64[i]] = i;
            fromBase64['='] = -2;
        }

        /**
         * Lookup table for decoding "URL and Filename safe Base64 Alphabet"
         * as specified in Table2 of the RFC 4648.
         */
        private static final int[] fromBase64URL = new int[256];

        static {
            Arrays.fill(fromBase64URL, -1);
            for (int i = 0; i < Encoder.toBase64URL.length; i++)
                fromBase64URL[Encoder.toBase64URL[i]] = i;
            fromBase64URL['='] = -2;
        }

        static final Decoder RFC4648_URLSAFE = new Decoder(true, false);

        /**
         * Decodes all bytes from the input byte array using the {@link Base64}
         * encoding scheme, writing the results into a newly-allocated output
         * byte array. The returned byte array is of the length of the resulting
         * bytes.
         *
         * @param   src
         *          the byte array to decode
         *
         * @return  A newly-allocated byte array containing the decoded bytes.
         *
         * @throws  IllegalArgumentException
         *          if {@code src} is not in valid Base64 scheme
         */
        public byte[] decode(byte[] src) {
            byte[] dst = new byte[decodedOutLength(src, 0, src.length)];
            int ret = decode0(src, 0, src.length, dst);
            if (ret != dst.length) {
                dst = Arrays.copyOf(dst, ret);
            }
            return dst;
        }

        /**
         * Decodes a Base64 encoded String into a newly-allocated byte array
         * using the {@link Base64} encoding scheme.
         *
         * <p> An invocation of this method has exactly the same effect as invoking
         * {@code decode(src.getBytes(StandardCharsets.ISO_8859_1))}
         *
         * @param   src
         *          the string to decode
         *
         * @return  A newly-allocated byte array containing the decoded bytes.
         *
         * @throws  IllegalArgumentException
         *          if {@code src} is not in valid Base64 scheme
         */
        public byte[] decode(String src) {
            // Android-changed: keep Java 8 implementation.
            // return decode(src.getBytes(ISO_8859_1.INSTANCE));
            try {
                return decode(src.getBytes(ISO_8859_1));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException("Unable to decode: "+ src+", err:"+ e.getMessage(), e);
            }
        }

        /**
         * Decodes all bytes from the input byte array using the {@link Base64}
         * encoding scheme, writing the results into the given output byte array,
         * starting at offset 0.
         *
         * <p> It is the responsibility of the invoker of this method to make
         * sure the output byte array {@code dst} has enough space for decoding
         * all bytes from the input byte array. No bytes will be written to
         * the output byte array if the output byte array is not big enough.
         *
         * <p> If the input byte array is not in valid Base64 encoding scheme
         * then some bytes may have been written to the output byte array before
         * IllegalargumentException is thrown.
         *
         * @param   src
         *          the byte array to decode
         * @param   dst
         *          the output byte array
         *
         * @return  The number of bytes written to the output byte array
         *
         * @throws  IllegalArgumentException
         *          if {@code src} is not in valid Base64 scheme, or {@code dst}
         *          does not have enough space for decoding all input bytes.
         */
        public int decode(byte[] src, byte[] dst) {
            int len = decodedOutLength(src, 0, src.length);
            if (dst.length < len || len == -1)
                throw new IllegalArgumentException(
                    "Output byte array is too small for decoding all input bytes");
            return decode0(src, 0, src.length, dst);
        }

        /**
         * Decodes all bytes from the input byte buffer using the {@link Base64}
         * encoding scheme, writing the results into a newly-allocated ByteBuffer.
         *
         * <p> Upon return, the source buffer's position will be updated to
         * its limit; its limit will not have been changed. The returned
         * output buffer's position will be zero and its limit will be the
         * number of resulting decoded bytes
         *
         * <p> {@code IllegalArgumentException} is thrown if the input buffer
         * is not in valid Base64 encoding scheme. The position of the input
         * buffer will not be advanced in this case.
         *
         * @param   buffer
         *          the ByteBuffer to decode
         *
         * @return  A newly-allocated byte buffer containing the decoded bytes
         *
         * @throws  IllegalArgumentException
         *          if {@code buffer} is not in valid Base64 scheme
         */
        public ByteBuffer decode(ByteBuffer buffer) {
            int pos0 = buffer.position();
            try {
                byte[] src;
                int sp, sl;
                if (buffer.hasArray()) {
                    src = buffer.array();
                    sp = buffer.arrayOffset() + buffer.position();
                    sl = buffer.arrayOffset() + buffer.limit();
                    buffer.position(buffer.limit());
                } else {
                    src = new byte[buffer.remaining()];
                    buffer.get(src);
                    sp = 0;
                    sl = src.length;
                }
                byte[] dst = new byte[decodedOutLength(src, sp, sl)];
                return ByteBuffer.wrap(dst, 0, decode0(src, sp, sl, dst));
            } catch (IllegalArgumentException iae) {
                buffer.position(pos0);
                throw iae;
            }
        }

        /**
         * Returns an input stream for decoding {@link Base64} encoded byte stream.
         *
         * <p> The {@code read}  methods of the returned {@code InputStream} will
         * throw {@code IOException} when reading bytes that cannot be decoded.
         *
         * <p> Closing the returned input stream will close the underlying
         * input stream.
         *
         * @param   is
         *          the input stream
         *
         * @return  the input stream for decoding the specified Base64 encoded
         *          byte stream
         */
        public InputStream wrap(InputStream is) {
            Objects.requireNonNull(is);
            return new DecInputStream(is, isURL ? fromBase64URL : fromBase64, isMIME);
        }

        /**
         * Calculates the length of the decoded output bytes.
         *
         * @param src the byte array to decode
         * @param sp the source  position
         * @param sl the source limit
         *
         * @return length of the decoded bytes
         *
         */
        private int decodedOutLength(byte[] src, int sp, int sl) {
            int[] base64 = isURL ? fromBase64URL : fromBase64;
            int paddings = 0;
            int len = sl - sp;
            if (len == 0)
                return 0;
            if (len < 2) {
                if (isMIME && base64[0] == -1)
                    return 0;
                throw new IllegalArgumentException(
                    "Input byte[] should at least have 2 bytes for base64 bytes");
            }
            if (isMIME) {
                // scan all bytes to fill out all non-alphabet. a performance
                // trade-off of pre-scan or Arrays.copyOf
                int n = 0;
                while (sp < sl) {
                    int b = src[sp++] & 0xff;
                    if (b == '=') {
                        len -= (sl - sp + 1);
                        break;
                    }
                    if ((b = base64[b]) == -1)
                        n++;
                }
                len -= n;
            } else {
                if (src[sl - 1] == '=') {
                    paddings++;
                    if (src[sl - 2] == '=')
                        paddings++;
                }
            }
            if (paddings == 0 && (len & 0x3) !=  0)
                paddings = 4 - (len & 0x3);

            // If len is near to Integer.MAX_VALUE, (len + 3)
            // can possibly overflow, perform this operation as
            // long and cast it back to integer when the value comes under
            // integer limit. The final value will always be in integer
            // limits
            return 3 * (int) ((len + 3L) / 4) - paddings;
        }

        /**
         * Decodes base64 characters, and returns the number of data bytes
         * written into the destination array.
         *
         * It is the fast path for full 4-byte to 3-byte decoding w/o errors.
         *
         * decodeBlock() can be overridden by an arch-specific intrinsic.
         * decodeBlock can choose to decode all, none, or a variable-sized
         * prefix of the src bytes.  This allows the intrinsic to decode in
         * chunks of the src that are of a favorable size for the specific
         * processor it's running on.
         *
         * If any illegal base64 bytes are encountered in src by the
         * intrinsic, the intrinsic must return the actual number of valid
         * data bytes already written to dst.  Note that the '=' pad
         * character is treated as an illegal Base64 character by
         * decodeBlock, so it will not process a block of 4 bytes
         * containing pad characters.  However, MIME decoding ignores
         * illegal characters, so any intrinsic overriding decodeBlock
         * can choose how to handle illegal characters based on the isMIME
         * parameter.
         *
         * Given the parameters, no length check is possible on dst, so dst
         * is assumed to be large enough to store the decoded bytes.
         *
         * @param  src
         *         the source byte array of Base64 encoded bytes
         * @param  sp
         *         the offset into src array to begin reading
         * @param  sl
         *         the offset (exclusive) past the last byte to be converted.
         * @param  dst
         *         the destination byte array of decoded data bytes
         * @param  dp
         *         the offset into dst array to begin writing
         * @param  isURL
         *         boolean, when true decode RFC4648 URL-safe base64 characters
         * @param  isMIME
         *         boolean, when true decode according to RFC2045 (ignore illegal chars)
         * @return the number of destination data bytes produced
         */
        private int decodeBlock(byte[] src, int sp, int sl, byte[] dst, int dp, boolean isURL, boolean isMIME) {
            int[] base64 = isURL ? fromBase64URL : fromBase64;
            int sl0 = sp + ((sl - sp) & ~0b11);
            int new_dp = dp;
            while (sp < sl0) {
                int b1 = base64[src[sp++] & 0xff];
                int b2 = base64[src[sp++] & 0xff];
                int b3 = base64[src[sp++] & 0xff];
                int b4 = base64[src[sp++] & 0xff];
                if ((b1 | b2 | b3 | b4) < 0) {    // non base64 byte
                    return new_dp - dp;
                }
                int bits0 = b1 << 18 | b2 << 12 | b3 << 6 | b4;
                dst[new_dp++] = (byte)(bits0 >> 16);
                dst[new_dp++] = (byte)(bits0 >>  8);
                dst[new_dp++] = (byte)(bits0);
            }
            return new_dp - dp;
        }

        private int decode0(byte[] src, int sp, int sl, byte[] dst) {
            int[] base64 = isURL ? fromBase64URL : fromBase64;
            int dp = 0;
            int bits = 0;
            int shiftto = 18;       // pos of first byte of 4-byte atom

            while (sp < sl) {
                if (shiftto == 18 && sp < sl - 4) {       // fast path
                    int dl = decodeBlock(src, sp, sl, dst, dp, isURL, isMIME);
                    /*
                     * Calculate how many characters were processed by how many
                     * bytes of data were returned.
                     */
                    int chars_decoded = ((dl + 2) / 3) * 4;

                    sp += chars_decoded;
                    dp += dl;
                }
                if (sp >= sl) {
                    // we're done
                    break;
                }
                int b = src[sp++] & 0xff;
                if ((b = base64[b]) < 0) {
                    if (b == -2) {         // padding byte '='
                        // =     shiftto==18 unnecessary padding
                        // x=    shiftto==12 a dangling single x
                        // x     to be handled together with non-padding case
                        // xx=   shiftto==6&&sp==sl missing last =
                        // xx=y  shiftto==6 last is not =
                        if (shiftto == 6 && (sp == sl || src[sp++] != '=') ||
                            shiftto == 18) {
                            throw new IllegalArgumentException(
                                "Input byte array has wrong 4-byte ending unit");
                        }
                        break;
                    }
                    if (isMIME)    // skip if for rfc2045
                        continue;
                    else
                        throw new IllegalArgumentException(
                            "Illegal base64 character " +
                            Integer.toString(src[sp - 1], 16));
                }
                bits |= (b << shiftto);
                shiftto -= 6;
                if (shiftto < 0) {
                    dst[dp++] = (byte)(bits >> 16);
                    dst[dp++] = (byte)(bits >>  8);
                    dst[dp++] = (byte)(bits);
                    shiftto = 18;
                    bits = 0;
                }
            }
            // reached end of byte array or hit padding '=' characters.
            if (shiftto == 6) {
                dst[dp++] = (byte)(bits >> 16);
            } else if (shiftto == 0) {
                dst[dp++] = (byte)(bits >> 16);
                dst[dp++] = (byte)(bits >>  8);
            } else if (shiftto == 12) {
                // dangling single "x", incorrectly encoded.
                throw new IllegalArgumentException(
                    "Last unit does not have enough valid bits");
            }
            // anything left is invalid, if is not MIME.
            // if MIME, ignore all non-base64 character
            while (sp < sl) {
                if (isMIME && base64[src[sp++] & 0xff] < 0)
                    continue;
                throw new IllegalArgumentException(
                    "Input byte array has incorrect ending byte at " + sp);
            }
            return dp;
        }
    }

    /*
     * An input stream for decoding Base64 bytes
     */
    private static class DecInputStream extends InputStream {

        private final InputStream is;
        private final boolean isMIME;
        private final int[] base64;     // base64 -> byte mapping
        private int bits = 0;           // 24-bit buffer for decoding

        /* writing bit pos inside bits; one of 24 (left, msb), 18, 12, 6, 0 */
        private int wpos = 0;

        /* reading bit pos inside bits: one of 24 (left, msb), 16, 8, 0 */
        private int rpos = 0;

        private boolean eof = false;
        private boolean closed = false;

        DecInputStream(InputStream is, int[] base64, boolean isMIME) {
            this.is = is;
            this.base64 = base64;
            this.isMIME = isMIME;
        }

        private byte[] sbBuf = new byte[1];

        @Override
        public int read() throws IOException {
            return read(sbBuf, 0, 1) == -1 ? -1 : sbBuf[0] & 0xff;
        }

        private int leftovers(byte[] b, int off, int pos, int limit) {
            eof = true;

            /*
             * We use a loop here, as this method is executed only a few times.
             * Unrolling the loop would probably not contribute much here.
             */
            while (rpos - 8 >= wpos && pos != limit) {
                rpos -= 8;
                b[pos++] = (byte) (bits >> rpos);
            }
            return pos - off != 0 || rpos - 8 >= wpos ? pos - off : -1;
        }

        private int eof(byte[] b, int off, int pos, int limit) throws IOException {
            /*
             * pos != limit
             *
             * wpos == 18: x     dangling single x, invalid unit
             * accept ending xx or xxx without padding characters
             */
            if (wpos == 18) {
                throw new IOException("Base64 stream has one un-decoded dangling byte.");
            }
            rpos = 24;
            return leftovers(b, off, pos, limit);
        }

        private int padding(byte[] b, int off, int pos, int limit) throws IOException {
            /*
             * pos != limit
             *
             * wpos == 24: =    (unnecessary padding)
             * wpos == 18: x=   (dangling single x, invalid unit)
             * wpos == 12 and missing last '=': xx=  (invalid padding)
             * wpos == 12 and last is not '=': xx=x (invalid padding)
             */
            if (wpos >= 18 || wpos == 12 && is.read() != '=') {
                throw new IOException("Illegal base64 ending sequence:" + wpos);
            }
            rpos = 24;
            return leftovers(b, off, pos, limit);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (closed) {
                throw new IOException("Stream is closed");
            }
            Objects.checkFromIndexSize(off, len, b.length);
            if (len == 0) {
                return 0;
            }

            /*
             * Rather than keeping 2 running vars (e.g., off and len),
             * we only keep one (pos), while definitely fixing the boundaries
             * of the range [off, limit).
             * More specifically, each use of pos as an index in b meets
             *      pos - off >= 0 & limit - pos > 0
             *
             * Note that limit can overflow to Integer.MIN_VALUE. However,
             * as long as comparisons with pos are as coded, there's no harm.
             */
            int pos = off;
            final int limit = off + len;
            if (eof) {
                return leftovers(b, off, pos, limit);
            }

            /*
             * Leftovers from previous invocation; here, wpos = 0.
             * There can be at most 2 leftover bytes (rpos <= 16).
             * Further, b has at least one free place.
             *
             * The logic could be coded as a loop, (as in method leftovers())
             * but the explicit "unrolling" makes it possible to generate
             * better byte extraction code.
             */
            if (rpos == 16) {
                b[pos++] = (byte) (bits >> 8);
                rpos = 8;
                if (pos == limit) {
                    return len;
                }
            }
            if (rpos == 8) {
                b[pos++] = (byte) bits;
                rpos = 0;
                if (pos == limit) {
                    return len;
                }
            }

            bits = 0;
            wpos = 24;
            for (;;) {
                /* pos != limit & rpos == 0 */
                final int i = is.read();
                if (i < 0) {
                    return eof(b, off, pos, limit);
                }
                final int v = base64[i];
                if (v < 0) {
                    /*
                     * i not in alphabet, thus
                     *      v == -2: i is '=', the padding
                     *      v == -1: i is something else, typically CR or LF
                     */
                    if (v == -1) {
                        if (isMIME) {
                            continue;
                        }
                        throw new IOException("Illegal base64 character 0x" +
                                Integer.toHexString(i));
                    }
                    return padding(b, off, pos, limit);
                }
                wpos -= 6;
                bits |= v << wpos;
                if (wpos != 0) {
                    continue;
                }
                if (limit - pos >= 3) {
                    /* frequently taken fast path, no need to track rpos */
                    b[pos++] = (byte) (bits >> 16);
                    b[pos++] = (byte) (bits >> 8);
                    b[pos++] = (byte) bits;
                    bits = 0;
                    wpos = 24;
                    if (pos == limit) {
                        return len;
                    }
                    continue;
                }

                /* b has either 1 or 2 free places */
                b[pos++] = (byte) (bits >> 16);
                if (pos == limit) {
                    rpos = 16;
                    return len;
                }
                b[pos++] = (byte) (bits >> 8);
                /* pos == limit, no need for an if */
                rpos = 8;
                return len;
            }
        }

        @Override
        public int available() throws IOException {
            if (closed)
                throw new IOException("Stream is closed");
            return is.available();   // TBD:
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                is.close();
            }
        }
    }
}
