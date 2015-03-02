(ns clj-msgpack.core
  (:require [clojure.java.io :as io])
  (:import [java.io ByteArrayOutputStream]
           [java.nio ByteBuffer]
           [org.msgpack.core MessagePack
            MessagePacker MessageUnpacker]
           [org.msgpack.value ArrayValue BooleanValue MapValue MapCursor
            RawValue Value FloatValue IntegerValue NilValue NumberValue
            ArrayCursor BinaryValue]
           [org.msgpack.value.impl BigIntegerValueImpl
            IntegerValueImpl LongValueImpl DoubleValueImpl FloatValueImpl]))

;;; Packing

(defprotocol Packable
  "Serialize the object to the packer."
  (pack-me [obj packer] "Serialize the object into the packer."))

(extend-protocol Packable
  nil
  (pack-me [_ ^MessagePacker packer]
    (.packNil packer))

  clojure.lang.Keyword
  (pack-me [kw ^MessagePacker packer]
    (.packString packer ^String (name kw)))

  clojure.lang.Symbol
  (pack-me [sym ^MessagePacker packer]
    (.packString packer ^String (name sym)))

  clojure.lang.Sequential
  (pack-me [s ^MessagePacker packer]
    (.packArrayHeader packer (count s))
    (doseq [item s]
      (pack-me item packer)))

  clojure.lang.IPersistentMap
  (pack-me [m ^MessagePacker packer]
    (.packMapHeader packer (count m))
    (doseq [[k v] m]
      (pack-me k packer)
      (pack-me v packer)))

  java.lang.String
  (pack-me [s ^MessagePacker packer]
    (.packString packer s))

  java.lang.Integer
  (pack-me [s ^MessagePacker packer]
    (.packInt packer s))

  java.lang.Short
  (pack-me [s ^MessagePacker packer]
    (.packShort packer s))

  java.lang.Byte
  (pack-me [s ^MessagePacker packer]
    (.packByte packer s))

  java.lang.Long
  (pack-me [s ^MessagePacker packer]
    (.packLong packer s))

  java.lang.Boolean
  (pack-me [s ^MessagePacker packer]
    (.packBoolean packer s))

  java.lang.Character
  (pack-me [s ^MessagePacker packer]
    (.packInt packer (.intValue ^Character s)))

  java.util.List
  (pack-me [s ^MessagePacker packer]
    (.packArrayHeader packer (.size s))
    (doseq [item s]
      (pack-me item packer)))

  java.util.Set
  (pack-me [s ^MessagePacker packer]
    (.packArrayHeader packer (.size s))
    (doseq [item s]
      (pack-me item packer)))

  java.util.Map
  (pack-me [m ^MessagePacker packer]
    (.packMapHeader packer (.size m))
    (doseq [[k v] m]
      (pack-me k packer)
      (pack-me v packer)))

  java.math.BigInteger
  (pack-me [m ^MessagePacker packer]
    (.packBigInteger packer m))

  java.nio.ByteBuffer
  (pack-me [m ^MessagePacker packer]
    (.packBinaryHeader packer (.remaining m))
    (.writePayload packer m)))

(extend (Class/forName "[B")
  Packable
  {:pack-me (fn [m ^MessagePacker packer]
              (.packBinaryHeader packer (alength m))
              (.writePayload packer m))})

(defprotocol ToPacker
  (to-packer [obj]))

(extend-protocol ToPacker
                                        ;Convert an object into an org.msgpack.Packer instance.
  MessagePacker
  (to-packer [p] p)
  Object
  (to-packer [obj]
    (MessagePack/newDefaultPacker (io/output-stream obj))))

(defn packer [dest]
  (to-packer dest))

(defn pack-into
  "Pack objects to the destination, which must be a Packer or coercible to an InputStream."
  [p & objs]
  (let [pr (packer p)]
    (doseq [obj objs]
      (pack-me obj pr))
    (.close pr))
  p)

(defn pack
  "Pack the objects into a byte array and return it."
  [& objs]
  (let [buf (ByteArrayOutputStream.)
        p (MessagePack/newDefaultPacker buf)]
    (apply pack-into p objs)
    (.toByteArray buf)))

;;; Unpacking

(defn- map-cursor-seq [^MapCursor mc]
  (loop [r []]
    (if (.hasNext mc)
      (let [k (.toValue (.nextKeyOrValue mc))
            v (.toValue (.nextKeyOrValue mc))]
        (recur (conj r [k v])))
      r)))

(defprotocol Unwrapable
  (unwrap [msgpack-obj key-fn key?]
    "Unwrap one of the funky wrapper objects that msgpack uses."))

(extend-protocol Unwrapable
  ;; Specialized unwraps
  BigIntegerValueImpl
  (unwrap [o _ _] (.toBigInteger o))
  DoubleValueImpl
  (unwrap [o _ _] (.toDouble o))
  FloatValueImpl
  (unwrap [o _ _] (.toFloat o))
  LongValueImpl
  (unwrap [o _ _] (.toLong o))

  ;; Non-specialized
  IntegerValue
  (unwrap [o _ _] (.toInt o))
  ArrayValue
  (unwrap [o key-fn key?]
    (into [] (map #(unwrap (.toValue %) key-fn key?)
                  (iterator-seq (.iterator (.getArrayCursor o))))))
  BooleanValue
  (unwrap [o _ _] (.toBoolean o))
  MapValue
  (unwrap [o key-fn _] (into {} (map (fn [[k v]] [(unwrap k key-fn true) (unwrap v key-fn false)]) (map-cursor-seq o))))
  NilValue
  (unwrap [o _ _] nil)
  RawValue
  (unwrap [o key-fn key?]
    (let [v (.toString o)]
      (if key? (key-fn v) v)))
  BinaryValue
  (unwrap [o _ _]
    (.toByteArray o)))

(defn unpack [from & {:keys [key-fn]
                      :or {key-fn identity}}]
  (let [is (io/input-stream from) ; hmmm, can't use with-open here...
        unpacker (MessagePack/newDefaultUnpacker is)
        cursor (.getCursor unpacker)]
    (map #(unwrap % key-fn false) (iterator-seq cursor))))
