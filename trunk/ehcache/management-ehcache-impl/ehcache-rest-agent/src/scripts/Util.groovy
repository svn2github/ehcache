
class Util {
  static final def ant = new AntBuilder()

  static void packageAgentJar(project) {
    def jarFile = new File(project.build.directory, "${project.artifactId}-${project.version}.jar")
    def packagingDir = new File(project.build.directory, "packaging")
    def privateClasspath = new File(packagingDir, project.properties["private-classpath"])
    
    // clean up
    if (packagingDir.exists()) {
      ant.delete(includeEmptyDirs: "true") {
        fileset(dir: packagingDir, includes: "**/*")
      }
    }
    
    // unzip and copy needed stuff to private classpath and META-INF
    ant.mkdir(dir: privateClasspath)
    ant.unzip(src : jarFile, dest: privateClasspath)
    ant.copy(todir: new File(packagingDir, "META-INF")) {
      fileset(dir: new File(privateClasspath, "META-INF")) {
        include(name: "**/maven/${project.groupId}/**")
        include(name: "MANIFEST.MF")
      }                                
    }
    
    // convert .class files under private classpath to have private-class-suffix
    def privateClassSuffix = project.properties["private-class-suffix"]
    ant.move(todir: privateClasspath, includeemptydirs: "false") {
      fileset(dir: privateClasspath) {
        include(name: "**/*.class")
      }
      mapper(type: "glob", from: "*.class", to: "*" + privateClassSuffix)
    }
    
    // inject correct pom.xml produced by shade plugin
    ant.copy(file: new File(project.build.directory, "dependency-reduced-pom.xml"),
             tofile: new File(packagingDir, "META-INF/maven/${project.groupId}/${project.artifactId}/pom.xml"),
             overwrite: "true")
    
    // rejar the artifact
    ant.jar(file: jarFile, basedir: packagingDir, manifest: new File(packagingDir, "META-INF/MANIFEST.MF"))  
  }
}