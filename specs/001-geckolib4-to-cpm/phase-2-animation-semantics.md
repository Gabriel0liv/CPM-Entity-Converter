# T202 animation semantics

The parser accepts Bedrock animation `format_version` **1.8.0** and indexes
clips by their exact (case-sensitive, Unicode-safe) identifiers. Playback maps
boolean/string loop values to `PLAY_ONCE`, `LOOP`, or `HOLD`; unknown values are
diagnosed. Explicit positive `animation_length` is used, otherwise the largest
keyframe timestamp is inferred.

Position channels are additive local offsets converted with `(-x,-y,+z)`.
Rotation remains source Euler degrees in ZYX order. Scale is absolute local
values. Scalar, vector, timestamp-map, and GeckoLib pre/post forms are parsed;
Molang and easing are rejected for T203. Sound, particle, and timeline events
are retained as ignored warnings. Sampling, lifecycle policy, and CPM mapping
remain outside T202.
