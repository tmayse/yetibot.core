(ns yetibot.core.repl
  "Load this namespace when working with YetiBot in the REPL or during dev."
  (:require
    [yetibot.core.db :as db]
    [yetibot.core.models.users :as users]
    [yetibot.core.loader :refer [load-commands-and-observers load-ns]]
    [yetibot.core.adapters.campfire :as cf]
    [yetibot.core.adapters.irc :as irc]))

; use a few non-network commands for testing
(defn load-minimal []
  (require 'yetibot.core.commands.echo :reload)
  (require 'yetibot.core.commands.help :reload)
  (require 'yetibot.core.observers.history :reload)
  (require 'yetibot.core.commands.history :reload)
  (require 'yetibot.core.commands.users :reload)
  (require 'yetibot.core.commands.collections :reload))

(defn start []
  (load-minimal)
  (db/repl-start)
  (cf/start)
  (irc/start))

(defn load-all []
  (future
    (load-commands-and-observers)))
