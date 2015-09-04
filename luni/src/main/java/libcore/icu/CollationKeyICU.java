/**
*******************************************************************************
* Copyright (C) 1996-2005, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*
*******************************************************************************
*/

package libcore.icu;

import java.text.CollationKey;

/**
 * A concrete implementation of the abstract java.text.CollationKey.
 */
public final class CollationKeyICU extends CollationKey {
    /**
     * The key.
     */
    private final com.ibm.icu.text.CollationKey key;

    /**
     * Cached hash value.
     */
    private int hashCode;

    public CollationKeyICU(String source, com.ibm.icu.text.CollationKey key) {
        super(source);
        this.key = key;
    }

    @Override public int compareTo(CollationKey other) {
        final com.ibm.icu.text.CollationKey otherKey;
        if (other instanceof CollationKeyICU) {
            otherKey = ((CollationKeyICU) other).key;
        } else {
            otherKey = new com.ibm.icu.text.CollationKey(other.getSourceString(),
                    other.toByteArray());
        }

        return key.compareTo(otherKey);
    }

    @Override public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof CollationKey)) {
            return false;
        }
        return compareTo((CollationKey) object) == 0;
    }

    /**
     * <p>Returns a hash code for this CollationKey. The hash value is calculated
     * on the key itself, not the String from which the key was created. Thus
     * if x and y are CollationKeys, then x.hashCode(x) == y.hashCode()
     * if x.equals(y) is true. This allows language-sensitive comparison in a
     * hash table.
     * </p>
     * @return the hash value.
     * @stable ICU 2.8
     */
    @Override public int hashCode() {
        return key.hashCode();
    }

    @Override public byte[] toByteArray() {
        return key.toByteArray();
    }
}
