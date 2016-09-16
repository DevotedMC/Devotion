Devotion
=============

Baseline lightweight player tracker. Passively logs all actions by all players. Designed to have minimal impact on server runtime, leveraging background commits of data. Offers a "lossy mode" for maximum performance over monitoring accuracy.

Specific Features:

* Player block & entity interactions (place, break) are tracked
* Player crafting events are tracked
* Player furnace use events are tracked
* Player inventory events are tracked (self inventory, inventory moves, pickups, drops)
* Player entity inventory events are tracked (chests, hoppers, furnace, droppers, dispensers)
* Player login and logout events are tracked (including inventory and armors on logout, IPs, etc.)
* Player combat events are tracked (PVE, PVP, sword, bow, punch, buffs, debuffs, pearl TP)
* Player potion use events are tracked (splash, bottle, brew)
* Player enchantment events are tracked (anvil, table, XP gains and uses)
* Player food use events are tracked
* Player vehicle use is tracked (horse, boat, minecart)
* Player redstone interactions (buttons, plates, levers) are tracked
* Player door interactions are tracked (doors, gates)
* Player bucket use is tracked
* Player death events are tracked
* Player damage events are tracked (lightning, fall, drowning, poison/sick, suffocation)
* Player movement events are tracked (rate limited)
* Player chat events are tracked
* Player command events are tracked (successful and unsuccessful)

Technical details:

* Events fire, construct flyweights, pass to DAO, return
* All storage of events is asynchronous
* Focus on preserving server TPS and performance
* Aggressive paged-caching methodology for event tracking
* Fixed size memory buffers, pre-allocated, are used round-robin to queue writes
   * DB write is primary, but if too slow (data builds up faster then can flush) cache are flushed to temporary files on disk
   * DB buffer then pulls from disk until all "caught up".
   * Memory buffer sizes & count should be tweaked to prevent losses
   * Lossy mode skips disk write and simply throws away "too slow" pages
* Extensive monitoring metrics to gauge performance impact
* Tuning parameters to rate limit certain collected data
   * Movement and PVP events can be windowed -- e.g. sample for 5 ticks, then pause for 10, then sample for 5, etc. -- to prevent cache overflow

Gotchas:

THIS IS NOT AN ANALYTICS PLATFORM. This simply monitors. Perform your own associations; or keep your eyes open for a Django-based webapp that allows deep inspection of this data and analytics. Maybe.

The goal here is to track everything, so if reports of untoward behavior are received, you as operator have the ability to see and know everything that *actually* happened.

See [Siphon-README.md](Siphon-README.md) for details on an offloading platform that works in the background and does everything possible to minimize lag impact of offload.
