package com.cyberopsarena.simulation.graph;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NetworkGraphTest {
 @Test void findsDirectedReachabilityWithoutFollowingReverseEdges(){var graph=new NetworkGraph(List.of(new long[]{1,2},new long[]{2,3}));assertTrue(graph.reachable(1,3));assertFalse(graph.reachable(3,1));assertFalse(graph.reachable(1,4));}
 @Test void handlesCycles(){var graph=new NetworkGraph(List.of(new long[]{1,2},new long[]{2,1}));assertTrue(graph.reachable(1,1));assertFalse(graph.reachable(1,3));}
}
