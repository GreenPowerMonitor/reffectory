## dispatch!
It dispatches an **event** that will be processed by the event handling machinery.
That **event** mues be a vector of at least one element. The first element identifies the kind of event and the rest of the elements are the payload of the event.

**Important**: The corresponding event handler will be **synchronously** run.

Example:
```clj
(dispatch! [::domain.member/open.rim (:Id data) :overview])
```

## dispatch-n! [events]
It dispatches several events that will be sequentially processed by the event handling machinery.

It receives **a sequence of events**. Each of them must have the structure described in [`dispatch!`](https://github.com/GreenPowerMonitor/reffectory/blob/master/docs/api.md#dispatch-event-data).

Example:
```clj
(dispatch-n! [[::preview/close-and-clean]
              [::dialogs.edit/clean-state]])
```

## register-event-handler!
  [event-id handler]
  [event-id interceptors handler]
TODO

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
