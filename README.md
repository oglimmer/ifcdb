ifcdb
=====

The "infrequently changed data database" system. Stores byte data by string id.


REQUIREMENTS
============

* java 7
* maven 3.x
* haproxy (if you want to use fail-over and/or load-balancing)
* mysql (or any other jdbc enabled database, look into conf/config.properties for jdbc config)
* python 2.7.x if you want to run the python test


HOW TO RUN
==========

* start the mysql and create a database "prod_hib" (we assume an user `root` with no password)
* haproxy -f haproxy.cfg
* bin/startup.sh 8081
* bin/startup.sh 8082


HOW TO RUN SOME TESTS
=====================
* src/test/python/http-test.py "A_FILE_ON_YOUR_LOCAL_FILE_SYSTEM"
* src/test/bash/upload_load.sh "A_FILE_ON_YOUR_LOCAL_FILE_SYSTEM"
* mvn -Dtest=de.oglimmer.ifcdb.integration.MultiThreadedHttpTest test
* mvn -Dtest=de.oglimmer.ifcdb.integration.GeneralHttpTest test


SOME WAYS TO LOOK INTO THE SYSTEM
=================================
* see the haproxy admin at http://localhost:8090/haproxy?stats
* Use VisualVM or jconsole to connect to the JVM and watch the statistics at the MBean IFCDB:statistics
* see the debug/info/error logs in the log directory

