#include <stdio.h>
#include "sample.h"

int main()
{
    printf("hello\n");
    printf("%d\n", sum(1, 3));
    return 0;
}

int sum(int a, int b)
{
    return a + b;
}