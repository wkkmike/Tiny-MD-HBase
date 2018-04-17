/*
 * Copyright 2012 Shoji Nishimura
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package tiny.mdhbase;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import java.nio.ByteBuffer;

import org.apache.hadoop.hbase.util.Bytes;

/**
 * @author shoji
 *
 */
public class Utils3D {
    private Utils3D() {

    }

    public static byte[] bitwiseZip(int x, int y, int t) {
        byte[] ret = new byte[12];
        String xstring = intToString(x);
        String ystring = intToString(y);
        String tstring = intToString(t);
        StringBuilder result = new StringBuilder();
        for(int i=0; i<96; i++){
            if(i % 3 == 0){
                result.append(xstring.charAt(i / 3));
            }
            else if(i % 3 == 1){
                result.append(ystring.charAt(i / 3));
            }
            else{
                result.append(tstring.charAt(i / 3));
            }
        }
        String resultStrirng = result.toString();
        for(int i=0; i<12; i++){
            ret[i] = Integer.valueOf(resultStrirng.substring(i*8, i*8 + 8),2).byteValue();
        }
        return ret;
    }

    public static String intToString(int number) {
        StringBuilder result = new StringBuilder();
        for(int i=31; i>=0 ; i--) {
            int mask = 1 << i;
            result.append((number & mask) != 0 ? "1" : "0");
        }
        return result.toString();
    }

    public static int[] bitwiseUnzip(byte[] bs) {
        int ret[] = new int[3];
        String binaryString = toBinary(bs);
        StringBuilder xBuilder = new StringBuilder();
        StringBuilder yBuilder = new StringBuilder();
        StringBuilder tBuilder = new StringBuilder();
        for(int i=0; i<96; i++){
            if(i%3 == 0){
                xBuilder.append(binaryString.charAt(i));
            }
            else if(i%3 == 1){
                yBuilder.append(binaryString.charAt(i));
            }
            else{
                tBuilder.append(binaryString.charAt(i));
            }
        }
        ret[0] = Integer.parseInt(xBuilder.toString(), 2);
        ret[1] = Integer.parseInt(yBuilder.toString(),2);
        ret[2] = Integer.parseInt(tBuilder.toString(), 2);
        return ret;
    }

    public static String toBinary( byte[] bytes ) {
        StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
        for(int i=0; i<Byte.SIZE * bytes.length; i++ )
            sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
        return sb.toString();
    }

    public static byte[] concat(byte[] b1, byte[] b2, byte[] b3) {
        checkNotNull(b1);
        checkNotNull(b2);
        checkNotNull(b3);

        byte[] ret = new byte[b1.length + b2.length + b3.length];
        System.arraycopy(b1, 0, ret, 0, b1.length);
        System.arraycopy(b2, 0, ret, b1.length, b2.length);
        System.arraycopy(b3, 0, ret, b1.length + b2.length, b3.length);
        return ret;
    }

    public static boolean prefixMatch(byte[] prefix, int prefixSize, byte[] target) {
        byte[] mask = makeMask(prefixSize);
        checkArgument(prefix.length >= mask.length);
        checkArgument(target.length >= mask.length);

        for (int i = 0; i < mask.length; i++) {
            if (!(prefix[i] == (target[i] & mask[i]))) {
                return false;
            }
        }
        return true;
    }

    public static byte[] makeMask(int prefixSize) {
        checkArgument(prefixSize > 0);
        int d = (prefixSize - 1) / 8;
        int r = (prefixSize - 1) % 8;
        // mask[] = {0b10000000, 0b11000000, ..., 0b11111111}
        final byte[] mask = new byte[] { -128, -64, -32, -16, -8, -4, -2, -1 };

        byte[] ret = new byte[12];
        for (int i = 0; i < d; i++) {
            ret[i] = -1; // 0xFF
        }
        ret[d] = mask[r];
        return ret;
    }

    public static byte[] not(byte[] bs) {
        byte[] ret = new byte[bs.length];
        for (int i = 0; i < bs.length; i++) {
            ret[i] = (byte) (~bs[i]);
        }
        return ret;
    }

    public static byte[] or(byte[] b1, byte[] b2) {
        checkArgument(b1.length == b2.length);
        byte[] ret = new byte[b1.length];
        for (int i = 0; i < b1.length; i++) {
            ret[i] = (byte) (b1[i] | b2[i]);
        }
        return ret;
    }

    public static byte[] and(byte[] b1, byte[] b2) {
        checkArgument(b1.length == b2.length);
        byte[] ret = new byte[b1.length];
        for (int i = 0; i < b1.length; i++) {
            ret[i] = (byte) (b1[i] & b2[i]);
        }
        return ret;
    }

    public static byte[] makeBit(byte[] target, int pos) {
        checkArgument(pos >= 0);
        checkArgument(pos < target.length * 8);
        int d = pos / 8;
        int r = pos % 8;
        final byte[] bits = new byte[] { -128, 64, 32, 16, 8, 4, 2, 1 };

        byte[] ret = new byte[target.length];
        System.arraycopy(target, 0, ret, 0, target.length);
        ret[d] |= bits[r];
        return ret;
    }

    public static String toString(byte[] key, int prefixLength) {
        StringBuilder buf = new StringBuilder();
        int d = (prefixLength - 1) / 8;
        int r = (prefixLength - 1) % 8;

        final int[] masks = new int[] { -128, 64, 32, 16, 8, 4, 2, 1 };
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < masks.length; j++) {
                buf.append((key[i] & masks[j]) == 0 ? "0" : "1");
            }
        }
        for (int j = 0; j <= r; j++) {
            buf.append((key[d] & masks[j]) == 0 ? "0" : "1");
        }
        for (int j = r + 1; j < masks.length; j++) {
            buf.append("*");
        }
        for (int i = d + 1; i < key.length; i++) {
            buf.append("********");
        }
        return buf.toString();
    }
}
