package net.sf.ehcache.management.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.jersey.core.spi.scanning.ScannerException;
import com.sun.jersey.core.spi.scanning.ScannerListener;
import com.sun.jersey.core.spi.scanning.uri.BundleSchemeScanner;
import com.sun.jersey.core.spi.scanning.uri.FileSchemeScanner;
import com.sun.jersey.core.spi.scanning.uri.JarZipSchemeScanner;
import com.sun.jersey.core.spi.scanning.uri.UriSchemeScanner;
import com.sun.jersey.core.spi.scanning.uri.VfsSchemeScanner;

public class TerracottaPrivateClassScanner implements UriSchemeScanner {
	private static final String PRIVATE_CLASS_SUFFIX = ".class_terracotta";

    private Map<String, UriSchemeScanner> scanners = new HashMap<String, UriSchemeScanner>();

    public TerracottaPrivateClassScanner() {
        inspect(new JarZipSchemeScanner());
        inspect(new FileSchemeScanner());
        inspect(new VfsSchemeScanner());
        inspect(new BundleSchemeScanner());
    }

    private void inspect(UriSchemeScanner scanner) {
        for (String scheme : scanner.getSchemes()) {
            scanners.put(scheme, scanner);
        }
    }

    @Override
    public Set<String> getSchemes() {
        return scanners.keySet();
    }

    @Override
    public void scan(URI u, ScannerListener sl) throws ScannerException {
        UriSchemeScanner scanner = scanners.get(u.getScheme());
        if (scanner == null) {
            throw new AssertionError();
        }
        scanner.scan(u, new ClazzScannerListener(sl));
    }

    private static class ClazzScannerListener implements ScannerListener {

        private final ScannerListener sl;

        ClazzScannerListener(ScannerListener sl) {
            this.sl = sl;
        }

        public boolean onAccept(String name) {
            if (name.endsWith(PRIVATE_CLASS_SUFFIX)) {
                name = name.substring(0, name.lastIndexOf(PRIVATE_CLASS_SUFFIX)).concat(".class");
            }

            return sl.onAccept(name);
        }

        public void onProcess(String name, InputStream in) throws IOException {
            sl.onProcess(name, in);
        }
    }

}
