package com.cyberopsarena.simulation.graph;

import java.util.*;

/** Directed graph helper used only for virtual reachability checks. */
public final class NetworkGraph {
    private final Map<Long,List<Long>> adjacency = new HashMap<>();
    public NetworkGraph(Collection<long[]> edges) { for (long[] edge : edges) adjacency.computeIfAbsent(edge[0], k->new ArrayList<>()).add(edge[1]); }
    public boolean reachable(long from,long to) {
        if(from==to) return true;
        Set<Long> seen=new HashSet<>(); ArrayDeque<Long> todo=new ArrayDeque<>(); todo.add(from); seen.add(from);
        while(!todo.isEmpty()) for(Long next:adjacency.getOrDefault(todo.remove(),List.of())) { if(next==to)return true; if(seen.add(next))todo.add(next); }
        return false;
    }
}
