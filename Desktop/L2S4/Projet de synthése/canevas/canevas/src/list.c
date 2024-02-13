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
	struct list_node_t *L=calloc(1,sizeof(struct list_node_t));
	if(L == NULL){
		printf("espace indisponible \n");
		return NULL;
	}else{
		L->data=data;
		return L;
	}
}

void * get_list_node_data(const struct list_node_t * node) {
	assert(node);
	return node->data;
}

struct list_node_t * get_successor(const struct list_node_t* node) {
	assert(node);
	return node->succesor;
}

struct list_node_t * get_predecessor(const struct list_node_t * node) {
	assert(node);
	return node->predecessor;
}

void set_list_node_data(struct list_node_t * node, void * newData) {
	assert(node);
	if(newData != NULL){
		node->data=newData;
	}
}

void set_successor(struct list_node_t * node, struct list_node_t * newSuccessor) {
	assert(node);
	node->successor=newSuccessor;
}

void set_predecessor(struct list_node_t * node, struct list_node_t * newPredecessor) {
	assert(node);
	node->predecessor=newPredecessor;
}

/********************************************************************
 * list_t (Double-Linked List)
 ********************************************************************/

struct list_t * new_list() {
	struct list_t *L=calloc(1,sizeof(struct list_t ));
	if(L == NULL){
		printf("espace indisponible \n");
		return NULL;
	}else{
		return L;
	}
}


int list_is_empty(const struct list_t * L) {
	assert(L);
	if(L->size == 0){
		return 0;
	}
	return 1;
}

unsigned int get_list_size(const struct list_t * L) {
	assert(L);
	return L->size;
}

struct list_node_t * get_list_head(const struct list_t * L) {
	assert(L);
	return L->head;
}

struct list_node_t * get_list_tail(const struct list_t * L) {
	assert(L);
	return L->tail;
}

void increase_list_size(struct list_t * L) {
	assert(L);
	L->size++;
}

void decrease_list_size(struct list_t * L) {
	assert(L);
	L->size--;
}

void set_list_size(struct list_t * L, unsigned int newSize) {
	assert(L);
	if()
	L->size = L->newSize;
}

void set_list_head(struct list_t * L, struct list_node_t * newHead) {
	assert(L);
	L->head=newHead;
}

void set_list_tail(struct list_t * L, struct list_node_t * newTail) {
	assert(L);
	L->tail = newTail;
}

void delete_list(struct list_t * L, void (*freeData)(void *)) {
	assert(L);
	for(struct list_node_t *E=get_list_head(L);E;get_successor(E)){
		if(freeData != NULL){
			(*freeData) (E->data);
		}
		free(E);
	}
	free(&L);
	L=NULL;
}

void view_list(const struct list_t * L, void (*viewData)(const void *)) {
	assert(L);
	for(struct list_node_t *E=get_list_head(L);E;get_successor(E)){
		if(viewData !=NULL){
			(*viewData)(E->data);
		}
	}
}

void list_insert_first(struct list_t *L, void *data) {
    // Création d'un nouveau nœud
    struct list_node_t *nv_noeud = (struct list_node_t *)malloc(sizeof(struct list_node_t));
    if (nv_noeud == NULL) {
        fprintf(stderr, "Erreur d'allocation de mémoire\n");
        exit(EXIT_FAILURE);
    }

    // Initialisation des champs du nouveau nœud
    nv_noeud->data = data;
    nv_noeud->successor = L->head;
    nv_noeud->predecessor = NULL;

    // Mettre à jour le prédécesseur de l'ancienne tête si elle existe
    if (L->head != NULL) {
        L->head->predecessor = nv_noeud;
    }

    // Mettre à jour la tête de la liste avec le nouveau nœud
    L->head = nv_noeud;
}

void list_insert_last(struct list_t *L, void *data) {
    // Création d'un nouveau nœud
    struct list_node_t *nv_noeud = (struct list_node_t *)malloc(sizeof(struct list_node_t));
    if (nv_noeud == NULL) {
        fprintf(stderr, "Erreur d'allocation de mémoire\n");
        exit(EXIT_FAILURE);
    }

    // Initialisation des champs du nouveau nœud
    nv_noeud->data = data;
    nv_noeud->successor = NULL;
    nv_noeud->predecessor = NULL;

    // Cas où la liste est vide
    if (L->head == NULL) {
        L->head = nv_noeud;
        return;
    }

    // Trouver le dernier nœud dans la liste
    struct list_node_t *actuel = L->head;
    while (actuel->successor != NULL) {
        actuel = actuel->successor;
    }

    // Mettre à jour le successeur du dernier nœud avec le nouveau nœud
    actuel->successor = nv_noeud;
    // Mettre à jour le prédécesseur du nouveau nœud
    nv_noeud->predecessor = actuel;
}

void list_insert_after(struct list_t *L, void *data, struct list_node_t *ptrelm) {
    // Vérifier si le nœud après lequel nous voulons insérer existe
    if (ptrelm == NULL) {
        fprintf(stderr, "Erreur : Le pointeur vers le nœud de référence est NULL.\n");
        exit(EXIT_FAILURE);
    }

    // Création d'un nouveau nœud
    struct list_node_t *nv_noeud = (struct list_node_t *)malloc(sizeof(struct list_node_t));
    if (nv_noeud == NULL) {
        fprintf(stderr, "Erreur d'allocation de mémoire\n");
        exit(EXIT_FAILURE);
    }

    // Initialisation des champs du nouveau nœud
    nv_noeud->data = data;
    nv_noeud->successor = ptrelm->successor;
    nv_noeud->predecessor = ptrelm;

    // Mettre à jour le successeur du nœud de référence avec le nouveau nœud
    if (ptrelm->successor != NULL) {
        ptrelm->successor->predecessor = nv_noeud;
    }
    ptrelm->successor = nv_noeud;
}

void * list_remove_first(struct list_t * L) {
	assert(L);
	assert(get_list_head(L) && get_list_tail(L));
	struct list_node_t *E=get_list_head(L);
	set_list_head(L,get_successor(L->head));
	set_successor(E,NULL);
	void *d = E->data;
	free(E);
	decrease_list_size(L);
	return d;
}

void * list_remove_last(struct list_t * L) {
	assert(L);
	assert(get_list_head(L) && get_list_tail(L));
	struct list_node_t *E=get_list_tail(L);
	set_list_tail(L,get_predecessor(L->tail));
	set_predecessor(E,NULL);
	void *d = E->data;
	free(E);
	decrease_list_size(L);
	return d;
}

void * list_remove_node(struct list_t * L, struct list_node_t * node) {
	assert(L);
	assert(get_list_head(L) && get_list_tail(L));
	struct list_node_t *N=calloc(1,sizeof(struct list_node_t));
	N=node;
	set_successor((get_predecessor(N)),get_successor(N));
	set_predecessor(get_successor(N),get_predecessor(N));
	set_successor(N;NULL);
	set_predecessor(N,NULL);
	void *d= E->data;
	free(E);
	decrease_list_size(L);
	return d;
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
