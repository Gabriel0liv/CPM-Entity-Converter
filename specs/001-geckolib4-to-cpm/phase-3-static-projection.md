# T300 static CPM projection

`projection-cpm` is the in-memory boundary from validated `ModelIR` plus a
compiled `SemanticRigMap` to a logical CPM V1 graph. It does not read geometry,
PNG or mapping files and does not serialize JSON.

The baseline is a deterministic `single_anchor` projection. The mapped `body`
BoneId must be the sole ModelIR root and becomes the CPM BODY root. HEAD, BODY,
LEFT_ARM, RIGHT_ARM, LEFT_LEG and RIGHT_LEG are always emitted in canonical
order; only BODY receives the source hierarchy. Bone and cube nodes retain
logical keys and provenance. Rotated cubes receive one synthetic helper, while
the cube target remains the real cube node.

Transforms use local ModelIR bind transforms. Authored pivots are reconstructed
by summing local translations without applying parent rotation or scale. Static
quaternions are decomposed to ZYX Euler degrees. Box and per-face UV are carried
to the logical graph with signed dimensions and mirror/inflate preserved.

`CpmProjectionIndex` maps every BoneId and CubeId to logical targets and records
helpers without assigning numeric store IDs. T301 will assign persisted IDs;
T302 will write the project; T303 will validate the persisted artifact.

Deferred from T300: root partitioning, modelScale, verticalOffset, animation
projection, sampling, lifecycle, pose mapping, writer and ProjectIO.
