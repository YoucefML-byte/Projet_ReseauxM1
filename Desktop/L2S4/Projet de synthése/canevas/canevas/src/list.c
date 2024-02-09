#include "list.h"

#include <stdio.h>
#include <stdlib.h>
#include <assert.h>

#include "util.h"

/********************************************************************
 * list_node_t
 ********************************************************************/

/**
 * Construit et initialise un nouveau nœud d'une liste doublement chaînée.
 * Le paramètre \p data correspond à la donnée à affecter au nouveau nœud.
 * 
 * @param[in] data
 * @return le nouveau nœud créé
 */
static struct list_node_t * new_list_node(void * data) {
	// TODO
}

void * get_list_node_data(const struct list_node_t * node) {
	assert(node);
	// TODO
}

struct list_node_t * get_successor(const struct list_node_t* node) {
	assert(node);
	// TODO
}

struct list_node_t * get_predecessor(const struct list_node_t * node) {
	assert(node);
	// TODO
}

void set_list_node_data(struct list_node_t * node, void * newData) {
	assert(node);
	// TODO
}

void set_successor(struct list_node_t * node, struct list_node_t * newSuccessor) {
	assert(node);
	// TODO
}

void set_predecessor(struct list_node_t * node, struct list_node_t * newPredecessor) {
	assert(node);
	// TODO
}

/********************************************************************
 * list_t (Double-Linked List)
 ********************************************************************/

struct list_t * new_list() {
	// TODO
}

int list_is_empty(const struct list_t * L) {
	assert(L);
	// TODO
}

unsigned int get_list_size(const struct list_t * L) {
	assert(L);
	// TODO
}

struct list_node_t * get_list_head(const struct list_t * L) {
	assert(L);
	// TODO
}

struct list_node_t * get_list_tail(const struct list_t * L) {
	assert(L);
	// TODO
}

void increase_list_size(struct list_t * L) {
	assert(L);
	// TODO
}

void decrease_list_size(struct list_t * L) {
	assert(L);
	// TODO
}

void set_list_size(struct list_t * L, unsigned int newSize) {
	assert(L);
	// TODO
}

void set_list_head(struct list_t * L, struct list_node_t * newHead) {
	assert(L);
	// TODO
}

void set_list_tail(struct list_t * L, struct list_node_t * newTail) {
	assert(L);
	// TODO
}

void delete_list(struct list_t * L, void (*freeData)(void *)) {
	assert(L);
	// TODO
}

void view_list(const struct list_t * L, void (*viewData)(const void *)) {
	assert(L);
	// TODO
}

void list_insert_first(struct list_t * L, void * data) {
	assert(L);
	// TODO
}

void list_insert_last(struct list_t * L, void * data) {
	assert(L);
	// TODO
}

void list_insert_after(struct list_t * L, void * data, struct list_node_t * node) {
	assert(L);
	// TODO
}

void * list_remove_first(struct list_t * L) {
	assert(L);
	assert(get_list_head(L) && get_list_tail(L));
	// TODO
}

void * list_remove_last(struct list_t * L) {
	assert(L);
	assert(get_list_head(L) && get_list_tail(L));
	// TODO
}

void * list_remove_node(struct list_t * L, struct list_node_t * node) {
	assert(L);
	assert(get_list_head(L) && get_list_tail(L));
	// TODO
}

void list_swap_nodes_data(struct list_node_t * node1, struct list_node_t * node2) {
	assert(node1);
	assert(node2);
	// TODO
}

int list_data_exist(struct list_t * L, void * data) {
	assert(L);
	// TODO
}