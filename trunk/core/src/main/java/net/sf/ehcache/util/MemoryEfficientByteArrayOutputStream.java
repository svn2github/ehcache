package net.sf.ehcache.util;

import net.sf.ehcache.Element;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * This class is designed to minimise the number of System.arraycopy(); methods
 * required to complete.
 */
public final class MemoryEfficientByteArrayOutputStream extends ByteArrayOutputStream {

    private static int lastSize = 512;

    /**
     * Creates a new byte array output stream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param size the initial size.
     */
    public MemoryEfficientByteArrayOutputStream(int size) {
        super(size);
    }




    /**
     * Gets the bytes. Not all may be valid. Use only up to getSize()
     *
     * @return the underlying byte[]
     */
    public synchronized byte getBytes()[] {
        return buf;
    }

    /**
     * Factory method
     * @param serializable
     * @param estimatedPayloadSize
     * @return
     * @throws java.io.IOException
     */
    public static MemoryEfficientByteArrayOutputStream serialize(Serializable serializable, int estimatedPayloadSize) throws IOException {
        MemoryEfficientByteArrayOutputStream outstr = new MemoryEfficientByteArrayOutputStream(estimatedPayloadSize);
        ObjectOutputStream objstr = new ObjectOutputStream(outstr);
        objstr.writeObject(serializable);
        objstr.close();
        return outstr;
    }

    /**
     * Factory method. This method optimises memory by trying to make a better guess than the Java default
     * of 32 bytes by assuming the starting point for the serialized size will be what it was last time
     * this method was called.
     * @param serializable
     * @return
     * @throws java.io.IOException
     */
    public static MemoryEfficientByteArrayOutputStream serialize(Serializable serializable) throws IOException {
        MemoryEfficientByteArrayOutputStream outstr = new MemoryEfficientByteArrayOutputStream(lastSize);
        ObjectOutputStream objstr = new ObjectOutputStream(outstr);
        objstr.writeObject(serializable);
        objstr.close();
        lastSize = outstr.getBytes().length;
        return outstr;
    }
}
