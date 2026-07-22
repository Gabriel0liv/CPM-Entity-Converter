# T203 easing and constant Molang

The baseline is GeckoLib 4.4.9. Easing is represented on each keyframe as
`easingFromPrevious`; the easing declared at timestamp `t` describes the
segment ending at `t`. The adapter maps the built-in sine, polynomial,
exponential, circular, back, elastic, bounce, step, and catmullrom names
case-insensitively. Easing arguments are preserved in author order; the
evaluator uses the first argument where GeckoLib does so. The `easeinquint`
power-four behavior is retained for 4.4.9 compatibility.

Constant Molang is intentionally offline and small: numeric arithmetic,
parentheses, unary operators, `math.pi`, deterministic math functions, and
degree-based sine/cosine are supported. Queries, variables, random functions,
and unknown identifiers are rejected as dynamic. Invalid constant syntax uses
`ANIM_MOLANG_PARSE_ERROR`.

Sampling and timeline evaluation belong to T400; runtime-dependent Molang and
future interpolation behavior remain outside this parser's scope.
