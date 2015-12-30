# yetibot.core change log

## yetibot.core 0.3.0

- Deprecated Campfire adapter but left code in place in case anyone wants to
  port it to the new protocol-based adapter system. If you use Campfire and want
  it to be supported but don't want to PR let me know and I'll re-add it. It was
  removed because I wasn't sure anyone uses Campfire.

- Huge refactoring of adapters. Each adapter is now an instance of an Adapter
  protocol, and there can be any number of the various types of adapters
  (currently Slack & IRC).

  Rooms are now configured independent of adapter configuration. Configuration
  is non-backward compatible. See
  [config-sample.edn](../config/config-sample.edn) for details.

- Refactored room settings. Arbitrary config can now be set on a room, e.g.
  ```
  !room set jira-project YETI
  ```

  Defaults for known settings are:

  | Name            | Default |
  | --------------- | ------- |
  | broadcast       | false   |
  | fun             | true    |
  | jira-project    | ""      |
  | jenkins-default | ""      |

  But you can also set any key/val you like.

  To view settings use `!room settings`.

  Full `room` docs:

  ```
  room settings <key> # show the value for a single setting
  room join <room> # join <room>
  room leave <room> # leave <room>
  room list # list rooms that yetibot is in
  room settings # show all chat settings forall rooms
  room set <key> <value> # configure a setting for the current room
  ```

- Disalbed AOT