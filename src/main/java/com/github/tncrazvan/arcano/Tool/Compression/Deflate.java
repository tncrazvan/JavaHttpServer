package com.github.tncrazvan.arcano.Tool.Compression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;

/**
 *
 * @author Administrator
 */
public interface Deflate {
    static byte[] deflate(final byte[] input) throws IOException{
        if(input == null || input.length == 0)
            return input;
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
            dos.write(input);
            dos.flush();
        }
        return  baos.toByteArray();
    }
}