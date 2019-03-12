//
// Created by honto on 2018/05/25.
//

#ifndef NAVIGATION_C_WIN_DESJOINT_SET_H
#define NAVIGATION_C_WIN_DESJOINT_SET_H


typedef struct {
    int rank;
    int p;
    int size;
} uni_elt;

class universe {
public:
    universe(int elements);
    ~universe();
    int find(int x);
    void join(int x, int y);
    int size(int x) const { return elts[x].size; }
    int num_sets() const { return num; }

private:
    uni_elt *elts;
    int num;
};


#endif //NAVIGATION_C_WIN_DESJOINT_SET_H
