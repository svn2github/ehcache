/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.constructs.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.io.Serializable;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for the GenericResponseWrapper
 * @author Benjamin Schmaus
 */
public class GenericResponseWrapperTest {

    private GenericResponseWrapper impl;
    @Mock
    private HttpServletResponse mockResponse;
    @Mock
    private OutputStream mockOutputStream;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        impl = new GenericResponseWrapper(mockResponse, mockOutputStream);
    }

    @Test
    public void verifySetHeaderOverwritesExistingHeaderValues() {
        impl.addHeader("Cache-Control", "public, max-age=0, stale-if-error=600");
        impl.addHeader("CACHE-CONTROL", "public, max-age=120, stale-if-error=600");
        int headerSize = impl.getAllHeaders().size();
        assertTrue(String.format("Expected size for headers is two but got %d", headerSize), headerSize == 2);

        String cacheHeader = "public, max-age=120, stale-if-error=300";

        impl.setHeader("cache-control", cacheHeader);

        headerSize = impl.getAllHeaders().size();
        assertTrue(String.format("Expected size for headers is 1 but got %d", headerSize), headerSize == 1);

        final Header<? extends Serializable> retrievedHeader = impl.getAllHeaders().iterator().next();

        assertEquals(cacheHeader, retrievedHeader.getValue());
    }
    
    @Test
    public void testHeaderCrossTypeOverwrite() {
        impl.addHeader("Expires", "Tue, 29 Mar 2011 19:46:30 GMT");
        impl.addDateHeader("Expires", 123456789L);
        impl.addIntHeader("Expires", 123456789);
        int headerSize = impl.getAllHeaders().size();
        assertTrue(String.format("Expected size for headers is 3 but got %d", headerSize), headerSize == 3);

        String expiresHeader = "Tue, 29 Mar 2011 19:46:30 GMT";
        impl.setHeader("Expires", "Tue, 29 Mar 2011 19:46:30 GMT");

        headerSize = impl.getAllHeaders().size();
        assertTrue(String.format("Expected size for headers is 1 but got %d", headerSize), headerSize == 1);

        final Header<? extends Serializable> retrievedHeader = impl.getAllHeaders().iterator().next();

        assertEquals(expiresHeader, retrievedHeader.getValue());
    }

}
