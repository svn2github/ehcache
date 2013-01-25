package net.sf.ehcache.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;

public class TestUtils {
	private static final Map<String, String> dependencies = getDependencies();

	private static Map<String, String> getDependencies() {
		Pattern mavenCoords = Pattern.compile("^\\s*([^:]+):([^:]+):([^:]+):([^:]+):(\\w+)$");
		Map<String, String> deps = new HashMap<String, String>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader("target/dependencies.txt"));
			String line = null;
			while ((line = reader.readLine()) != null) {
				Matcher m = mavenCoords.matcher(line);
				if (m.matches()) {
					String groupIdPlusArtifactId = m.group(1) + ":" + m.group(2);
					String version = m.group(4);
					deps.put(groupIdPlusArtifactId, version);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// ignored
				}
			}
		}
		return deps;
	}

	public static final String versionOf(String groupIdPlusArtifactId) {
		return dependencies.get(groupIdPlusArtifactId);
	}

	public static MavenArtifactProvisionOption testMavenBundle(String groupId, String artifactId) {
		String version = versionOf(groupId + ":" + artifactId);
		return new MavenArtifactProvisionOption().groupId(groupId).artifactId(artifactId)
		    .version(version);
	}
}
