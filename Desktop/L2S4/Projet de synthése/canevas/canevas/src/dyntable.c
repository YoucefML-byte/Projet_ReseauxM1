#include "dyntable.h"

#include <stdlib.h>
#include <stdio.h>
#include <assert.h>

#include "util.h"

/********************************************************************
 * dyn_table_t (Dynamic Table)
 ********************************************************************/

void * get_dyn_table_data(const struct dyn_table_t * table, unsigned int position) {
	assert(table);
	assert(table->T);
	assert(position < table->used);
	// TODO
}

unsigned int get_dyn_table_size(const struct dyn_table_t * table) {
	assert(table);
	// TODO
}

unsigned int get_dyn_table_used(const struct dyn_table_t * table) {
	assert(table);
	// TODO
}

int dyn_table_is_empty(const struct dyn_table_t * table) {
	assert(table);
	// TODO
}

int dyn_table_is_full(const struct dyn_table_t * table) {
	assert(table);
	// TODO
}

int dyn_table_is_quasi_empty(const struct dyn_table_t * table) {
	assert(table);
	// TODO
}

void set_dyn_table_data(struct dyn_table_t * table, unsigned int position, void * newData) {
	assert(table);
	assert(table->T);
	assert(position < table->size);
	// TODO
}

void set_dyn_table_size(struct dyn_table_t * table, unsigned int newSize) {
	assert(table);
	// TODO
}

void increase_dyn_table_used(struct dyn_table_t * table) {
	assert(table);
	// TODO
}

void decrease_dyn_table_used(struct dyn_table_t * table) {
	assert(table);
	// TODO
}

/**
 * Dédoubler la taille du tableau dynamique \p table.
 *
 * @param table
 */
static void scale_up(struct dyn_table_t * table) {
	// TODO
}

/**
 * Diviser par 2 la taille du tableau dynamique \p table.
 *
 * @param table
 */
static void scale_down(struct dyn_table_t * table) {
	// TODO
}

struct dyn_table_t * new_dyn_table() {
	// TODO
}

void delete_dyn_table(struct dyn_table_t * table, void (*freeData)(void *)) {
	assert(table);
	// TODO
}

void view_dyn_table(const struct dyn_table_t * table, void (*viewData)(const void *)) {
	assert(table);
	// TODO
}

void dyn_table_insert(struct dyn_table_t * table, void *data) {
	assert(table);
	// TODO
}

void * dyn_table_remove(struct dyn_table_t * table) {
	assert(table);
	assert(get_dyn_table_used(table) > 0);
	// TODO
}

void dyn_table_swap_nodes_data(struct dyn_table_t * table, unsigned int pos1, unsigned int pos2) {
	assert(pos1 < get_dyn_table_used(table));
	assert(pos2 < get_dyn_table_used(table));
	// TODO
}