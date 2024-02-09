#include "util.h"

#include <stdio.h>
#include <stdlib.h>

void ShowMessage(char * msg, int interrupt) {
	fprintf(stderr, "%s\n", msg);
	if (interrupt) {
		fprintf(stderr, "An error has occurred. The execution of the program will be interrupted!\n");
		exit(EXIT_FAILURE);
	}
}

void viewInt(const void * i) {
	if (i == NULL)
		printf("NULL");
	else
		printf("%d", *((int*) i));
}

void freeInt(void * i) {
	free((int*) i);
}

int intSmallerThan(const void * a, const void * b) {
	if (*((int*) a) < *((int*) b))
		return 1;
	else
		return 0;
}

int intGreaterThan(const void * a, const void * b) {
	if (*((int*) a) > *((int*) b))
		return 1;
	else
		return 0;
}

int intEqualTo(const void * a, const void * b) {
	if (*((int*) a) == *((int*) b))
		return 1;
	else
		return 0;
}