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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        int headerSize = impl.getHeaders().size();
        assertTrue(String.format("Expected size for headers is two but got %d", headerSize), headerSize == 2);

        String cacheHeader = "public, max-age=120, stale-if-error=300";

        impl.setHeader("cache-control", cacheHeader);

        headerSize = impl.getHeaders().size();
        assertTrue(String.format("Expected size for headers is 1 but got %d", headerSize), headerSize == 1);

        String[] retrievedHeader = (String[]) impl.getHeaders().iterator().next();

        assertEquals(cacheHeader, retrievedHeader[1]);
    }

}
