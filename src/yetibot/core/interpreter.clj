(ns yetibot.core.interpreter
  "Handles evaluation of a parse tree"
  (:require
    [yetibot.core.models.default-command :refer [configured-default-command]]
    [clojure.set :refer [difference intersection]]
    [yetibot.core.models.room :as room]
    [taoensso.timbre :refer [debug info warn error]]
    [yetibot.core.util :refer [psuedo-format]]
    [yetibot.core.util.format :refer [to-coll-if-contains-newlines]]))

(def ^:dynamic *current-user*)
(def ^:dynamic *chat-source*)

(defn handle-cmd
  "Hooked entry point for all command handlers. If no handlers intercept, it
   falls back to image search when available."
  [cmd-with-args extra]
  (info "nothing handled" cmd-with-args)
  (if-not (:fallback? extra)
    (handle-cmd (str (configured-default-command) " " cmd-with-args)
                (assoc extra :fallback? true))
    ; if fallback? is true, nothing handled this command so don't try to
    ; fallback again
    (format "I don't know how to handle %s" cmd-with-args)))

(defn pipe-cmds
  "Pipe acc into cmd-with-args by either appending or sending acc as an extra
   :opts"
  [acc [cmd-with-args & next-cmds]]
  (debug "pipe-cmds" *chat-source* acc cmd-with-args next-cmds)
  (let [extra {:raw (:value acc)
               :settings (:settings acc)
               :skip-next-n (:skip-next-n acc)
               :next-cmds next-cmds
               :user *current-user*
               :chat-source *chat-source*}
        {previous-value :value
         previous-data :data} acc
        possible-opts (to-coll-if-contains-newlines previous-value)]

    (debug "previous value" (pr-str previous-value))
    (debug "previous data" (pr-str previous-data))

    (if (> @(:skip-next-n acc) 0)
      (do
        (swap! (:skip-next-n acc) dec)
        (info "skipping already-consumed command" cmd-with-args "and the next"
              @(:skip-next-n acc) "commands")
        acc)

      ;; if possible-opts is a string, append acc to args. otherwise send
      ;; possible-opts as an extra :opts param and append nothing to
      ;; cmd-with-args.

      (let [;; the previous accumulated value. for the first command in a series
            ;; of piped commands, preivous-value and previous-data will be empty

            {previous-value :value
             previous-data :data} acc

            ;; the result of a commad handler can either be:
            ;; - the literal value itself
            ;; - a map containing a :value key and an optional :data key
            command-result
            (apply
              handle-cmd
              ;; determine whether to pass args to handle command as an :opts
              ;; collection or as a single value, depending on whether previous
              ;; value looks like a collection
              (if (coll? possible-opts)
                [cmd-with-args (conj extra {:opts possible-opts
                                            :data previous-data})]
                ;; value is the previous primitive output from the last
                ;; command. the first time around value is empty so just use
                ;; the raw cmd-with-args
                [(if (empty? previous-value)
                   cmd-with-args
                   ; next time apply psuedo-format to support %s substitution
                   (psuedo-format cmd-with-args previous-value))
                 extra]))

            {value :result/value
             data :result/data} (when (map? command-result)
                                  command-result)

            ]

        (if (and (map? command-result) value)
          (assoc acc
                 :value value
                 :data data)
          (assoc acc :value command-result))))))

(defn handle-expr
  "Entry point for Yetibot expression evaluation  An expression is the
   top-level construct. It contains 1 or more commands. Commands are reduced
   to a single output by sequentially piping the output from one command to the
   next until there are no more commands to evaluate.

   :data is an optional map that can contain any additional data from a
   previous command, to be optionally consumed by later commands."
  [& cmds]
  (info "reduce commands" (prn-str cmds) (partition-all (count cmds) 1 cmds))
  ; look up the settings for room in *chat-source*
  (let [room-settings (room/settings-for-chat-source *chat-source*)]
    (:value
      (reduce
        pipe-cmds
        ;; Allow commands to consume n next commands in the pipeline and inform
        ;; the reducer to skip over them. This is useful e.g. queries that can be
        ;; optimized by "pushing down" the operating into the query engine.
        {:settings room-settings :skip-next-n (atom 0) :value "" :data nil}
        ;; let each command peak into the next command so it can decide whether it
        ;; wants to consume it.
        (partition-all (count cmds) 1 cmds)))))
