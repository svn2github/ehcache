package net.sf.ehcache;

import java.io.Serializable;

/**
 * A bean used to wrap byte[] values to be placed in an Element so as to preserve MIME type information.
 * <p/>
 * It is necessary to preserve the MIME Type, if known.
 * @author Greg Luck
 * @version $Id$
 */
public class MimeTypeByteArray implements Serializable {

    private String mimeType;
    private byte[] value;

    public MimeTypeByteArray() {
        //
    }

    public MimeTypeByteArray(String mimeType, byte[] value) {
        this.mimeType = mimeType;
        this.value = value;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

}
