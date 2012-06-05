(defproject arduieensy-deployment-alert "1.0.0-SNAPSHOT"
  :description "Sources for controlling a Teensy 2.0 based alerting orb (that flashes when you need to move your deployment on)"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.rxtx/rxtx "2.1.7"]]
  :jvm-opts ["-Djava.library.path=/usr/lib/jni" "-Dgnu.io.rxtx.SerialPorts=/dev/ttyACM0"])
