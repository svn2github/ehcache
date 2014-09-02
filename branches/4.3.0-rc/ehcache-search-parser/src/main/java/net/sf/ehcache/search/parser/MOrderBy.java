/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.ehcache.search.parser;

public class MOrderBy {

    /**
     * The attr.
     */
    private final MAttribute attr;

    /**
     * The asc.
     */
    private final boolean asc;

    /**
     * Instantiates a new model order by.
     *
     * @param attr the attr
     * @param asc  the asc
     */
    public MOrderBy(MAttribute attr, boolean asc) {
        this.attr = attr;
        this.asc = asc;
    }

    /**
     * Gets the attribute.
     *
     * @return the attribute
     */
    public MAttribute getAttribute() {
        return attr;
    }

    /**
     * Checks if is order ascending.
     *
     * @return true, if is order ascending
     */
    public boolean isOrderAscending() {
        return asc;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "order by " + attr + (asc ? " ascending" : " descending");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (asc ? 1231 : 1237);
        result = prime * result + ((attr == null) ? 0 : attr.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MOrderBy other = (MOrderBy)obj;
        if (asc != other.asc) return false;
        if (attr == null) {
            if (other.attr != null) return false;
        } else if (!attr.equals(other.attr)) return false;
        return true;
    }

}
