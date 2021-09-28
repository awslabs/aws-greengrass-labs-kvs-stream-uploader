package com.aws.greengrass.sitewatch.videouploader.mkv;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * It's a subclass of {@link MkvRawElement}. It represents a parent MKV element. Its data field is consisted of other
 * {@link MkvParentRawElement} and {@link MkvDataRawElement}.
 */
public class MkvParentRawElement extends MkvRawElement {

    private final ArrayList<MkvRawElement> children = new ArrayList<>();

    /**
     * The constructor.
     *
     * @param idAndSizeByteBuffer The ID raw data that used to initialize ID field.
     */
    public MkvParentRawElement(ByteBuffer idAndSizeByteBuffer) {
        super(idAndSizeByteBuffer);
    }

    /**
     * Add child element to this parent element. This operation won't update it's length field.
     *
     * @param element The child element to be added.
     */
    public void addRawElement(MkvRawElement element) {
        children.add(element);
        element.setParent(this);
    }

    /**
     * Flatten all its children elements in DFS order.
     *
     * @return The {@link MkvRawElement} ArrayList
     */
    public ArrayList<MkvRawElement> getFlattenedElements() {
        final ArrayList<MkvRawElement> flattenedElements = new ArrayList<>();
        flattenedElements.add(this);
        for (MkvRawElement child : children) {
            if (child instanceof MkvParentRawElement) {
                MkvParentRawElement parent = (MkvParentRawElement) child;
                flattenedElements.addAll(parent.getFlattenedElements());
            } else {
                flattenedElements.add(child);
            }
        }
        return flattenedElements;
    }

    /**
     * Convert this MKV raw element into byte array. It includes all its children elements.
     *
     * @return The MKV elements in byte array
     */
    @Override
    public byte[] toMkvRawData() {
        final ArrayList<byte[]> childrenRaws = new ArrayList<>();
        for (MkvRawElement element : children) {
            childrenRaws.add(element.toMkvRawData());
        }
        long dataLen = 0;
        for (byte[] childRaw : childrenRaws) {
            dataLen += childRaw.length;
        }

        final byte[] size = getMkvSize(dataLen);
        final byte[] raw = new byte[(int) (this.getIdCopy().length + size.length + dataLen)];
        final ByteBuffer buffer = ByteBuffer.wrap(raw);
        buffer.put(this.getIdCopy());
        buffer.put(size);
        for (byte[] childRaw : childrenRaws) {
            buffer.put(childRaw);
        }
        return buffer.array();
    }

    /**
     * Compare MKV element with their ID, length, and data (if available).
     *
     * @param otherObj The other MKV element to be compared
     * @return True if they are equal, false otherwise
     */
    @Override
    public boolean equals(Object otherObj) {
        if (otherObj == null) {
            return false;
        }
        if (!otherObj.getClass().equals(this.getClass())) {
            return false;
        }
        MkvParentRawElement other = (MkvParentRawElement) otherObj;
        return new EqualsBuilder()
                .append(this.getIdCopy(), other.getIdCopy())
                .isEquals();
    }

    /**
     * Return hash code of this element.
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(this.getIdCopy())
                .toHashCode();
    }

}
