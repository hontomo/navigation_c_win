//
// Created by honto on 2018/05/25.
//

#ifndef NAVIGATION_C_WIN_SEGMENT_GRAPH_H
#define NAVIGATION_C_WIN_SEGMENT_GRAPH_H


#include <algorithm>
#include <cmath>
#include "disjoint-set.h"

// threshold function
#define THRESHOLD(size, c) (c/size)

typedef struct {
    float w;
    int a, b;
} edge;

bool operator<(const edge &a, const edge &b);

//segment a graph
universe *segment_graph(int num_vertices, int num_edges, edge *edges,
                        float c);


#endif //NAVIGATION_C_WIN_SEGMENT_GRAPH_H
