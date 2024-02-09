#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "util.h"
#include "list.h"
#include "tree.h"
#include "dyntable.h"
#include "heap.h"
#include "graph.h"
#include "algo.h"

/////////////////////////////////////////////////////////////////////////////
///////////////////////////////// TEST LIST /////////////////////////////////
/////////////////////////////////////////////////////////////////////////////

static int compare_lists(struct list_t * l1, void * l2[], unsigned int size) {
	if (get_list_size(l1) != size)
		return 0;

	if (get_list_head(l1) == NULL)
		return 1;

	struct list_node_t * curr = get_list_head(l1);
	unsigned int i = 0;
	while (curr != NULL && l2) {
		if (get_list_node_data(curr) != l2[i])
			return 0;
		curr = get_successor(curr);
		i++;
	}

	curr = get_list_tail(l1);
	i = size-1;
	while (curr != NULL && l2) {
		if (get_list_node_data(curr) != l2[i])
			return 0;
		curr = get_predecessor(curr);
		i--;
	}
	return 1;
}

void test_list_insert_last() {
	int t = 0;
	int f = 0;
	int* tab[3];

	int *i1 = malloc(sizeof(int));
	int *i2 = malloc(sizeof(int));
	int *i3 = malloc(sizeof(int));
	*i1 = 1;
	*i2 = 2;
	*i3 = 3;

	fprintf(stderr, "Testing list_insert_last ");
	struct list_t * L = new_list();
	list_insert_last(L, i1);
	tab[0] = i1;
	if (compare_lists(L, (void *) tab, 1) == 0) {fprintf(stderr,"x");f++;} else {fprintf(stderr,".");t++;}
	list_insert_last(L, i2);
	tab[1] = i2;
	if (compare_lists(L, (void *) tab, 2) == 0) {fprintf(stderr,"x");f++;} else {fprintf(stderr,".");t++;}
	list_insert_last(L, i3);
	tab[2] = i3;
	if (compare_lists(L, (void *) tab, 3) == 0) {fprintf(stderr,"x");f++;} else {fprintf(stderr,".");t++;}

	delete_list(L, freeInt);

	fprintf(stderr, " DONE (%d/%d)\n", t, t+f);
}

int main() {
	test_list_insert_last();
}