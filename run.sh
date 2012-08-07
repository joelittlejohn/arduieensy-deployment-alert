export PIPELINE_URL="http://pipeline.brislabs.com:8080/pipeline"
java -jar -Djava.library.path=/usr/lib/jni -Dgnu.io.rxtx.SerialPorts=/dev/ttyACM0 -Djava.ext.dirs=lib target/arduieensy-deployment-alert-1.0.0-SNAPSHOT-standalone.jar
