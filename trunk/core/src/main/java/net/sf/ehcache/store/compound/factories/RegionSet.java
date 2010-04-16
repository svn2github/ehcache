/**
 *  Copyright 2003-2010 Terracotta, Inc.
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
package net.sf.ehcache.store.compound.factories;

/**
 * Implements an AA-tree. AA tree provides all the advantages of a Red Black Tree while keeping the implementation
 * simple. For more details on AA tree, check out http://user.it.uu.se/~arnea/abs/simp.html and
 * http://en.wikipedia.org/wiki/AA_tree This source code is taken from
 * http://www.cs.fiu.edu/~weiss/dsaa_java/Code/DataStructures/ and modified slightly.
 * <p>
 * This tree implementation behaves like a set, it doesn't allow duplicate nodes.
 * <p>
 * Note:: "matching" is based on the compareTo method. This class is *NOT* thread safe. Synchronize externally if you want it to be thread
 * safe.
 * <p>
 * There are memory optimizations done to this which allows users to extend AANode and implement a couple of methods themselves thus saving
 * an extra object plus a reference for every node in this tree. But if you do so, then you either can't have different types of elements in
 * the same AATreeSet or should handle copying one over other.
 * 
 * @author Mark Allen Weiss
 */
class RegionSet {

    private static final Region NULL_NODE = new Region();

    private final long size;
    
    private Region root;
    private Region deletedNode;
    private Region lastNode;
    private Region deletedElement;


    /**
     * Construct the tree.
     */
    protected RegionSet(long size) {
        this.size = size;
        this.root = new Region(0, size - 1);
    }

    /**
     * Insert into the tree.
     * 
     * @param x the item to insert.
     */
    protected void insert(Region x) {
        this.root = insert(x, this.root);
    }

    /**
     * Remove from the tree.
     * 
     * @param x the item to remove.
     */
    protected Region remove(Region x) {
        this.deletedNode = NULL_NODE;
        this.root = remove(x, this.root);
        Region d = this.deletedElement;
        // deletedElement is set to null to free the reference,
        // deletedNode is not freed as it will endup pointing to a valid node.
        this.deletedElement = null;
        if (d == null) {
            return null;
        } else {
            return new Region(d);
        }
    }

    /**
     * Find a region of the the given size.
     */
    protected Region find(long size) {
        Region current = this.root;

        if (size > current.contiguous()) {
            throw new IllegalArgumentException("Need to grow the region set");
        } else {
            while (true) {
                if (current.size() >= size) {
                    return new Region(current.start, current.start + size - 1);
                } else if (current.left.contiguous() >= size) {
                    current = current.left;
                } else if (current.right.contiguous() >= size) {
                    current = current.right;
                } else {
                    throw new IllegalArgumentException("Couldn't find a " + size + " sized free area in " + current.dump());
                }
            }
        }        
    }

    /**
     * Find the smallest item in the tree.
     * 
     * @return the smallest item or null if empty.
     */
    protected Region findMin() {
        if (isEmpty()) {
            return null;
        }

        Region ptr = this.root;

        while (ptr.left != NULL_NODE) {
            ptr = ptr.left;
        }

        return ptr;
    }

    /**
     * Find the largest item in the tree.
     * 
     * @return the largest item or null if empty.
     */
    protected Region findMax() {
        if (isEmpty()) {
            return null;
        }

        Region ptr = this.root;

        while (ptr.right != NULL_NODE) {
            ptr = ptr.right;
        }

        return ptr;
    }

    /**
     * Find an item in the tree.
     * 
     * @param x
     *            the item to search for.
     * @return the matching item of null if not found.
     */

    protected Region find(Region x) {
        Region current = this.root;

        while (current != NULL_NODE) {
            int res = x.compareTo(current);
            if (res < 0) {
                current = current.left;
            } else if (res > 0) {
                current = current.right;
            } else {
                return current;
            }
        }
        return null;
    }

    /**
     * Make the tree logically empty.
     */
    protected void clear() {
        this.root = new Region(0, size - 1);
    }

    /**
     * Test if the tree is logically empty.
     * 
     * @return true if empty, false otherwise.
     */
    protected boolean isEmpty() {
        return this.root == NULL_NODE;
    }

    /**
     * Internal method to insert into a subtree.
     * 
     * @param x
     *            the item to insert.
     * @param t
     *            the node that roots the tree.
     * @return the new root.
     * @throws DuplicateItemException
     *             if x is already present.
     */
    private Region insert(Region x, Region t) {
        if (t == NULL_NODE) {
            t = x;
        } else if (x.compareTo(t) < 0) {
            t.left(insert(x, t.left));
        } else if (x.compareTo(t) > 0) {
            t.right(insert(x, t.right));
        } else {
            throw new IllegalArgumentException("Cannot insert " + x + " into " + this);
        }

        t = skew(t);
        t = split(t);
        return t;
    }

    /**
     * Internal method to remove from a subtree.
     * 
     * @param x
     *            the item to remove.
     * @param t
     *            the node that roots the tree.
     * @return the new root.
     */
    private Region remove(Comparable x, Region t) {
        if (t != NULL_NODE) {
            // Step 1: Search down the tree and set lastNode and deletedNode
            this.lastNode = t;
            if (x.compareTo(t) < 0) {
                t.left(remove(x, t.left));
            } else {
                this.deletedNode = t;
                t.right(remove(x, t.right));
            }

            // Step 2: If at the bottom of the tree and
            // x is present, we remove it
            if (t == this.lastNode) {
                if (this.deletedNode != NULL_NODE && x.compareTo(this.deletedNode) == 0) {
                    this.deletedNode.swap(t);
                    this.deletedElement = t;
                    t = t.right;
                }
            } else if (t.left.level < t.level - 1 || t.right.level < t.level - 1) {
                // Step 3: Otherwise, we are not at the bottom; re-balance
                if (t.right.level > --t.level) {
                    t.right.level = t.level;
                }
                t = skew(t);
                t.right(skew(t.right));
                t.right.right(skew(t.right.right));
                t = split(t);
                t.right(split(t.right));
            }
        }
        return t;
    }

    /**
     * Skew primitive for AA-trees.
     * 
     * @param t
     *            the node that roots the tree.
     * @return the new root after the rotation.
     */
    private static Region skew(Region t) {
        if (t.left.level == t.level) {
            t = rotateWithLeftChild(t);
        }
        return t;
    }

    /**
     * Split primitive for AA-trees.
     * 
     * @param t
     *            the node that roots the tree.
     * @return the new root after the rotation.
     */
    private static Region split(Region t) {
        if (t.right.right.level == t.level) {
            t = rotateWithRightChild(t);
            t.level++;
        }
        return t;
    }

    /**
     * Rotate binary tree node with left child.
     */
    private static Region rotateWithLeftChild(Region k2) {
        Region k1 = k2.left;
        k2.left(k1.right);
        k1.right(k2);
        return k1;
    }

    /**
     * Rotate binary tree node with right child.
     */
    private static Region rotateWithRightChild(Region k1) {
        Region k2 = k1.right;
        k1.right(k2.left);
        k2.left(k1);
        return k2;        
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "RegionSet = { " + this.root.dump() + " }";
    }
    
    /**
     * Class that represents the regions held within this set.
     */
    public static class Region implements Comparable<Region> {
        private Region left;
        private Region right;
        private int level;

        private long start;
        private long end;

        private long contiguous;

        private Region() {
            this.start = 1;
            this.end = 0;
            this.level = 0;
            this.left = this;
            this.right = this;
            this.contiguous = size();
        }

        /**
         * Creates a region containing just the single given value
         * @param value
         */
        public Region(long value) {
            this(value, value);
        }

        /**
         * Creates a region containing the given range of value (inclusive).
         */
        public Region(long start, long end) {
            this.start = start;
            this.end = end;
            
            this.left = NULL_NODE; 
            this.right = NULL_NODE;
            this.level = 1;
            updateContiguous();
        }

        /**
         * Create a shallow copy of a region.
         * <p>
         * The new Region has NULL left and right children.
         */
        public Region(Region r) {
            this(r.start(), r.end());
        }
        
        private long contiguous() {
            if (left == NULL_NODE && right == NULL_NODE) {
                return size();
            } else {
                //long recursive = Math.max(size(), Math.max(left.contiguous(), right.contiguous()));
                //Assert.assertEquals(recursive, contiguous);
                return contiguous;
            }
        }

        private void updateContiguous() {
            contiguous = Math.max(size(), Math.max(left.contiguous(), right.contiguous()));
        }
        
        private void left(Region l) {
            this.left = l;
            updateContiguous();
        }
        
        private void right(Region r) {
            this.right = r;
            updateContiguous();
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "Range(" + this.start + "," + this.end + ")" + " contiguous:" + this.contiguous();
        }

        private String dump() {
            String ds = "";
            if (this.left != NULL_NODE) {
                ds = this.left.dump();
                ds += "," + String.valueOf(this);
            } else {
                ds += String.valueOf(this);
            }
            if (this.right != NULL_NODE) {
                ds = ds + "," + this.right.dump();
            }
            return ds;
        }
        
        /**
         * Returns the size of this range (the number of values within its bounds).
         */
        public long size() {
            // since it is all inclusive
            return (isNull() ? 0 : this.end - this.start + 1);
        }

        /**
         * Return true if this region is null, i.e. represents no valid range.
         */
        protected boolean isNull() {
            return this.start > this.end;
        }

        /**
         * Remove the supplied region from this region.
         * 
         * @param r region to remove
         * @return a possible extra region to be added, or null if none is required
         * @throws IllegalArgumentException if this region does not completely enclose the supplied region
         */
        protected Region remove(Region r) throws IllegalArgumentException {
            if (r.start < this.start || r.end > this.end) {
                throw new IllegalArgumentException("Ranges : Illegal value passed to remove : " + this + " remove called for : " + r);
            }
            if (this.start == r.start) {
                this.start = r.end + 1;
                updateContiguous();
                return null;
            } else if (this.end == r.end) {
                this.end = r.start - 1;
                updateContiguous();
                return null;
            } else {
                Region newRegion = new Region(r.end + 1, this.end);
                this.end = r.start - 1;
                updateContiguous();
                return newRegion;
            }
        }

        /**
         * Merge the supplied region into this region (if they are adjoining).
         * 
         * @param r region to merge
         * @throws IllegalArgumentException if the regions are not adjoining
         */
        protected void merge(Region r) throws IllegalArgumentException {
            if (this.start == r.end + 1) {
                this.start = r.start;
            } else if (this.end == r.start - 1) {
                this.end = r.end;
            } else {
                throw new IllegalArgumentException("Ranges : Merge called on non contiguous values : [this]:" + this + " and " + r);
            }
            updateContiguous();
        }

        /**
         * {@inheritDoc}
         */
        public int compareTo(Region other) {
            if (this.start < other.start) {
                return -1;
            } else if (this.end > other.end) {
                return 1;
            } else {
                return 0;
            }
        }

        private void swap(Region other) {
            Region r = other;
            long temp = this.start;
            this.start = r.start;
            r.start = temp;
            temp = this.end;
            this.end = r.end;
            r.end = temp;
            updateContiguous();
        }

        /**
         * Returns the start of this range (inclusive).
         */
        public long start() {
            return start;
        }

        /**
         * Returns the end of this range (inclusive).
         */
        public long end() {
            return end;
        }
    }
}