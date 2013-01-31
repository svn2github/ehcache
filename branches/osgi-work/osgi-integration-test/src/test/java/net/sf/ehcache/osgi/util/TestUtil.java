package net.sf.ehcache.osgi.util;

import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;

import org.ops4j.pax.exam.Option;

public class TestUtil {
  public static Option commonOptions() {
    return composite(
        junitBundles(),
        workingDirectory("target/pax-exam"),
        cleanCaches(),
        when(Boolean.getBoolean("debug")).useOptions(
            vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"),
            systemTimeout(0)));
  }
}
