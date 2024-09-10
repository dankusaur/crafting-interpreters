# Directories
BIN = bin
OBJ = obj
SRC = src
DEPS := deps

CC = gcc
CFLAGS = -Wall
DEPFLAGS = -MT $@ -MMD -MP -MF $(DEPS)/$*.d

SOURCES := $(wildcard $(SRC)/*.c)
OBJECTS := $(patsubst $(SRC)/%.c,$(OBJ)/%.o,$(SOURCES))
DEPENDENCIES:= $(patsubst $(SRC)/%.c,$(DEPS)/%.d,$(SOURCES))

.DEFAULT_GOAL := all

.PHONY: clean all

$(BIN):
	mkdir -p $(BIN)

$(OBJ):
	mkdir -p $(OBJ)

$(DEPS):
	mkdir -p $(DEPS)

$(OBJ)/%.o: $(SRC)/%.c $(DEPS)/%.d | $(OBJ) $(DEPS)
	$(CC) $(CFLAGS) $(DEPFLAGS) -c -o $@ $<

all: $(OBJECTS) | $(BIN)
	$(CC) $(CFLAGS) $(OBJECTS) -o $(BIN)/clox 

clean:
	rm -rf $(OBJ) $(BIN) $(DEPS)

$(DEPENDENCIES):

include $(wildcard $(DEPENDENCIES))