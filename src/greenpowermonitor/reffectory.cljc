(ns greenpowermonitor.reffectory)

(def verbose (atom true))

(def ^:private initial-state {:cofxs     {}
                              :fxs       {}
                              :events    {}
                              :event-fns {}})

(def handlers (atom initial-state))

(defn- handler-not-found-error [id handler-type]
  (ex-info
   "Not registered handler!!"
   {:cause        :no-handler-registered
    :id           id
    :handler-type handler-type}))

(defn get-handler [handler-type id]
  (if-let [handler (get-in @handlers [handler-type id])]
    handler
    (throw (handler-not-found-error id handler-type))))

(defn register-cofx! [cofx-id handler]
  {:pre [(keyword? cofx-id)
         (fn? handler)]}
  (swap! handlers assoc-in [:cofxs cofx-id] handler))

(defn register-fx! [fx-id handler]
  {:pre [(keyword? fx-id)
         (fn? handler)]}
  (swap! handlers assoc-in [:fxs fx-id] handler))

(defn- handle-effects [effects-descriptions]
  (doseq [[effect-id data] effects-descriptions]
    (when data
      ((get-handler :fxs effect-id) data))))

(defn- apply-interceptors [interceptors]
  (reduce
   (fn [acc interceptor]
     (interceptor acc))
   {}
   interceptors))

(defn inject-cofx [cofx-kw & args]
  {:pre [(keyword? cofx-kw)]}
  (apply partial (get-handler :cofxs cofx-kw) args))

(defn- check-register-event-handler-parameters! [event-id interceptors handler]
  (when-not (keyword? event-id)
    (throw (ex-info "Event id is not a keyword"
                    {:event-id event-id})))
  (when-not (every? fn? interceptors)
    (throw (ex-info "Not all interceptors are functions"
                    {:interceptors interceptors})))
  (when-not (fn? handler)
    (throw (ex-info "The event handler is not a function"
                    {:handler handler}))))

(defn register-event-handler!
  ([event-id handler] (register-event-handler! event-id [] handler))
  ([event-id interceptors handler]
   (check-register-event-handler-parameters! event-id interceptors handler)
   (swap! handlers assoc-in [:event-fns event-id] handler)
   (swap! handlers assoc-in [:events event-id] {:interceptors interceptors
                                                :handler      handler})))

(defn- execute-event-handler [handler payload cofx]
  (handler cofx payload))

(defn- execute-event-chain [{:keys [interceptors handler]} payload]
  (->> interceptors
       apply-interceptors
       (execute-event-handler handler payload)
       handle-effects))

(defn- log-event! [event-data]
  (when @verbose
    #?(:cljs (js/console.debug "%cre-om/submit!" "background: #444444; color: #1abb9b" event-data)
       :clj  (println "re-om/submit!" event-data))))

(defn dispatch! [event-data]
  {:pre [(vector? event-data)
         (keyword? (first event-data))]}
  (log-event! event-data)
  (let [[event-id & payload] event-data
        event-handler-chain (get-handler :events event-id)]
    (execute-event-chain event-handler-chain payload)))

(defn dispatch-n! [events]
  (doseq [event events]
    (when event
      (dispatch! event))))

(register-fx!
 :dispatch
 (fn [event]
   (dispatch! event)))

(register-fx!
 :dispatch-n
 (fn [events]
   (dispatch-n! events)))

(register-fx!
 :dispatch-later
 (fn [{:keys [ms event]}]
   #?(:cljs (js/setTimeout
             #(dispatch! event)
             ms)
      :clj  (future (Thread/sleep ms) (dispatch! event)))))

(defn register-events-delegation! [origin-events target-event]
  (doseq [event origin-events]
    (register-event-handler!
     event
     (fn [_ args]
       {:dispatch (into [target-event] args)}))))
