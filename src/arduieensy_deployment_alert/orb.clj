(ns arduieensy-deployment-alert.orb
  (:import (gnu.io CommPortIdentifier SerialPort))
  (:require [clojure.set :as set]))

(defn- open-teensy-output-stream []
  (loop [pids (enumeration-seq (CommPortIdentifier/getPortIdentifiers))]
    (if (= CommPortIdentifier/PORT_SERIAL (.getPortType (first pids)))
      (let [serial-port (.open (first pids) "ArduieensyExtremeFeedback" 2000)]
        (do (.setSerialPortParams serial-port 57600 SerialPort/DATABITS_8 SerialPort/STOPBITS_1 SerialPort/PARITY_NONE)            
            (.getOutputStream serial-port)))
      (recur (rest pids)))))

(def orb {:pins {:blue #{12} :green #{14} :red #{15}
                 :yellow #{14 15} :cyan #{12 14} :magenta #{12 15}
                 :all #{12 14 15} :none #{}}
          :pin-max 255
          :pin-min 0
          :color-ramp-interval 3
          :output-stream (open-teensy-output-stream)
          :current-color (ref :none)
          :throbbing? (ref false)})

(defn- write-to-pin! [orb pin value]
  (.write (orb :output-stream)
          (byte-array [(unchecked-byte pin) (unchecked-byte value)])))

(defn- jump-to! [orb color]
  (doseq [pin (get-in orb [:pins :all])]
    (write-to-pin! orb pin
                   (if (some #{pin} (get-in orb [:pins color]))
                     (orb :pin-max)
                     (orb :pin-min)))))

(defn- ramp-up! [orb color]
  (do (jump-to! orb :none)
      (doseq [value (range (orb :pin-min) (orb :pin-max))
              pin (get-in orb [:pins color])]
        (write-to-pin! orb pin value)
        (Thread/sleep (orb :color-ramp-interval)))))

(defn- ramp-down! [orb color]
  (do (jump-to! orb :none)
      (doseq [value (range (orb :pin-max) (orb :pin-min) -1)
              pin (get-in orb [:pins color])]
        (write-to-pin! orb pin value)
        (Thread/sleep (orb :color-ramp-interval)))))
 
(defn set-color!
  ([orb color]
     (set-color! orb color false))
  ([orb color throbbing?]
     (dosync
      (ref-set (orb :current-color) color)
      (ref-set (orb :throbbing?) throbbing?))))

(defn update! [orb]
  (if @(orb :throbbing?)
    (do (ramp-up! orb @(orb :current-color))
        (ramp-down! orb @(orb :current-color))
        (recur orb))
    (do (jump-to! orb @(orb :current-color))
        (Thread/sleep 1000)
        (recur orb))))

(update! orb)