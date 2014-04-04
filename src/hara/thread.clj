(ns hara.thread)

(defn current
  "Returns the currenly executing thread."
  []
  (Thread/currentThread))

(defn daemon
  "Creates a new daemon thread and sets runnable to f"
  [f]
  (doto (Thread. f) 
    (.setDaemon true)
    (.start)))

(defn thread
  "Creates a new thread and sets runnable to f"
  [f]
  (doto (Thread. f) 
    (.start)))

(defn sleep
  "Shortcut for Thread/sleep.

    (sleep 100) ;=> <sleeps for 100ms>.
  "
  [ms]
  (Thread/sleep ms))

(defn yield
  "Yields control of the currently executing thread."
  []
  (Thread/yield))

(defn interrupt
  "Interrupts a `thd` or the current thread
   if no arguments are given.
  "
  ([] (interrupt (current)))
  ([thd] (.interrupt thd)))


(comment
  (defn periodic
    [f ms]
    (fn []
      (loop [] (f) (sleep ms) (recur))))
)