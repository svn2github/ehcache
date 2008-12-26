jsvc -pidfile ../pid/ehcache_server.pid -outfile ../logs/ehcache-server.log -errfile '&1' -cp ../lib/commons-daemon-1.0.1.jar:../lib/ehcache-standalone-server-0.6.jar:../lib/glassfish-embedded-all-3.0-Prelude-Embedded-m2.jar  net.sf.ehcache.server.standalone.Server 8080 ../war

