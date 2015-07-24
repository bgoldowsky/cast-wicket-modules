# Introduction #

Cwm-base holds definitions that are widely needed by other modules.


# Details #

At the moment there is only one feature defined in cwm-base: a service and interface for logging of events.  This allows CWM modules to contain code that generates events that may be interesting to log.  The default service does nothing with these generated events however; it is expected that other modules or your application will override the default behavior to do something useful with the logged information if needed (see [cwm-data](CwmData.md) for an implementation that defines an Event class and stores the Events into a database table).