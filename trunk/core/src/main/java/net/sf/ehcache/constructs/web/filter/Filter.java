/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

package net.sf.ehcache.constructs.web.filter;

import java.io.IOException;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A generic {@link javax.servlet.Filter} with most of what we need done.
 * <p/>
 * Participates in the Template Method pattern with {@link javax.servlet.Filter}.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public abstract class Filter implements javax.servlet.Filter {
    /**
     * If a request attribute NO_FILTER is set, then filtering will be skipped
     */
    public static final String NO_FILTER = "NO_FILTER";
    private static final Log LOG = LogFactory.getLog(Filter.class.getName());

    /**
     * The filter configuration.
     */
    protected FilterConfig filterConfig;

    /**
     * The exceptions to log differently, as a comma separated list
     */
    protected String exceptionsToLogDifferently;


    /**
     * A the level of the exceptions which will be logged differently
     */
    protected String exceptionsToLogDifferentlyLevel;


    /**
     * Most {@link Throwable}s in Web applications propagate to the user. Usually they are logged where they first
     * happened. Printing the stack trace once a {@link Throwable} as propagated to the servlet is sometimes
     * just clutters the log.
     * <p/>
     * This field corresponds to an init-param of the same name. If set to true stack traces will be suppressed.
     */
    protected boolean suppressStackTraces;


    /**
     * Performs the filtering.  This method calls template method
     * {@link #doFilter(javax.servlet.http.HttpServletRequest,javax.servlet.http.HttpServletResponse,javax.servlet.FilterChain) } which does the filtering.
     * This method takes care of error reporting and handling.
     * Errors are reported at {@link Log#warn(Object)} level because http tends to produce lots of errors.
     *
     * @throws IOException if an IOException occurs during this method it will be rethrown and will not be wrapped
     */
    public final void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws ServletException, IOException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
        final HttpServletResponse httpResponse = (HttpServletResponse) response;
        try {
            //NO_FILTER set for RequestDispatcher forwards to avoid double gzipping
            if (filterNotDisabled(httpRequest)) {
                doFilter(httpRequest, httpResponse, chain);
            } else {
                chain.doFilter(request, response);
            }
        } catch (final Throwable throwable) {
            logThrowable(throwable, httpRequest);
        }
    }

    /**
     * Filters can be disabled programmatically by adding a {@link #NO_FILTER} parameter to the request.
     * This parameter is normally added to make RequestDispatcher include and forwards work.
     *
     * @param httpRequest the request
     * @return true if NO_FILTER is not set.
     */
    protected boolean filterNotDisabled(final HttpServletRequest httpRequest) {
        return httpRequest.getAttribute(NO_FILTER) == null;
    }

    /**
     * This method should throw IOExceptions, not wrap them.
     */
    private void logThrowable(final Throwable throwable, final HttpServletRequest httpRequest)
            throws ServletException, IOException {
        StringBuffer messageBuffer = new StringBuffer("Throwable thrown during doFilter on request with URI: ")
                .append(httpRequest.getRequestURI())
                .append(" and Query: ")
                .append(httpRequest.getQueryString());
        String message = messageBuffer.toString();
        boolean matchFound = matches(throwable);
        if (matchFound) {
            try {
                if (suppressStackTraces) {
                    Method method = Log.class.getMethod(exceptionsToLogDifferentlyLevel, new Class[]{Object.class});
                    method.invoke(LOG, new Object[]{throwable.getMessage()});
                } else {
                    Method method = Log.class.getMethod(exceptionsToLogDifferentlyLevel,
                            new Class[]{Object.class, Throwable.class});
                    method.invoke(LOG, new Object[]{throwable.getMessage(), throwable});
                }
            } catch (Exception e) {
                LOG.fatal("Could not invoke Log method for " + exceptionsToLogDifferentlyLevel, e);
            }
            if (throwable instanceof IOException) {
                throw (IOException) throwable;
            } else {
                throw new ServletException(message, throwable);
            }
        } else {

            if (suppressStackTraces) {
                LOG.warn(messageBuffer.append(throwable.getMessage()).append("\nTop StackTraceElement: ")
                        .append(throwable.getStackTrace()[0].toString()));
            } else {
                LOG.warn(messageBuffer.append(throwable.getMessage()), throwable);
            }
            if (throwable instanceof IOException) {
                throw (IOException) throwable;
            } else {
                throw new ServletException(throwable);
            }
        }
    }

    /**
     * Checks whether a throwable, its root cause if it is a {@link ServletException}, or its cause, if it is a
     * Chained Exception matches an entry in the exceptionsToLogDifferently list
     *
     * @param throwable
     * @return true if the class name of any of the throwables is found in the exceptions to log differently
     */
    private boolean matches(Throwable throwable) {
        if (exceptionsToLogDifferently == null) {
            return false;
        }
        if (exceptionsToLogDifferently.indexOf(throwable.getClass().getName()) != -1) {
            return true;
        }
        if (throwable instanceof ServletException) {
            Throwable rootCause = (((ServletException) throwable).getRootCause());
            if (exceptionsToLogDifferently.indexOf(rootCause.getClass().getName()) != -1) {
                return true;
            }
        }
        if (throwable.getCause() != null) {
            Throwable cause = throwable.getCause();
            if (exceptionsToLogDifferently.indexOf(cause.getClass().getName()) != -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Initialises the filter.
     * <p/>
     * Calls template method {@link #doInit(javax.servlet.FilterConfig)} to perform any filter specific initialisation.
     */
    public final void init(final FilterConfig filterConfig) throws ServletException {
        try {

            this.filterConfig = filterConfig;
            processInitParams(filterConfig);

            // Attempt to initialise this filter
            doInit(filterConfig);
        } catch (final Exception e) {
            LOG.fatal("Could not initialise servlet filter.", e);
            throw new ServletException("Could not initialise servlet filter.", e);
        }
    }

    /**
     * Processes initialisation parameters. These are configured in web.xml in accordance with the
     * Servlet specification using the following syntax:
     * <pre>
     * <filter>
     *      ...
     *      <init-param>
     *          <param-name>blah</param-name>
     *          <param-value>blahvalue</param-value>
     *      </init-param>
     *      ...
     * </filter>
     * </pre>
     * @throws ServletException
     */
    protected void processInitParams(final FilterConfig config) throws ServletException {
        String exceptions = config.getInitParameter("exceptionsToLogDifferently");
        String level = config.getInitParameter("exceptionsToLogDifferentlyLevel");
        String suppressStackTracesString = config.getInitParameter("suppressStackTraces");
        suppressStackTraces = Boolean.valueOf(suppressStackTracesString).booleanValue();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Suppression of stack traces enabled for " + this.getClass().getName());
        }

        if (exceptions != null) {
            validateMandatoryParameters(exceptions, level);
            validateLevel(level);
            exceptionsToLogDifferentlyLevel = level;
            exceptionsToLogDifferently = exceptions;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Different logging levels configured for " + this.getClass().getName());
            }
        }
    }

    private void validateMandatoryParameters(String exceptions, String level) throws ServletException {
        if ((exceptions != null && level == null) || (level != null && exceptions == null)) {
            throw new ServletException("Invalid init-params. Both exceptionsToLogDifferently"
                    + " and exceptionsToLogDifferentlyLevelvalue should be specified if one is"
                    + " specified.");
        }
    }

    private void validateLevel(String level) throws ServletException {
        //Check correct level set
        if (!(level.equals("debug")
                || level.equals("info")
                || level.equals("warn")
                || level.equals("error")
                || level.equals("fatal"))) {
            throw new ServletException("Invalid init-params value for \"exceptionsToLogDifferentlyLevel\"."
                    + "Must be one of debug, info, warn, error or fatal.");
        }
    }

    /**
     * Destroys the filter. Calls template method {@link #doDestroy()}  to perform any filter specific
     * destruction tasks.
     */
    public final void destroy() {
        this.filterConfig = null;
        doDestroy();
    }

    /**
     * Checks if request accepts the named encoding.
     */
    protected boolean acceptsEncoding(final HttpServletRequest request, final String name) {
        final boolean accepts = headerContains(request, "Accept-Encoding", name);
        return accepts;
    }

    /**
     * Checks if request contains the header value.
     */
    private boolean headerContains(final HttpServletRequest request, final String header, final String value) {

        logRequestHeaders(request);

        final Enumeration accepted = request.getHeaders(header);
        while (accepted.hasMoreElements()) {
            final String headerValue = (String) accepted.nextElement();
            if (headerValue.indexOf(value) != -1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Logs the request headers, if debug is enabled.
     *
     * @param request
     */
    protected void logRequestHeaders(final HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            Map headers = new HashMap();
            Enumeration enumeration = request.getHeaderNames();
            StringBuffer logLine = new StringBuffer();
            logLine.append("Request Headers");
            while (enumeration.hasMoreElements()) {
                String name = (String) enumeration.nextElement();
                String headerValue = request.getHeader(name);
                headers.put(name, headerValue);
                logLine.append(": ").append(name).append(" -> ").append(headerValue);
            }
            LOG.debug(logLine);
        }
    }


    /**
     * A template method that performs any Filter specific destruction tasks.
     * Called from {@link #destroy()}
     */
    protected abstract void doDestroy();


    /**
     * A template method that performs the filtering for a request.
     * Called from {@link #doFilter(ServletRequest,ServletResponse,FilterChain)}.
     */
    protected abstract void doFilter(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse,
                                     final FilterChain chain) throws Throwable;

    /**
     * A template method that performs any Filter specific initialisation tasks.
     * Called from {@link #init(FilterConfig)}.
     * @param filterConfig
     */
    protected abstract void doInit(FilterConfig filterConfig) throws Exception;

    /**
     * Returns the filter config.
     */
    public FilterConfig getFilterConfig() {
        return filterConfig;
    }

    /**
     * Determine whether the user agent accepts GZIP encoding. This feature is part of HTTP1.1.
     * If a browser accepts GZIP encoding it will advertise this by including in its HTTP header:
     * <p/>
     * <code>
     * Accept-Encoding: gzip
     * </code>
     * <p/>
     * Requests which do not accept GZIP encoding fall into the following categories:
     * <ul>
     * <li>Old browsers, notably IE 5 on Macintosh.
     * <li>Search robots such as yahoo. While there are quite a few bots, they only hit individual
     * pages once or twice a day. Note that Googlebot as of August 2004 now accepts GZIP.
     * <li>Internet Explorer through a proxy. By default HTTP1.1 is enabled but disabled when going
     * through a proxy. 90% of non gzip requests are caused by this.
     * <li>Site monitoring tools
     * </ul>
     * As of September 2004, about 34% of requests coming from the Internet did not accept GZIP encoding.
     *
     * @param request
     * @return true, if the User Agent request accepts GZIP encoding
     */
    protected boolean acceptsGzipEncoding(HttpServletRequest request) {
        return acceptsEncoding(request, "gzip");
    }

}

