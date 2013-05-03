;;; Stolen from https://www.refheap.com/paste/5073/fullscreen

(ns hara.nothing)
;;; There's a very common question which is asked during interviews for
;;; software engineering jobs. The candidate is asked to implement a version
;;; of a game so-called Fizz-Buzz. The problem is very simple and any one
;;; who can code should be able to implement a solution in a couple of
;;; minutes. Surprisingly enough, some people claim that this in reality
;;; is not quite so[1].

;;; The Fizz-Buzz problem is very simple: write a program that prints a sequence
;;; of numbers from 1 to 100, however whenever it should print a multiple of 3
;;; instead print the word "Fizz". For multiples of 5, print the word "Buzz"
;;; and for multiples of both 3 and 5 print "FizzBuzz".

;;; An implementation in clojure could be written as
;;; follows:

;; (defn fizzbuzz [n]
;;   (cond (and (zero? (mod n 15))) "FizzBuzz"
;;              (zero? (mod n 3)) "Fizz"
;;              (zero? (mod n 5)) "Buzz"
;;              :default (str n))

;; (defn run []
;;   (map fizzbuzz (range 1 101)))

;;; Here, instead of printing the numbers (and words) we're producing a sequence
;;; for re-use later. I think most interviewers would be happy with this
;;; solution, even if it returns the sequence instead of printing the values.

;;; Now, to be able to implement a solution like the one above, we have relied
;;; on several constructs that come as part of Clojure:

;;;   - all natural numbers between 1 and 100
;;;   - the ability to iterate over these
;;;   - the ability to check whether a number is 0
;;;   - the ability to branch depending on a particular predicate
;;;   - the ability to represent strings


;;; Following the footsteps of [2], where an implementation is offered using
;;; only Ruby's Procs, we shall implement the above Fizz-Buzz algorithm
;;; using only Clojure functions. In particular, and for no good reason,
;;; we will also limit ourselves to single-parameter functions.

;;; The first building block we need to implement are the natural numbers.

;;; Natural Numbers

;;; To implement the natural numbers using only Clojure functions, we're going
;;; to work with the following idea: counting can be thought of as comparing
;;; two bags of apples and oranges. If you start taking an apple from the first
;;; bag, followed by taking an orange from the other bag, and you find yourself
;;; with two empty bags at the same time, you can conclude that both have the
;;; same "number" of apples and oranges in each bag although you don't know what
;;; that number is.
;;; Hence, we can think of a "number" as applying an operation to an element a
;;; a particular number of times.

;;; And now onto some example numbers. Let's start with ZERO. If a NUMBER is the
;;; application of an operation to an element a particular number of times, then
;;; the NUMBER ZERO is the element itself:

(def ZERO (fn [f] (fn [x] x)))

;;; And ONE is the single application of the operation to the element:

(def ONE (fn [f] (fn [x] (f x))))

;;; And so on.

(def TWO (fn [f] (fn [x] (f (f x)))))
(def THREE (fn [f] (fn [x] (f (f (f x))))))
(def FOUR (fn [f] (fn [x] (f (f (f (f x)))))))
(def FIVE
  (fn [f] (fn [x] (f (f (f (f (f x))))))))

;;; more numbers should go here...

(def FIFTEEN
  (fn [f]
    (fn [x]
      (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f x))))))))))))))))))

;;; ...and plenty more in here...

(def ONE-HUNDRED
  (fn [f]
    (fn [x]
      (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f (f x)))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))

;;; By now it should be clear that defining all natural numbers like this is
;;; really tiresome. But we'll get back to this later when we define operations
;;; on NUMBERs.


;;; In any case, for easier inspection and to check that we're on the right
;;; track, we'll define a helper function that'll convert one of our
;;; implemented NUMBERs to a Clojure number.

;;; Since numbers are the application of an operation to an element, to convert
;;; a NUMBER to a Clojure number, we'll apply the function inc (which adds 1 to
;;; the given number) to 0.

(def ^{:doc "Utility function to get better reporting for NUMBERs.
  Use like (to-integer NUMBER), e.g.

  user> (to-integer FIVE) ; 5"}
  to-integer (fn [number] ((number inc) 0)))


;;; Booleans
;;; For any kind of flow control in an algorithm, we need to be able to
;;; tell whether an outcome is positive or negative. For this we'll use booleans.

;;; Booleans can be implemented as two functions which simply choose one parameter over the other

(def TRUE (fn [x] (fn [_] x)))
(def FALSE (fn [_] (fn [y] y)))

;;; Again, the obligatory helper function to check things in the REPL:
(def ^{:doc "Utility function to convert BOOLEANs to Clojure booleans.
  Use like (to-boolean BOOLEAN), e.g.

  user> (to-boolean TRUE) ; true"}
  to-boolean
  (fn [bool] ((bool true) false)))

;;; IF-THEN-ELSE

;;; Now that we can represent positive and negative outcomes as booleans, we
;;; can work on building an if-then-else flow control building block.

;;; Initially one can implement IF along the lines of (in Clojure):

;; (def IF (fn [pred] (fn [then] (fn [else] ((pred then) else)))))

;;; In our code predicates will ultimate boil down to either TRUE or FALSE.
;;; If pred evaluates to TRUE, then evaluating the entire IF form will be
;;; equivalent to evaluating the 'then' form. If pred is FALSE, then the
;;; evaluation of the 'else' part will be the result of the evaluation of
;;; the entire IF form.

;;; Hence, the implementation of IF can be simplified as follows:
;;; the way IF is written so that the evaluation of the entire form
;;; is actually the evaluation of the given predicate on the 'then' and the
;;; 'else' forms. This is basically saying that IF is a wrapper around
;;; the given predicate, and evaluating the IF form is equivalent to evaluating
;;; the predicate form. So, in principle, IF doesn't do much other than call
;;; pred with the then- and else-clauses. And so it can be simplified to be just
;;; the pred.

(def IF (fn [pred] pred))

;;; Predicates

;;; We now need to build some predicates that hopefully will be useful in the
;;; future.

;;; If you look at the proposed implementation of the Fizz-Buzz algorithm
;;; in clojure we're trying to rewrite, you'll see we make use of zero?
;;; which is a Clojure predicate that checks whether a given number is
;;; 0 or not. Let's start with that one.

;;; If you recall, we defined the number ZERO to be a function that applies
;;; the given operation 0 times on the given element. Any other natural NUMBER
;;; will apply the given operation N times on the element. Hence, to check if
;;; a NUMBER is ZERO or any other natural, we need to check how many times the
;;; given operation is applied. One or more applications means that the given
;;; NUMBER is not ZERO. We'll define ZERO? then as a function that takes a
;;; NUMBER and calls that number with an operation that returns FALSE and we'll
;;; use as starting element TRUE. Hence, if the operation is applied at least
;;; once, the result of (ZERO? NUMBER) will be FALSE.

(def ZERO? (fn [number] ((number (fn [_] FALSE)) TRUE)))

;;; Numeric operations

;;; The next building block we need to implement is the mod operator. And for
;;; this, we first need to be able to INCrement and DECrement a NUMBER.

;;; INCrementing a NUMBER is fairly straightforward. If a NUMBER is a function
;;; that applies an operation a certain number of times, then incrementing
;;; that NUMBER is simply applying the operation one more time:

(def INC (fn [number] (fn [f] (fn [x] (f ((number f) x))))))

;;; And adding two NUMBERs up is also quite straightforward: for two given
;;; NUMBERs n and m, and an operation f we apply f m times to the result of
;;; applying f n times to x:

(def ADD (fn [n] (fn [m] (fn [f] (fn [x] ((m f) ((n f) x)))))))

;;; MULTiplying two NUMBERs m and n is equivalent to ADDing m n times:
(def MULT (fn [n] (fn [m] ((m (ADD n)) ZERO))))

;;; And so is taking the POWer, but MULTiplying instead of ADDing:
(def POW (fn [n] (fn [m] ((m (MULT n)) ONE))))

;;; DECrementing a NUMBER is more involved and a better explanation can be
;;; in the Wikipedia entry for Church Encoding[3]. However, here's the
;;; implementation:

(def DEC (fn [number] (fn [f] (fn [x] (((number (fn [g] (fn [h] (h (g f)))))  (fn [y] x)) (fn [y] y))))))

;;; SUBstracting two NUMBERs n and m then is based on DECrementing n m times:

(def SUB (fn [n] (fn [m] ((m DEC) n))))

;;; Remainder of a division

;;; Now that we have some basic arithmetic operations in place, let's implement
;;; the mod operator.

;;; A tentative implementation in Clojure would be as follows:

;; (defn my-mod [n m]
;;   (if (>= n m)
;;     (my-mod (- n m) m) n))

;;; however this implementation relies on <= which we're not allowed to use!

;;; It looks like we'll have to implement <= before we can implement mod.
;;; We can implement <= rather easily by rewriting <= to:

;; (defn <= [n m] (<= (- n m) 0))

;;; (cheating by using <= again)

;;; This rewrite shows that we can check for lower-equal by checking
;;; if the SUBstraction of the two numbers is <= 0, then n <= m. Additionally,
;;; our definition of SUB is such that if we try to substract a larger number
;;; from a smaller one we'll get ZERO so all is good.

(def LEQ
  (fn [n]
    (fn [m]
      (ZERO? ((SUB n) m)))))

;;; With LEQ implemented, we can now implement modulo rather easily:
;;; We'll rewrite

;; (defn mymod [n m]
;;   (if (<= m n) (mymod (- n m) m) n))

;;; using our LEQ implementation and our SUB implementation.
;;;
;;; Once re-written, our MOD operation looks like:
;;;
;; (def MOD (fn [n]
;;            (fn [m]
;;              (((IF ((LEQ m) n)) (((MOD ((SUB n) m)) m) x)) n))))

;;; However this will quickly result in a nasty StackOverflow :'''(

;;; The reason for this is that in clojure, (if...) is a special form that
;;; only evaluates the then-clause if the predicate evaluates to true (same
;;; applies to the else-clause.) Our implementation of IF instead is eager,
;;; i.e. it will always evaluate both the then-clause and the else-clause
;;; regardless of the truthiness of the predicate. To work around this we
;;; use the fact we used to simplify our implementation of IF but in
;;; reverse: if we have a function f which takes a single parameter x and calls
;;; a function g with it, i.e. (defn f [x] (g x)), then we can replace f with g
;;; in all our code.
;;; Here we will do the opposite, wherever we have a call to g, we will
;;; introduce a function f so that we delay the evaluation of g. Our g in this
;;; case is MOD and so we'll replace ((MOD ((SUB n) m)) m) with
;;; (fn [x] (((MOD ((SUB n) m)) m) x)) which is equivalent in nature but
;;; lazy-er! \o/

(def MOD-CHEAT
  (fn [n]
    (fn [m]
      (((IF ((LEQ m) n)) (fn [x] (((MOD-CHEAT ((SUB n) m)) m) x))) n))))

;;; Unfortunately, albeit correct, our implementation of MOD relies on
;;; a feature of the language we should not rely on: the fact that to define
;;; MOD we use MOD as if it had been defined already.
;;; To solve this, we make use of the Y-combinator[4].

;;; Briefly, the Y-combinator solves the assignment problem since
;;; when you call the Y-combinator with a function, it will in turn
;;; call the function with the function itself as its first argument. Hence
;;; the function can now recurse (by calling itself again) without
;;; relying on itself being defined. The Y-combinator is implemented as follows:

;;;   (def Y-combinator (fn [f] (fn [x] ((f (x x)) (fn [x] (f (x x)))))))

;;; Sadly, just as our first version of MOD produced a StackOverflow, using
;;; the Y-combinator will also produce a StackOverflow due to eager
;;; evaluation of its parameters. Again, indirection saves the day and we
;;; use the same trick we used to fix MOD: we introduce a function f which
;;; will simply call g with the parameter passed (this is called the
;;; Z-combinator):

(def Z-combinator
  (fn [f]
    ((fn [x] (f (fn [y] ((x x) y))))
     (fn [x] (f (fn [y] ((x x) y)))))))

(def MOD
  (Z-combinator
   (fn [f]
     (fn [n]
       (fn [m]
         (((IF ((LEQ  m) n))
           (fn [x] (((f ((SUB n) m)) m) x)))
          n))))))

;;; Ok, now we have a bunch of building blocks for our barebones Fizz-Buzz
;;; however we still need: ranges, map, string literals and a way of
;;; converting NUMBERs to strings. Let's start with ranges and map.

;;; Ranges & Map

;;; To implement ranges we need to be able to build lists. The building block of
;;; lists is a pair (also known as tuple.) A pair is simply a pair of items
;;; (two - not "pair" in the many sense) and is sometimes implemented as an array
;;; of 2 items. In our case however we can only implement this with functions:

(def PAIR (fn [x] (fn [y] (fn [f] ((f x) y)))))

;;; A PAIR will take items x and y and a function f. A PAIR then becomes
;;; the result of calling as a function the result of (f x) with y.

;;; Extracting items from a pair then becomes:

(def LEFT (fn [pair] (pair (fn [x] (fn [_] x)))))
(def RIGHT (fn [pair] (pair (fn [_] (fn [y] y)))))

;;; So, LEFT will call pair with a function that returns its first element
;;; (in this case x) and ignore the second (naming the parameter of the
;;; returned function as _ denotes that it's not important.) And RIGHT
;;; will do the same thing except that it'll return the second value: y.

;;; Now that we have pairs, we can implement linked lists. However,
;;; instead of using a pointer to the next item (as it is normally done)
;;; we store the actual pair.

(def EMPTY ((PAIR TRUE) TRUE))
(def EMPTY? LEFT)
(def CONJ (fn [l] (fn [x] ((PAIR FALSE) ((PAIR x) l)))))
(def FIRST (fn [l] (LEFT (RIGHT l))))
(def REST (fn [l] (RIGHT (RIGHT l))))

;;; For easier inspection in the repl, we also write a convenience function
;;; (just like to-integer and to-boolean):

(defn to-vector
  "Convenience function to represent a list as a Clojure vector"
  ([l] (to-vector l []))
  ([l acc]
     (if (to-boolean (EMPTY? l))
       (reverse acc)
       (recur (REST l) (cons (FIRST l) acc)))))

(defn to-integer-vector
  "Convenience function to pretty-print ranges in the REPL."
  [l]
  (map to-integer (to-vector l)))

;;; Now we want to build ranges of NUMBERs. A slightly contrived
;;; way of doing so is to think as (range m n) as (conj (range (inc m) n) m)
;;; which is saying that the range of numbers between m and n is just
;;; m followed by the range of numbers between m+1 and n (provided that m and
;;; (m + 1) aren't larger than n.
;;; This observation will make it easier for us to write RANGE in terms of
;;; the basic operations we've just built.

(def RANGE
  (Z-combinator
   (fn [f]
     (fn [m]
       (fn [n] (((IF ((LEQ m) n)) (fn [x] (((CONJ ((f (INC m)) n)) m) x))) EMPTY))))))

;;; Ok, ranges out of the way means we need to be able to iterate over them.
;;; For this, we're going to implement the map function. map is a higher-order
;;; function (HOF) since it takes a function as one of its parameters.

;;; We will implement map using a helper HOF called fold. fold recursively
;;; combines the items in a data structure using a combination function which
;;; is passed as a parameter. fold also takes an optional initial item to
;;; combine with the first item extracted from the collection.
;;; Take for example folding over a sequence of numbers using + as the
;;; combination function and initial item 0: (fold [1 2 3] 0 +)
;;; Evaluating that form above would produce 6.
;;; Note: fold is sometimes referred to as reduce.

(def FOLD
  (Z-combinator
   (fn [f]
     (fn [l]
       (fn [x]
         (fn [g]
           (((IF (EMPTY? l))  x)
            (fn [y]
              (((g (((f (REST l)) x) g)) (FIRST l)) y)))))))))

;;; Now that we have a FOLD helper function implemented, writing MAP is simple.
;;; We'll implement MAP by using FOLD in the following fashion: the initial
;;; item will be an empty list (EMPTY) and the combination function will
;;; append items onto the target collection by means of CONJ:

(def MAP
  (fn [coll]
    (fn [f]
      (((FOLD coll) EMPTY)
       (fn [l]
         (fn [x] ((CONJ l) (f x))))))))

;;; We're getting closer.

;;; Recapping, we have the means of:
;;; 1) Representing numbers
;;; 2) Performing some arithmetic operations with them, including MOD
;;; 3) Representing lists of things, including (but not limited to) ranges of
;;;    numbers
;;; 4) Iterating over lists of things and performing an arbitrary operation
;;;    on these things

;;; It looks like all we're missing to be able to implement a full version of
;;; Fizz-Buzz are strings (and strings representing numbers too!) Let's
;;; implement that.

;;; Strings

;;; Strings aren't that difficult to represent. We can represent them as a list
;;; of numbers and then interpret each number according an encoding of our
;;; choosing.

;;; For Fizz-Buzz, it may be overkill to use a general purpose encoding scheme
;;; like ASCII, so we'll design our own. We only need to encode the strings
;;; "Fizz", "Buzz", FizzBuzz and the numbers 0 to 9. For the latter, we can
;;; simply use the numbers 0 to 9, i.e. 0 represents "0", 1 "1", and so on.
;;; That means we can use 10 to 14 for "B", "F", "i", "u", and "z".

(def B ((MULT TWO) FIVE))
(def F (INC B))
(def i (INC F))
(def u (INC i))
(def z (INC u))

(def Fizz ((CONJ ((CONJ ((CONJ ((CONJ EMPTY) z)) z)) i)) F))
(def Buzz ((CONJ ((CONJ ((CONJ ((CONJ EMPTY) z)) z)) u)) B))
(def FizzBuzz ((CONJ ((CONJ ((CONJ ((CONJ Buzz) z)) z)) i)) F))

;;; Now that we have some string representation for the words Fizz, Buzz and
;;; FizzBuzz, let's implement some helper functions for pretty printing these
;;; in the REPL (just like we did for integers, booleans, and lists).

(defn to-char
  "Converts an integer between 0 and 14 into a character."
  [n]
  {:pre [(<= n 14) (>= n 0)]}
  (nth (seq "0123456789BFiuz") n))

(defn to-string
  "Converts a LIST of NUMBERs to a Clojure string."
  [s]
  (apply str (map to-char (to-integer-vector s))))

;;; The final piece of the puzzle is the ability to represent a number as a
;;; string, e.g. 154 as "154". For this, we first need a function that'll
;;; split a natural number into a sequence of its digits, e.g. 154 -> '(1 5 4)

;;; An example implementation in clojure is as follows:

;; (defn to-digits
;;   [n]
;;   (conj (if (< n 10)
;;           '()
;;           (to-digits (int (/ n 10))))
;;         (mod n 10)))

;;; To rewrite this implementation using our own versions of IF, CONJ, MOD, etc.
;;; we'll need to fist implement / (we can get away without implementing < and
;;; checking for <= 9 instead of < 10).

(def DIV
  (Z-combinator
   (fn [f]
     (fn [m]
       (fn [n]
         (((IF ((LEQ n) m))
           (fn [x]
             ((INC ((f ((SUB m) n)) n)) x)))
          ZERO))))))

;;; Now we can go about implementing TODIGITS.

(def TEN ((MULT TWO) FIVE))
(def NINE (DEC TEN))

(def TODIGITS*
  (Z-combinator
   (fn [f]
     (fn [number]
       ((CONJ (((IF ((LEQ number) NINE)) EMPTY) (fn [x] ((f ((DIV number) TEN)) x)))) ((MOD number) TEN))))))

;;; But wait! The above function produces the wrong results I hear you say.
;;; Indeed it does. For the NUMBER 154 it will produce the list (4 5 1).
;;; That's because we're using conj/CONJ to accumulate the digits, and, as you
;;; correctly point out right now, conj/CONJ will append to the head of the
;;; list (to the first position in the list).
;;; We have two solutions to our problem:
;;; 1) Implement an operation PUSH that appends to the end of the list or
;;; 2) Implement REVERSE to reverse lists

;;; We'll go for option 2) since when we're done we'll be left with a
;;; more general function and also since appending to the tail of a
;;; list is a bad idea in general^tm.

(def REVERSE*
  (Z-combinator
   (fn [f]
     (fn [l]
       (fn [reversed]
         (((IF (EMPTY? l)) reversed) (fn [x] (((f (REST l)) ((CONJ reversed) (FIRST l))) x))))))))

(def REVERSE
  (fn [l] ((REVERSE* l) EMPTY)))

;;; And now we can properly implement TODIGITS:

(def TODIGITS (fn [number] (REVERSE (TODIGITS* number))))

;;; Now we're ready to implement Fizz-Buzz using nothing but functions.

;;; YAY! \o/

;;; However, one last helper function to pretty print things in the REPL:
;;; to-string-vector will help us by converting a LIST of our STRINGS
;;; into a clojure seq of clojure strings:

(defn to-string-vector
  "Converts a LIST of STRING into a Clojure seq of strings."
  [l]
  (map to-string (to-vector l)))

;;; And now, without further ado, FizzBuzz:

(def FIZZBUZZ
  (fn [number]
    (((IF (ZERO? ((MOD number) FIFTEEN))) FizzBuzz)
     (((IF (ZERO? ((MOD number) FIVE))) Fizz)
      (((IF (ZERO? ((MOD number) THREE))) Buzz)
       (TODIGITS number))))))

;;(def RUN
;;  ((MAP ((RANGE ONE) FIFTEEN)) FIZZBUZZ))

;;(println (to-string-vector RUN))

;;; The end


;; [1] http://www.codinghorror.com/blog/2007/02/why-cant-programmers-program.html
;; [2] http://experthuman.com/programming-with-nothing
;; [3] http://en.wikipedia.org/wiki/Church_encoding#Computation_with_Church_numerals
;; [4] http://en.wikipedia.org/wiki/Fixed-point_combinator#Y_combinator
