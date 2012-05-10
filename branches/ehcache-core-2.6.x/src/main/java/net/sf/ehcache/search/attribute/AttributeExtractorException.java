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

package net.sf.ehcache.search.attribute;

import net.sf.ehcache.search.SearchException;

import java.io.Serializable;

/**
 * An exception to indicate that an attribute extractor was unable to be processed.
 * <p/>
 * Attributes are extracted on put or update, so this exception will be thrown to the calling
 * thread.
 *
 * @author Greg Luck
 */
public class AttributeExtractorException extends SearchException implements Serializable {


    private static final long serialVersionUID = 5066522240394222152L;


    /**
     * Construct a AttributeExtractorException
     *
     * @param message the description of the exception
     */
    public AttributeExtractorException(String message) {
        super(message);
    }

    /**
     * Construct a AttributeExtractorException with an underlying cause and message
     *
     * @param message
     * @param cause
     */
    public AttributeExtractorException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a AttributeExtractorException with an underlying cause
     *
     * @param cause
     */
    public AttributeExtractorException(Throwable cause) {
        super(cause);
    }
}
