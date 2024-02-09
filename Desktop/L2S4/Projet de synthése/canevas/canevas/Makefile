IDIR = include
ODIR = obj
SDIR = src
JDIR = java
SCDIR = script

CC = gcc
CFLAGS = -Wall -Wextra -fPIC -c -I$(IDIR)

PROG = dijkstra
TEST = runtest
LIB = $(JDIR)/libdijkstra.so
EXPE = $(SCDIR)/expe

JAVA_HOME = /usr/lib/jvm/java-11-openjdk-amd64
JINCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

_DEPS = util.h list.h tree.h dyntable.h heap.h graph.h algo.h
DEPS = $(patsubst %,$(IDIR)/%,$(_DEPS))

_OBJ= util.o list.o tree.o dyntable.o heap.o graph.o algo.o
OBJ = $(patsubst %,$(ODIR)/%,$(_OBJ))

.PHONY: run all test jni expe clean delete deletetest deletelib deleteexpe cleanall memorycheck testmemorycheck

all : $(PROG)

run : all
	./$(PROG) $(in) $(out) $(source) $(heap)

test : $(TEST)
	./$(TEST)

jni : $(LIB)

expe : $(EXPE)

clean :
	rm -f $(ODIR)/*.o

delete :
	rm -f $(PROG)

deletetest :
	rm -f $(TEST)

deletelib :
	rm -f $(LIB)

deleteexpe :
	rm -f $(EXPE)

cleanall : clean delete deletetest deletelib deleteexpe

memorycheck : all
	valgrind ./$(PROG) $(in) $(out) $(source) $(heap)

testmemorycheck : $(TEST)
	valgrind ./$(TEST)

$(ODIR)/%.o : $(SDIR)/%.c $(DEP)
	$(CC) $(CFLAGS) -o $@ $<

$(ODIR)/application_Dijkstra.o : $(SDIR)/application_Dijkstra.c $(DEP)
	$(CC) $(CFLAGS) $(JINCLUDES) -o $@ $<

$(PROG) : $(OBJ) $(ODIR)/main.o
	$(CC) -o $@ $^ -lm

$(TEST) : $(OBJ) $(ODIR)/test.o
	$(CC) -o $@ $^ -lm

$(EXPE) : $(OBJ) $(ODIR)/expe.o
	$(CC) -o $@ $^ -lm

$(LIB) : $(OBJ) $(ODIR)/application_Dijkstra.o
	$(CC) -shared -o $@ $^ -lm