package de.miraculixx.veinminer.pattern

enum class Shape(val strategy: ShapeStrategy) {
    NORMAL(NormalStrategy),
    TUNNEL_1X1(TunnelStrategy(1)),
    TUNNEL_2X2(TunnelStrategy(2)),
    TUNNEL_3X3(TunnelStrategy(3)),
    FLAT(FlatStrategy);
}