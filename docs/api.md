## dispatch!
This function dispatches an **event** that will be processed by the event handling machinery.
That **event** mues be a vector of at least one element. The first element identifies the kind of event and the rest of the elements are the payload of the event.

**Important**: The corresponding event handler will be **synchronously** run.

Example:
```clj
(dispatch! [::domain.member/open.rim (:Id data) :overview])
```

## dispatch-n!
This function dispatches several events that will be sequentially processed by the event handling machinery.

It receives **a sequence of events**. Each of them must have the structure described in [`dispatch!`](https://github.com/GreenPowerMonitor/reffectory/blob/master/docs/api.md#dispatch).

Example:
```clj
(dispatch-n! [[::preview/close-and-clean]
              [::dialogs.edit/clean-state]])
```

## register-event-handler!
This function is used to associate a given event with its handler.

It has two possible arities:

1. [event-id handler]

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

2. [event-id interceptors handler]

In this case the first parameter is the event identifier,
the second parameter is a list of interceptors that will be executed before or after the event handler
(depending on how they are defined, see [Interceptors](https://github.com/GreenPowerMonitor/reffectory/blob/master/docs/interceptors.md))
and the last parameter is the event handler.

A typical usage of interceptors is to inject coeffects values into the coeffects map that event handlers receive.

Example:

```clj
(register-event-handler!
  ::device-overview.init
  [inject-cofx :db]
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

# inject-cofx
[cofx-kw & args]
TODO

# register-fx!
 [fx-id handler]
TODO

# register-cofx!
  {:pre [(keyword? cofx-id)
         (fn? handler)]}
TODO

## register-events-delegation!
[origin-events target-event]
TODO

# interceptor
[{:keys [before after id] :or {before identity after identity}}]
TODO

# get-handler
[handler-type id]
TODO
