Interceptors decorate event handlers with pure functions that are
executed before or after an event handler is executed.

Interceptors might look after "cross-cutting" concerns like undo, tracing or validation.

Have a look at the [great explanation of interceptors in re-frame's documentation](https://github.com/Day8/re-frame/blob/master/docs/Interceptors.md).

Also have a look at [an example of how you might use them which is reffectory's tests](https://github.com/GreenPowerMonitor/reffectory/blob/bfa13d839782f103cc83502c1b5b4c020887da14/test/greenpowermonitor/reffectory_test.cljc#L49).