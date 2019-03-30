## `dispatch!`
This function dispatches an **event** that will be processed by the event handling machinery.
That **event** must be a vector of at least one element. The first element identifies the kind of event and the rest of the elements are the payload of the event.

**Important**: The corresponding event handler will be **synchronously** run.

Example:
```clj
(dispatch! [::domain.member/open.rim (:Id data) :overview])
```

## `dispatch-n!`
This function dispatches several events that will be sequentially processed by the event handling machinery.

It receives **a sequence of events**. Each of them must have the structure described in [`dispatch!`](https://github.com/GreenPowerMonitor/reffectory/blob/master/docs/api.md#dispatch).

Example:
```clj
(dispatch-n! [[::preview/close-and-clean]
              [::dialogs.edit/clean-state]])
```

## `register-event-handler!`
This function is used to associate a given event with its handler.

It has two possible arities:

#### [event-id handler]

In this case the first parameter is the event identifier and
the second parameter is the event handler.

Example:
```clj
(register-event-handler!
 ::set-selected-charts-range
 (fn [_ [selected-charts-range]]
   {:dispatch-n [[::combobox.domain/set-value ::charts-range-combo selected-charts-range]
                 [::load-charts-data-from-range-change selected-charts-range]]}))
```

#### [event-id interceptors handler]

In this case the first parameter is the event identifier,
the second parameter is a list of interceptors that will be executed before or after the event handler
(depending on how they are defined, see [Interceptors](https://github.com/GreenPowerMonitor/reffectory/blob/master/docs/interceptors.md))
and the last parameter is the event handler.

A typical usage of interceptors is to inject coeffects values into the coeffects map that event handlers receive.

Example:
```clj
(register-event-handler!
  ::device-overview.init
  [(inject-cofx :db)]
  (fn [{:keys [db]} [device-id]]
    (let [navigation (get-in db [:navigation :current-route])
          facility-id (-> navigation :params :facility-id)
          tech-type (-> navigation :tech-type)]
      (when facility-id
        {:dispatch [::core/device-config.fetch
                    {facility-id facility-id
                     tech-type tech-type
                     device-id device-id}]}))))
```

## `inject-cofx`
This function is used to inject coeffects. It actually creates an interceptor that
will call the corresponding coeffect handler right before the event handler is called,
so that the coeffects map includes the actual value that the coeffect declares.

It receives at least one parameter: the coeffect identifier.
The rest of parameters are optional and depend on the coeffect being used because
they will be passed to the coeffect handler.

Example:
```clj
(register-event-handler!
 ::update-facilities-devices-list-info
 [(inject-cofx :server-uri)
  (inject-cofx :state {:facility-id facilities-devices-current-facility-id-lens
                               :time-period facilities-devices-time-period-lens})]
 (fn [{:keys [server-uri] {:keys [facility-id time-period]} :state} _]
   {:http.get {:url (urls/mk-facility-devices-list-url server-uri facility-id)
                     :affected-state-lenses #{facilities-devices-card-devices-state-lens}
                     :cancel-key ::update-facilities-devices
                     :success-event ::update-facilities-devices-list-info-succeeded
                     :query-params {:period (serialize time-period)}}}))
```

In this example, `inject-cofx` is used to create two interceptors that will inject the values
represented by the custom `:server-uri` and `:state` coeffects into the coeffects map.

Notice how in this example when `inject-cofx` is used with the `:state` coeffect, it receives another parameter beside the coeffect identifier,
that means that the `:state` coeffect handler will receive that parameter and use it to compute the value that will be injected in the coeffects map
when the `::update-facilities-devices-list-info` event handler is about to be executed.

## `register-fx!`
This function is used to associate a given effect with its handler.

It receives two parameters: the effect identifier which has to be a keyword and the effect handler which has to be a function.

Example:
```clj
(re-om/register-fx!
  :om/state
  (fn [[owner mutations]]
    (doseq [[kw value] mutations]
      (om/set-state! owner kw value))))
```

This example registers an effect `:om/state` that mutates the local state of an Om component.

## `register-cofx!`
This function is used to associate a given coeffect with its handler.

It receives two parameters: the coeffect identifier which has to be a keyword and the coeffect handler which has to be a function.

```clj
(re-om/register-cofx!
  :om/state
  (fn [owner kws cofx]
    (assoc cofx
      :om/state
      (into {}
            (map #(vector % (om/get-state owner %))
                 kws)))))
```

This example registers a coeffect `:om/state` that extracts from the local state of an Om component
the values associated to some given keys.

## `register-events-delegation!`
This function delegates the handling of the vector of event identifiers it receives as its first parameter
to the event handler associated with the event identifier it receives as its second parameter.

Example:
```clj
(re-om/register-events-delegation!
 [:facilities-device-power-curve-chart.enter
  :facilities-device-power-curve-table.enter]
 :facilities-device-power-curve.enter)
```

In this examples the handling of `:facilities-device-power-curve-chart.enter` and `:facilities-device-power-curve-table.enter`
will be delegated to the event handler of the `:facilities-device-power-curve.enter` event,
which will be the only handler that needs to be registered using `register-event-handler!`.


## `interceptor`
It's a factory function that can be used to create an interceptor.

See [Interceptors](https://github.com/GreenPowerMonitor/reffectory/blob/master/docs/interceptors.md) to know what an interceptor is.

This function should receive a map with values for the following keywords:
* `:id`: the identifier of the interceptor.
* `:before`: a pure function that will be called right before an event handler is executed and will transform what the event handler will receive. By default it's the `identity` function.
* `:after`: a pure function that will be called right after an event handler is executed and will transform what the effect handlers will receive. By default it's the `identity` function.

Example:
```clj
(defn inject-cofx [cofx-kw & args]
  {:pre [(keyword? cofx-kw)]}
  (let [cofx-handler (get-handler :cofxs cofx-kw)]
    (interceptor {:id cofx-kw
                  :before (fn [{:keys [coeffects] :as context}]
                                (assoc context :coeffects (apply cofx-handler (concat args [coeffects]))))})))
```

In this example we show the `inject-cofx` function from reffectory. It uses the `interceptor` function to create an interceptor that
will apply the corresponding coeffect handler right before the event handler to which you pass this interceptor when it's registered (see [`register-event-handler` documentation above](https://github.com/GreenPowerMonitor/reffectory/blob/master/docs/api.md#register-event-handler))
and adds its result to the coeffects map.

You can check [another interesting usage of `interceptor` in reffectory's tests](https://github.com/GreenPowerMonitor/reffectory/blob/bfa13d839782f103cc83502c1b5b4c020887da14/test/greenpowermonitor/reffectory_test.cljc#L49).

## `get-handler`
This function should be used **only in tests**. It gets handlers registered in reffectory.

It receives two parameters:  the handler type and the identifier of
the thing (event, effect or coeffect) the handler is associated with.

Example:
```clj
(let [subscribe (get-handler :event-fns ::real-time-data/subscribe) ;; <- this extracts an event handler
       extract-om-state (get-handler :cofxs :om/state) ;; <- this extracts a coeffect handler
       mutate-om-state (get-handler :fxs :om/state)] ;; <- this extracts an effect handler
 ;; doing something with them in tests
)
```
